package patdroid.smali;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.common.collect.ImmutableList;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.reference.MethodReference;

import patdroid.Settings;
import patdroid.core.*;
import patdroid.util.Log;

import patdroid.dalvik.Dalvik;

/**
 * Load classes, methods, fields and instructions from an APK file with SMALI
 * https://github.com/JesusFreke/smali
 */
public class SmaliClassDetailLoader extends ClassDetailLoader {
    private final DexFile[] dexFiles;
    private final boolean translateInstructions;
    private final boolean isFramework;
    private final InvocationResolver resolver;

    private SmaliClassDetailLoader(DexFile[] dexFiles, boolean translateInstructions, boolean isFramework) {
        this.dexFiles = dexFiles;
        this.translateInstructions = translateInstructions;
        this.isFramework = isFramework;
        this.resolver = translateInstructions ? new InvocationResolver() : null;
    }

    /**
     * Create a loader that loads from an APK file (could contain multiple DEX files), optionally loading instructions
     * @param apkFile the APK file
     * @param translateInstructions true if the instructions shall be loaded
     */
    public static SmaliClassDetailLoader fromApkFile(ZipFile apkFile, boolean translateInstructions) {
        ArrayList<ZipEntry> dexEntries = new ArrayList<ZipEntry>();
        dexEntries.add(apkFile.getEntry("classes.dex"));
        for (int i = 2; i < 99; ++i) {
            final ZipEntry e = apkFile.getEntry("classes" + i +".dex");
            if (e != null) {
                dexEntries.add(e);
            } else {
                break;
            }
        }
        final int n = dexEntries.size();
        if (n == 0) {
            Log.err("Source apk does not have any dex files");
        }

        DexFile[] dexFiles = new DexFile[n];
        final Opcodes opcodes = Opcodes.forApi(Settings.apiLevel);
        try {
            for (int i = 0; i < n; ++i) {
                dexFiles[i] = DexBackedDexFile.fromInputStream(opcodes,
                        new BufferedInputStream(apkFile.getInputStream(dexEntries.get(i))));
            }
        } catch (IOException e) {
            Log.err("failed to process the source apk file");
            Log.err(e);
        }
        return new SmaliClassDetailLoader(dexFiles, translateInstructions, false);
    }

    public static SmaliClassDetailLoader fromFramework(int apiLevel) {
        File f = new File(Settings.frameworkClassesFolder, "android-" + apiLevel + ".dex");
        if (!f.exists())
            return null;
        DexFile dex = null;
        try {
            dex = DexFileFactory.loadDexFile(f, apiLevel);
        } catch (IOException e) {
            Log.err("failed to load framework classes");
            Log.err(e);
            return null;
        }
        return new SmaliClassDetailLoader(new DexFile[] {dex}, false, true);
    }

    @Override
    public void load(ClassInfo ci) throws ClassNotFoundException,
            ExceptionInInitializerError, NoClassDefFoundError
    {
        for (DexFile dexFile: dexFiles) {
            for (final ClassDef classDef : dexFile.getClasses()) {
                if (Dalvik.toCanonicalName(classDef.getType()).equals(ci.fullName)) {
                    ClassDetail detail = translateClassDef(ci, classDef);
                    setDetail(ci, detail);
                    if (translateInstructions) {
                        resolver.resolveAll();
                    }
                }
            }
        }
        throw new ClassNotFoundException("" + ci.fullName + " not found in the dex file");
    }

    /**
     * Parse an apk file and extract all classes, methods, fields and optionally instructions
     */
    public void loadAll(Scope scope) {
        for (DexFile dexFile: dexFiles) {
            for (final ClassDef classDef : dexFile.getClasses()) {
                ClassInfo ci = Dalvik.findOrCreateClass(scope, classDef.getType());
                ClassDetail detail = translateClassDef(ci, classDef);
                setDetail(ci, detail);
            }
        }
        if (translateInstructions) {
            resolver.resolveAll();
        }
    }

    private ClassDetail translateClassDef(
            ClassInfo ci, ClassDef classDef) {
        ClassInfo baseType;
        if (classDef.getSuperclass() == null) {
            baseType = null; // for java.lang.Object
        } else {
            baseType = Dalvik.findOrCreateClass(ci.scope, classDef.getSuperclass());
        }
        final ImmutableList<ClassInfo> interfaces = findOrCreateClasses(ci.scope, classDef.getInterfaces());
        final int accessFlags = translateAccessFlags(classDef.getAccessFlags());
        final ImmutableList<MethodInfo> methods = translateMethods(ci, classDef.getMethods());
        final HashMap<String, ClassInfo> fields = translateFields(
                ci.scope, classDef.getInstanceFields());
        // TODO: do we need this?
        if (ci.isInnerClass()) {
            fields.put("this$0", ci.getOuterClass());
        }
        final HashMap<String, ClassInfo> staticFields = translateFields(
                ci.scope, classDef.getStaticFields());
        return ClassDetail.create(baseType, interfaces, accessFlags, methods, fields, staticFields, isFramework);
    }

