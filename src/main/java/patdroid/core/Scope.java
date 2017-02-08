package patdroid.core;

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
    private final ConcurrentHashMap<String, ClassInfo> classes = new ConcurrentHashMap<String, ClassInfo>();
    public ClassInfo findClass(String fullName) { return classes.get(fullName); }
    public ClassInfo createClass(String fullName) { ClassInfo ci = new ClassInfo(fullName); classes.put(fullName, ci); return ci; }
    public boolean hasClass(ClassInfo ci) { return classes.containsValue(ci); }
    public Collection<ClassInfo> getAllClasses() { return classes.values(); }
    public Collection<String> getAllClassNames() { return classes.keySet(); }
    public ClassInfo findOrCreateClass(String fullName) {
        ClassInfo u = findClass(fullName);
        return (u == null ? createClass(fullName) : u);
    }
}
