/*
* Copyright 2014 Mingyuan Xia (http://mxia.me) and contributors
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
* Contributors:
*   Mingyuan Xia
*   Lu Gong
*/

package patdroid.core;

import java.util.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import patdroid.util.Log;

/**
 * The details of a class, including its methods, fields, inheritance relationship.
 * These details are only available when the class is loaded.
 * The details of a class are only supposed to be filled by class loader.
 */
public final class ClassDetail {
    public final ClassInfo baseType;
    public final ImmutableList<ClassInfo> interfaces;
    public final int accessFlags;
    /**
     * Primary index, from a full method signature (with return type) to exactly one possible method
     */
    public final ImmutableMap<FullMethodSignature, MethodInfo> methods;
    /**
     * Secondary index, from a method signature (without return type) to multiple possible methods
     */
    public final ImmutableMultimap<MethodSignature, MethodInfo> methodsIndex;
    public final ImmutableMap<String, ClassInfo> fields;
    public final ImmutableMap<String, ClassInfo> staticFields;
    public final boolean isFrameworkClass;

    public static final class Builder {
        ClassInfo baseType;
        ImmutableList<ClassInfo> interfaces;
        int accessFlags;
        ImmutableMap<FullMethodSignature, MethodInfo> methods;
        ImmutableMultimap<MethodSignature, MethodInfo> methodsIndex;
        ImmutableMap<String, ClassInfo> fields;
        ImmutableMap<String, ClassInfo> staticFields;
        boolean isFrameworkClass;
        public Builder() {
            baseType = null;
            interfaces = ImmutableList.<ClassInfo>of();
            accessFlags = 0;
            methods = ImmutableMap.<FullMethodSignature, MethodInfo>of();
            methodsIndex = ImmutableMultimap.<MethodSignature, MethodInfo>of();
            fields = ImmutableMap.<String, ClassInfo>of();
            isFrameworkClass = true;
        }

        public Builder setBaseType(ClassInfo baseType) {
            this.baseType = baseType;
            return this;
        }
        public Builder setInterfaces(List<ClassInfo> interfaces) {
            this.interfaces = ImmutableList.copyOf(interfaces);
            return this;
        }
        public Builder setAccessFlags(int accessFlags) {
            this.accessFlags = accessFlags;
            return this;
        }
        public Builder setAllMethods(List<MethodInfo> methods) {
            // build secondary index
            ImmutableMultimap.Builder<MethodSignature, MethodInfo> indexBuilder = ImmutableMultimap.builder();
            ImmutableMap.Builder<FullMethodSignature, MethodInfo> methodsBuilder = ImmutableMap.builder();
            for (MethodInfo method : methods) {
                methodsBuilder.put(method.signature, method);
                indexBuilder.put(method.signature.partialSignature, method);
            }
            this.methods = methodsBuilder.build();
            this.methodsIndex = indexBuilder.build();
            return this;
        }
        public Builder setMethods(Map<FullMethodSignature, MethodInfo> methods) {
            this.methods = ImmutableMap.copyOf(methods);
            return this;
        }
        public Builder setMethodsIndex(Multimap<MethodSignature, MethodInfo> methodsIndex) {
            this.methodsIndex = ImmutableMultimap.copyOf(methodsIndex);
            return this;
        }
        public Builder setFields(Map<String, ClassInfo> fields) {
            this.fields = ImmutableMap.copyOf(fields);
            return this;
        }
        public Builder setStaticFields(Map<String, ClassInfo> staticFields) {
            this.staticFields = ImmutableMap.copyOf(staticFields);
            return this;
        }
        public Builder setIsFrameworkClass(boolean isFrameworkClass) {
            this.isFrameworkClass = isFrameworkClass;
            return this;
        }
        public ClassDetail build() {
            return new ClassDetail(this);
        }
    }

    /**
     * A list of classes inherit this class.
     * Note that derivedClasses only covers loaded classes.
     * Use with care!
     */
    public ArrayList<ClassInfo> derivedClasses = new ArrayList<ClassInfo>();

    /**
     * Create a details class from a builder
     */
    private ClassDetail(Builder builder) {
        this.accessFlags = builder.accessFlags;
        this.baseType = builder.baseType;
        this.interfaces = builder.interfaces;
        this.methods = builder.methods;
        this.methodsIndex = builder.methodsIndex;
        this.fields = builder.fields;
        this.staticFields = builder.staticFields;
        this.isFrameworkClass = builder.isFrameworkClass;
    }

    /**
     * Get the type of a non-static field. This functions will look into the base class.
     * @param fieldName the field name
     * @return the type of the field, or null if the field is not found
     */
    public ClassInfo getFieldType(String fieldName) {
        ClassInfo r = fields.get(fieldName);
        if (r != null) {
            return r;
        } else {
            if (baseType == null) {
                Log.warnwarn("failed to find field: "+ fieldName);
                return null;
            }
            return baseType.mutableDetail.getFieldType(fieldName);
        }
    }

