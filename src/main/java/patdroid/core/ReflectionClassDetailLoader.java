package patdroid.core;

import com.google.common.collect.ImmutableList;

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
    private final Scope scope;

    public ReflectionClassDetailLoader(Scope scope) {
        this.scope = scope;
    }

    @Override
    public void load(ClassInfo type) throws ClassNotFoundException,
            ExceptionInInitializerError, NoClassDefFoundError {
        String fullName = type.toString();
        Class<?> c = null;
        if (type.isArray()) {
            c = int[].class; // use int[] for generic array
        } else {
            c = Class.forName(fullName);
        }
        ClassInfo baseType = scope.findOrCreateClass(c.getSuperclass());
		/*
		 * Java spec: When an interface has no direct SuperInterface , it will
		 * create abstract public method for all those public methods present in
		 * the Object class
		 */
        if (baseType == null && c.isInterface()) {
            baseType = scope.rootObject;
        }

        ArrayList<MethodInfo> methods = new ArrayList<MethodInfo>();

        // transform fields
        boolean hasStaticFields = false;
        Field[] raw_fields = c.getDeclaredFields();
        HashMap<String, ClassInfo> fields = new HashMap<String, ClassInfo>();
        HashMap<String, ClassInfo> staticFields = new HashMap<String, ClassInfo>();
        for (Field f : raw_fields) {
            if (Modifier.isStatic(f.getModifiers())) {
                staticFields.put(f.getName(), scope.findOrCreateClass(f.getType()));
                hasStaticFields = true;
            } else {
                fields.put(f.getName(), scope.findOrCreateClass(f.getType()));
            }
        }
        if (hasStaticFields) {
            methods.add(new MethodInfo(type, MethodSignature.of(MethodInfo.STATIC_INITIALIZER),
                    scope.primitiveVoid, Modifier.STATIC));
        }
        // TODO: do we actually need this?? I think the synthetic fields are included in declared fields
        // see http://www.public.iastate.edu/~java/docs/guide/innerclasses/html/innerclasses.doc.html
        if (type.isInnerClass()) {
            // say A is inside B and B is inside C
            // then in C, this$0 is A.this, this$1 is B.this
            fields.put("this$0", type.getOuterClass());
        }

        // transform the class methods
        for (Method m : c.getDeclaredMethods()) {
            MethodSignature signature = MethodSignature.of(scope, m.getName(), m.getParameterTypes());
            ClassInfo returnType = scope.findOrCreateClass(m.getReturnType());
            methods.add(new MethodInfo(type, signature, returnType, m.getModifiers()));
        }

        // transform the class constructors
        for (Constructor<?> m : c.getDeclaredConstructors()) {
            MethodSignature signature = MethodSignature.of(scope, MethodInfo.CONSTRUCTOR, m.getParameterTypes());
            ClassInfo returnType = scope.primitiveVoid;
            methods.add(new MethodInfo(type, signature, returnType, m.getModifiers()));
        }

        // transform interfaces
        ImmutableList<ClassInfo> interfaces = scope.findOrCreateClasses(c.getInterfaces());

        // loaded as a framework class
        setDetail(type, ClassDetail.create(baseType, interfaces, c.getModifiers(),
                                           methods, fields, staticFields, true));
    }
}