    private MethodInfo translateMethod(ClassInfo ci, Method method) {
        final ClassInfo retType = Dalvik.findOrCreateClass(ci.scope, method.getReturnType());
        final ImmutableList<ClassInfo> paramTypes = findOrCreateClasses(ci.scope, method.getParameterTypes());
        final MethodSignature signature = new MethodSignature(method.getName(), paramTypes);
        final int accessFlags = translateAccessFlags(method.getAccessFlags());
        final MethodInfo mi = new MethodInfo(ci, signature, retType, accessFlags);
        Log.msg("Translating method: %s", mi.toString());

        if (translateInstructions) {
            final MethodImplementation impl = method.getImplementation();
            // Decode instructions
            if (impl != null) {
                MethodImplementationTranslator mit = new MethodImplementationTranslator(ci.scope, resolver);
                mit.translate(mi, impl);
            }
        }

        return mi;
    }

    private ImmutableList<MethodInfo> translateMethods(ClassInfo ci, Iterable<? extends Method> methods) {
        ImmutableList.Builder<MethodInfo> builder = ImmutableList.builder();
        for (Method method : methods) {
            // TODO(iceboy): Put synthetic into MethodSignature, as they have the same name as non-synthetic methods.
            if (AccessFlags.SYNTHETIC.isSet(method.getAccessFlags())) {
                continue;
            }
            builder.add(translateMethod(ci, method));
        }
        return builder.build();
    }

    private HashMap<String, ClassInfo> translateFields(
            Scope scope, Iterable<? extends Field> fields) {
        HashMap<String, ClassInfo> result = new HashMap<String, ClassInfo>();
        for (Field field: fields) {
            // TODO access flags and initial value are ignored
            result.put(field.getName(), Dalvik.findOrCreateClass(scope, field.getType()));
        }
        return result;
    }

    public static int translateAccessFlags(int accessFlags) {
        int f = 0;
        f |= (AccessFlags.ABSTRACT.isSet(accessFlags) ? Modifier.ABSTRACT : 0);
//        f |= (AccessFlags.ANNOTATION.isSet(accessFlags) ? Modifier.ANNOTATION : 0);
//        f |= (AccessFlags.BRIDGE.isSet(accessFlags) ? Modifier.BRIDGE : 0);
//        f |= (AccessFlags.CONSTRUCTOR.isSet(accessFlags) ? Modifier.CONSTRUCTOR : 0);
//        f |= (AccessFlags.DECLARED_SYNCHRONIZED.isSet(accessFlags) ? Modifier.DECLARED_SYNCHRONIZED : 0);
//        f |= (AccessFlags.ENUM.isSet(accessFlags) ? Modifier.ENUM : 0);
        f |= (AccessFlags.FINAL.isSet(accessFlags) ? Modifier.FINAL : 0);
        f |= (AccessFlags.INTERFACE.isSet(accessFlags) ? Modifier.INTERFACE : 0);
        f |= (AccessFlags.NATIVE.isSet(accessFlags) ? Modifier.NATIVE : 0);
        f |= (AccessFlags.PRIVATE.isSet(accessFlags) ? Modifier.PRIVATE : 0);
        f |= (AccessFlags.PROTECTED.isSet(accessFlags) ? Modifier.PROTECTED : 0);
        f |= (AccessFlags.PUBLIC.isSet(accessFlags) ? Modifier.PUBLIC : 0);
        f |= (AccessFlags.STATIC.isSet(accessFlags) ? Modifier.STATIC : 0);
        f |= (AccessFlags.STRICTFP.isSet(accessFlags) ? Modifier.STRICT : 0);
        f |= (AccessFlags.SYNCHRONIZED.isSet(accessFlags) ? Modifier.SYNCHRONIZED : 0);
//        f |= (AccessFlags.SYNTHETIC.isSet(accessFlags) ? Modifier.SYNTHETIC : 0);
        f |= (AccessFlags.TRANSIENT.isSet(accessFlags) ? Modifier.TRANSIENT : 0);
//        f |= (AccessFlags.VARARGS.isSet(accessFlags) ? Modifier.VARARGS : 0);
        f |= (AccessFlags.VOLATILE.isSet(accessFlags) ? Modifier.VOLATILE : 0);
        return f;
    }

    public static MethodInfo translateMethodReference(Scope scope, MethodReference method, boolean isStatic) {
        int accessFlags = isStatic ? Modifier.STATIC : 0;
        ClassInfo ci = Dalvik.findOrCreateClass(scope, method.getDefiningClass());
        ClassInfo retType = Dalvik.findOrCreateClass(scope, method.getReturnType());
        ImmutableList<ClassInfo> paramTypes = findOrCreateClasses(scope, method.getParameterTypes());
        return new MethodInfo(ci, new MethodSignature(method.getName(), paramTypes), retType, accessFlags);
    }

    private static ImmutableList<ClassInfo> findOrCreateClasses(Scope scope, List<? extends CharSequence> l) {
        ImmutableList.Builder<ClassInfo> builder = ImmutableList.builder();
        for (CharSequence s : l) {
            builder.add(Dalvik.findOrCreateClass(scope, s.toString()));
        }
        return builder.build();
    }
}