    /**
     * Get the type of a static field. This functions will look into the base class.
     * @param fieldName the field name
     * @return the type of the field, or null if the field is not found
     */
    public ClassInfo getStaticFieldType(String fieldName) {
        ClassInfo r = staticFields.get(fieldName);
        if (r != null) {
            return r;
        } else {
            if (baseType == null) {
                Log.warnwarn("failed to find static field: "+ fieldName);
                return null;
            }
            return baseType.mutableDetail.getStaticFieldType(fieldName);
        }
    }

    /**
     * Find a concrete method given a method prototype
     *
     * @param signature The signature of a method
     * @return The method matching the prototype in the class
     */
    public MethodInfo findMethod(FullMethodSignature signature) {
        Deque<ClassDetail> q = new ArrayDeque<ClassDetail>();
        q.push(this);
        while (!q.isEmpty()) {
            ClassDetail detail = q.pop();
            MethodInfo mi = detail.methods.get(signature);
            if (mi != null) {
                return mi;
            }
            if (detail.baseType != null)
                q.push(detail.baseType.mutableDetail);
            for (ClassInfo i : detail.interfaces)
                q.push(i.mutableDetail);
        }
        return null;
    }

    /**
     * Find all concrete methods given a name
     * @param name The method name
     * @return All rebound methods
     */
    public MethodInfo[] findMethods(String name) {
        ArrayList<MethodInfo> result = new ArrayList<MethodInfo>();
        ArrayDeque<ClassDetail> q = new ArrayDeque<ClassDetail>();
        q.push(this);
        while (!q.isEmpty()) {
            ClassDetail detail = q.pop();
            MethodInfo[] mis = detail.findMethodsHere(name);

            for (MethodInfo mi : mis) {
                boolean overrided = false;
                for (MethodInfo mi0 : result) {
                    // N.B. mi and mi0 may belong to different super class or
                    // interfaces that have no inheritance relationship
                    if (mi0.canOverride(mi)) {
                        overrided = true;
                        break;
                    }
                }
                if (!overrided)
                    result.add(mi);
            }

            if (detail.baseType != null)
                q.push(detail.baseType.mutableDetail);
            for (ClassInfo i : detail.interfaces)
                q.push(i.mutableDetail);
        }
        return result.toArray(new MethodInfo[result.size()]);
    }

    /**
     * Find all methods that is only in the declaration of this class
     * @param name The method name
     * @return The real methods
     */
    public MethodInfo[] findMethodsHere(String name) {
        ArrayList<MethodInfo> result = new ArrayList<MethodInfo>();
        for (MethodInfo m : methods.values()) {
            if (m.signature.partialSignature.name.equals(name)) {
                result.add(m);
            }
        }
        return result.toArray(new MethodInfo[result.size()]);
    }

    /**
     * TypeA is convertible to TypeB if and only if TypeB is an (indirect)
     * base type or an (indirect) interface of TypeA.
     *
     * @param type typeB
     * @return if this class can be converted to the other.
     */
    public final boolean isConvertibleTo(ClassInfo type) {
        ClassDetail that = type.mutableDetail;
        if (this == that) {
            return true;
        }
        if (baseType != null && baseType.isConvertibleTo(type)) {
            return true;
        }
        for (ClassInfo c : interfaces) {
            if (c.isConvertibleTo(type)) {
                return true;
            }
        }
        return false;
        // derivedClasses is not that reliable
        // return type.derivedClasses.contains(this);
    }

    public ClassDetail changeBaseType(ClassInfo baseType) {
        Builder builder = new Builder();
        ClassDetail details = builder.setBaseType(baseType)
                .setInterfaces(interfaces)
                .setAccessFlags(accessFlags)
                .setMethods(methods)
                .setMethodsIndex(methodsIndex)
                .setFields(fields)
                .setStaticFields(staticFields)
                .setIsFrameworkClass(isFrameworkClass)
                .build();
        details.derivedClasses = this.derivedClasses;
        return details;
    }

    public final void updateDerivedClasses(ClassInfo ci) {
        ArrayDeque<ClassDetail> a = new ArrayDeque<ClassDetail>();
        if (baseType != null)
            a.add(baseType.mutableDetail);
        for (ClassInfo i : interfaces) {
            a.add(i.mutableDetail);
        }
        while (!a.isEmpty()) {
            ClassDetail detail = a.pop();
            detail.derivedClasses.add(ci);
            detail.derivedClasses.addAll(derivedClasses);
            if (detail.baseType != null)
                a.add(detail.baseType.mutableDetail);
            for (ClassInfo i : detail.interfaces) {
                a.add(i.mutableDetail);
            }
        }
    }

    public final void removeDerivedClasses(ClassInfo ci) {
        ArrayDeque<ClassDetail> a = new ArrayDeque<ClassDetail>();
        if (baseType != null)
            a.add(baseType.mutableDetail);
        for (ClassInfo i : interfaces) {
            a.add(i.mutableDetail);
        }
        while (!a.isEmpty()) {
            ClassDetail detail = a.pop();
            detail.derivedClasses.remove(ci);
            detail.derivedClasses.removeAll(derivedClasses);
            if (detail.baseType != null)
                a.add(detail.baseType.mutableDetail);
            for (ClassInfo i : detail.interfaces) {
                a.add(i.mutableDetail);
            }
        }
    }
}
