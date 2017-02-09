package patdroid.core;

import com.google.common.collect.ImmutableSet;

/**
 * Java scope.
 */
public class JavaScope extends Scope {
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

    public JavaScope() {
        reset();
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
        super.reset();
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
