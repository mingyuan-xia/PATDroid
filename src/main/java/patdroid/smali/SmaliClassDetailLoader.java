package patdroid.smali;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
import patdroid.core.ClassInfo;
import patdroid.core.ClassDetail;
import patdroid.core.MethodInfo;
import patdroid.dalvik.InvocationResolver;
import patdroid.util.Log;

import patdroid.core.ClassDetailLoader;
import patdroid.dalvik.Dalvik;

/**
 * Load classes, methods, fields and instructions from an APK file with SMALI
 * https://github.com/JesusFreke/smali
 */
public class SmaliClassDetailLoader extends ClassDetailLoader {
    public final InvocationResolver resolver;

    @Override
    public void load(ClassInfo ci) throws ClassNotFoundException,
            ExceptionInInitializerError, NoClassDefFoundError
    {
        for (final ClassDef classDef: dexFile.getClasses()) {
            if (Dalvik.toCanonicalName(classDef.getType()).equals(ci.fullName)) {
                ClassDetail detail = translateClassDef(ci, classDef);
                setDetails(ci, detail);
                if (translateInstructions) {
                    resolver.resolveAll();
                }
            }
        }
        throw new ClassNotFoundException("" + ci.fullName + " not found in the dex file");
    }

    public static SmaliClassDetailLoader getFrameworkClassLoader(int apiLevel) {
        File f = new File(Settings.frameworkClassesFolder, "android-" + apiLevel + ".dex");
        if (!f.exists())
            return null;
        DexFile dex = null;
        try {
            dex = DexFileFactory.loadDexFile(f, Settings.apiLevel);
        } catch (IOException e) {
            Log.err("failed to load framework classes");
            Log.err(e);
            return null;
        }
        SmaliClassDetailLoader ldr = new SmaliClassDetailLoader(dex, false);
        ldr.isFramework = true;
        return ldr;
    }

    /**
     * Parse an apk file and extract all classes, methods, fields and optionally instructions
     */
    public void loadAll() {
        for (final ClassDef classDef: dexFile.getClasses()) {
            ClassInfo ci = Dalvik.findOrCreateClass(classDef.getType());
            ClassDetail detail = translateClassDef(ci, classDef);
            setDetails(ci, detail);
        }
        if (translateInstructions) {
            resolver.resolveAll();
        }
    }

    private DexFile dexFile;
    private final boolean translateInstructions;
    private boolean isFramework = false;

    public SmaliClassDetailLoader(ZipFile apkFile) {
        this(apkFile, true);
    }

    public SmaliClassDetailLoader(ZipFile apkFile, boolean translateInstructions) {
        final ZipEntry dexEntry = apkFile.getEntry("classes.dex");
        this.translateInstructions = translateInstructions;
        this.resolver = translateInstructions ? new InvocationResolver() : null;
        if (dexEntry == null) {
            Log.err("Source apk does not have a classes.dex!");
        }
        final Opcodes opcodes = Opcodes.forApi(Settings.apiLevel);
        try {
            dexFile = DexBackedDexFile.fromInputStream(opcodes,
                    new BufferedInputStream(apkFile.getInputStream(dexEntry)));
        } catch (IOException e) {
            Log.err("failed to process the source apk file");
            Log.err(e);
        }
    }

    public SmaliClassDetailLoader(DexFile dexFile, boolean translateInstructions) {
        this.dexFile = dexFile;
        this.translateInstructions = translateInstructions;
        this.resolver = translateInstructions ? new InvocationResolver() : null;
    }

    private ClassDetail translateClassDef(
            ClassInfo ci, ClassDef classDef) {
        ClassInfo superClass;
        if (classDef.getSuperclass() == null) {
            superClass = null; // for java.lang.Object
        } else {
            superClass = Dalvik.findOrCreateClass(classDef.getSuperclass());
        }
        final ClassInfo[] interfaces = findOrCreateClass(classDef.getInterfaces());
        final int accessFlags = translateAccessFlags(classDef.getAccessFlags());
        final MethodInfo[] methods = translateMethods(ci,
                classDef.getMethods());
        final HashMap<String, ClassInfo> fields = translateFields(
                classDef.getInstanceFields());
        // TODO: do we need this?
        if (ci.isInnerClass()) {
            fields.put("this$0", ci.getOuterClass());
        }
        final HashMap<String, ClassInfo> staticFields = translateFields(
                classDef.getStaticFields());
        return createDetail(superClass, interfaces,
                accessFlags, methods, fields, staticFields, isFramework);
    }

