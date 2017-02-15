package patdroid.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * A scope is a container of classes. A scope can be used to represent different entities, which
 * entirely depends on the upper layer program analysis task.
 * Just to list a few things that a scope can stand for:
 * An APK file (as a whole), a Dex file (consider multi-dex), android framework classes (framework.jar)
 * Java core library classes (java.*), 3rd party library code (e.g. with a specific package name).
 * <p>
 * Overall it is suggested multiple scopes stay disjoint.
 */
public class Scope {
    private final HashMap<String, ClassInfo> classes = new HashMap<String, ClassInfo>();
    public final ClassInfo rootObject = findOrCreateClass(java.lang.Object.class);
    public final ClassInfo primitiveWide = findOrCreateClass("AndroidWide");
    public final ClassInfo primitiveVoid = findOrCreateClass(void.class);
    public final ClassInfo primitiveLong = findOrCreateClass(long.class);
    public final ClassInfo primitiveBoolean = findOrCreateClass(boolean.class);
    public final ClassInfo primitiveByte = findOrCreateClass(byte.class);
    public final ClassInfo primitiveInt = findOrCreateClass(int.class);
    public final ClassInfo primitiveShort = findOrCreateClass(short.class);
    public final ClassInfo primitiveChar = findOrCreateClass(char.class);
    public final ClassInfo primitiveDouble = findOrCreateClass(double.class);
    public final ClassInfo primitiveFloat = findOrCreateClass(float.class);
    public final ImmutableSet<ClassInfo> primitives =
            ImmutableSet.of(
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

    public ClassInfo findClass(String fullName) {
        return classes.get(fullName);
    }

    private ClassInfo createClass(String fullName) {
        ClassInfo ci = new ClassInfo(this, fullName);
        classes.put(fullName, ci);
        if (ci.isArray()) {
            findOrCreateClass(fullName.substring(1));
        }
        return ci;
    }

    public boolean hasClass(ClassInfo ci) {
        return classes.containsValue(ci);
    }

    public Collection<ClassInfo> getAllClasses() {
        return classes.values();
    }

    public Collection<String> getAllClassNames() {
        return classes.keySet();
    }

    public ClassInfo findOrCreateClass(String fullName) {
        ClassInfo u = findClass(fullName);
        return (u == null ? createClass(fullName) : u);
    }

    /**
     * Find or create a class representation
     *
     * @param c the java Class object
     * @return the class found or just created
     */
    public ClassInfo findOrCreateClass(Class<?> c) {
        return (c == null ? null : findOrCreateClass(c.getName()));
    }

    /**
     * Find or create a list of class representations
     *
     * @param classes the list of java Class objects
     * @return the list of class representations
     */
    public ImmutableList<ClassInfo> findOrCreateClasses(Class<?>[] classes) {
        ImmutableList.Builder<ClassInfo> builder = ImmutableList.builder();
        for (Class<?> clazz : classes) {
            builder.add(findOrCreateClass(clazz));
        }
        return builder.build();
    }
}
