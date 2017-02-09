package patdroid.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * The detail of the class is available in the current Java environment.
 * Load that with standard Java reflection
 */
public class ReflectionClassDetailLoader extends ClassDetailLoader {

    @Override
    public void load(ClassInfo ci) throws ClassNotFoundException,
            ExceptionInInitializerError, NoClassDefFoundError {
        String fullName = ci.toString();
        Class<?> c = null;
        if (ci.isArray()) {
            c = int[].class; // use int[] for generic array
        } else {
            c = Class.forName(fullName);
        }
        ClassInfo superClass = ClassInfo.globalScope.findOrCreateClass(c.getSuperclass());
		/*
		 * Java spec: When an interface has no direct SuperInterface , it will
		 * create abstract public method for all those public methods present in
		 * the Object class
		 */
        if (superClass == null && c.isInterface()) {
            superClass = ClassInfo.globalScope.rootObject;
        }

        ArrayList<MethodInfo> methods = new ArrayList<MethodInfo>();

        // transform fields
        boolean hasStaticFields = false;
        Field[] raw_fields = c.getDeclaredFields();
        HashMap<String, ClassInfo> fields = new HashMap<String, ClassInfo>();
        HashMap<String, ClassInfo> staticFields = new HashMap<String, ClassInfo>();
        for (Field f : raw_fields) {
            if (Modifier.isStatic(f.getModifiers())) {
                staticFields.put(f.getName(), ClassInfo.globalScope.findOrCreateClass(f.getType()));
                hasStaticFields = true;
            } else {
                fields.put(f.getName(), ClassInfo.globalScope.findOrCreateClass(f.getType()));
            }
        }
        if (hasStaticFields) {
            methods.add(new MethodInfo(ci, MethodInfo.STATIC_INITIALIZER,
                    ClassInfo.globalScope.primitiveVoid, new ClassInfo[0], Modifier.STATIC));
        }
        // TODO: do we actually need this?? I think the synthetic fields are included in declared fields
        // see http://www.public.iastate.edu/~java/docs/guide/innerclasses/html/innerclasses.doc.html
        if (ci.isInnerClass()) {
            // say A is inside B and B is inside C
            // then in C, this$0 is A.this, this$1 is B.this
            fields.put("this$0", ci.getOuterClass());
        }

        // transform the class methods
        for (Method m : c.getDeclaredMethods()) {
            String name = m.getName();
            ClassInfo returnType = ClassInfo.globalScope.findOrCreateClass(m.getReturnType());
            ClassInfo[] paramTypes = ClassInfo.globalScope.findOrCreateClass(m.getParameterTypes());
            methods.add(new MethodInfo(ci, name, returnType, paramTypes,
                    m.getModifiers()));
        }

        // transform the class constructors
        for (Constructor<?> m : c.getDeclaredConstructors()) {
            String name = MethodInfo.CONSTRUCTOR;
            ClassInfo returnType = ClassInfo.globalScope.primitiveVoid;
            ClassInfo[] paramTypes = ClassInfo.globalScope.findOrCreateClass(m.getParameterTypes());
            methods.add(new MethodInfo(ci, name, returnType,
                    paramTypes, m.getModifiers()));
        }

        // transform interfaces
        Class<?>[] raw_interfaces = c.getInterfaces();
        ClassInfo[] interfaces = new ClassInfo[raw_interfaces.length];
        for (int i = 0; i < raw_interfaces.length; ++i) {
            interfaces[i] = ClassInfo.globalScope.findOrCreateClass(raw_interfaces[i]);
        }

        // loaded as a framework class
        setDetails(ci, createDetail(superClass, interfaces,
                c.getModifiers(), methods.toArray(new MethodInfo[methods.size()]),
                fields, staticFields, true));
    }
}