    private MethodInfo translateMethod(ClassInfo ci, Method method) {
        final ClassInfo retType = Dalvik.findOrCreateClass(method.getReturnType());
        final ClassInfo[] paramTypes = findOrCreateClass(method.getParameterTypes());
        final int accessFlags = translateAccessFlags(method.getAccessFlags());
        final MethodInfo mi = new MethodInfo(ci, method.getName(),
                retType, paramTypes, accessFlags);
        Log.msg("Translating method: %s", mi.toString());

        if (translateInstructions) {
            final MethodImplementation impl = method.getImplementation();
            // Decode instructions
            if (impl != null) {
                MethodImplementationTranslator mit = new MethodImplementationTranslator(resolver);
                mit.translate(mi, impl);
            }
        }

        return mi;
    }

    private MethodInfo[] translateMethods(ClassInfo ci,
                                          Iterable<? extends Method> methods) {
        ArrayList<MethodInfo> result = new ArrayList<MethodInfo>();
        for (Method method: methods) {
            result.add(translateMethod(ci, method));
        }
        return result.toArray(new MethodInfo[result.size()]);
    }

    private static HashMap<String, ClassInfo> translateFields(
            Iterable<? extends Field> fields) {
        HashMap<String, ClassInfo> result = new HashMap<String, ClassInfo>();
        for (Field field: fields) {
            // TODO access flags and initial value are ignored
            result.put(field.getName(), Dalvik.findOrCreateClass(field.getType()));
        }
        return result;
    }

    public static int translateAccessFlags(int accessFlags) {
        int f = 0;
        f |= (AccessFlags.ABSTRACT.isSet(accessFlags) ? Modifier.ABSTRACT : 0);
//		f |= (AccessFlags.ANNOTATION.isSet(accessFlags) ? Modifier.ANNOTATION : 0);
//		f |= (AccessFlags.BRIDGE.isSet(accessFlags) ? Modifier.BRIDGE : 0);
//		f |= (AccessFlags.CONSTRUCTOR.isSet(accessFlags) ? Modifier.CONSTRUCTOR : 0);
//		f |= (AccessFlags.DECLARED_SYNCHRONIZED.isSet(accessFlags) ? Modifier.DECLARED_SYNCHRONIZED : 0);
//		f |= (AccessFlags.ENUM.isSet(accessFlags) ? Modifier.ENUM : 0);
        f |= (AccessFlags.FINAL.isSet(accessFlags) ? Modifier.FINAL : 0);
        f |= (AccessFlags.INTERFACE.isSet(accessFlags) ? Modifier.INTERFACE : 0);
        f |= (AccessFlags.NATIVE.isSet(accessFlags) ? Modifier.NATIVE : 0);
        f |= (AccessFlags.PRIVATE.isSet(accessFlags) ? Modifier.PRIVATE : 0);
        f |= (AccessFlags.PROTECTED.isSet(accessFlags) ? Modifier.PROTECTED : 0);
        f |= (AccessFlags.PUBLIC.isSet(accessFlags) ? Modifier.PUBLIC : 0);
        f |= (AccessFlags.STATIC.isSet(accessFlags) ? Modifier.STATIC : 0);
        f |= (AccessFlags.STRICTFP.isSet(accessFlags) ? Modifier.STRICT : 0);
        f |= (AccessFlags.SYNCHRONIZED.isSet(accessFlags) ? Modifier.SYNCHRONIZED : 0);
//		f |= (AccessFlags.SYNTHETIC.isSet(accessFlags) ? Modifier.SYNTHETIC : 0);
        f |= (AccessFlags.TRANSIENT.isSet(accessFlags) ? Modifier.TRANSIENT : 0);
//		f |= (AccessFlags.VARARGS.isSet(accessFlags) ? Modifier.VARARGS : 0);
        f |= (AccessFlags.VOLATILE.isSet(accessFlags) ? Modifier.VOLATILE : 0);
        return f;
    }

    static MethodInfo translateMethodReference(MethodReference method, int accessFlags) {
        ClassInfo ci = Dalvik.findOrCreateClass(method.getDefiningClass());
        ClassInfo retType = Dalvik.findOrCreateClass(method.getReturnType());
        ClassInfo[] paramTypes = findOrCreateClass(method.getParameterTypes());
        return new MethodInfo(ci, method.getName(),
                retType, paramTypes, accessFlags);
    }

    public static ClassInfo[] findOrCreateClass(Collection<? extends CharSequence> l) {
        ClassInfo[] ret = new ClassInfo[l.size()];
        int i = 0;
        for (CharSequence s : l) {
            ret[i] = Dalvik.findOrCreateClass(s.toString());
            ++i;
        }
        return ret;
    }
}
