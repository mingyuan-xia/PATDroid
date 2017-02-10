package patdroid.core;

import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A scope is a container of classes. A scope can be used to represent different entities, which
 * entirely depends on the upper layer program analysis task.
 * Just to like a few things that a scope can stand for:
 * An APK file (as a whole), a Dex file (consider multi-dex), android framework classes (framework.jar)
 * Java core library classes (java.*), app code (e.g. with a specific package name).
 *
 * Overall it is suggested multiple scopes stay disjoint.
 */
public class Scope {
    // TODO(iceboy): Make these final.
    public ClassInfo rootObject;
    public ClassInfo primitiveWide;
    public ClassInfo primitiveVoid;
    public ClassInfo primitiveLong;
    public ClassInfo primitiveBoolean;
    public ClassInfo primitiveByte;
    public ClassInfo primitiveInt;
    public ClassInfo primitiveShort;
    public ClassInfo primitiveChar;
    public ClassInfo primitiveDouble;
    public ClassInfo primitiveFloat;
    public ImmutableSet<ClassInfo> primitives;

    public Scope() {
        reset();
    }

    private final ConcurrentHashMap<String, ClassInfo> classes = new ConcurrentHashMap<String, ClassInfo>();
    public ClassInfo findClass(String fullName) { return classes.get(fullName); }
    public ClassInfo createClass(String fullName) {
        ClassInfo ci = new ClassInfo(this, fullName);
        classes.put(fullName, ci);
        if (ci.isArray()) {
            findOrCreateClass(fullName.substring(1));
        }
        return ci;
    }
    public boolean hasClass(ClassInfo ci) { return classes.containsValue(ci); }
    public Collection<ClassInfo> getAllClasses() { return classes.values(); }
    public Collection<String> getAllClassNames() { return classes.keySet(); }
    public ClassInfo findOrCreateClass(String fullName) {
        ClassInfo u = findClass(fullName);
        return (u == null ? createClass(fullName) : u);
    }

    /**
     * Find or create a class representation
     * @param c the java Class object
     * @return the class found or just created
     */
    public ClassInfo findOrCreateClass(Class<?> c) {
        return (c == null ? null : findOrCreateClass(c.getName()));
    }

    /**
     * Find or create a list of class representations
     * @param l the list of java Class objects
     * @return the list of class representations
     */
    public ClassInfo[] findOrCreateClass(Class<?>[] l) {
        final ClassInfo[] a = new ClassInfo[l.length];
        for (int i = 0; i < a.length; ++i) {
            a[i] = findOrCreateClass(l[i]);
        }
        return a;
    }

    // TODO(iceboy): Remove this method.
    public void reset() {
        classes.clear();
        rootObject = findOrCreateClass(java.lang.Object.class);
        primitiveWide = findOrCreateClass("AndroidWide");
        primitiveVoid = findOrCreateClass(void.class);
        primitiveLong = findOrCreateClass(long.class);
        primitiveBoolean = findOrCreateClass(boolean.class);
        primitiveByte = findOrCreateClass(byte.class);
        primitiveInt = findOrCreateClass(int.class);
        primitiveShort = findOrCreateClass(short.class);
        primitiveChar = findOrCreateClass(char.class);
        primitiveDouble = findOrCreateClass(double.class);
        primitiveFloat = findOrCreateClass(float.class);
        primitives = ImmutableSet.of(
                primitiveWide,
                primitiveVoid,
                primitiveLong,
                primitiveBoolean,
                primitiveByte,
                primitiveInt,
                primitiveShort,
                primitiveChar,
                primitiveDouble,
                primitiveFloat);
    }
}
