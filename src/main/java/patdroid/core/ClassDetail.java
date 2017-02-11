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
	public final ImmutableMap<MethodSignature, MethodInfo> methods;
    public final ImmutableMap<String, ClassInfo> fields;
    public final ImmutableMap<String, ClassInfo> staticFields;
    public final boolean isFrameworkClass;

	/**
	 * A list of classes inherit this class.
	 * Note that derivedClasses only covers loaded classes.
	 * Use with care!
	 */
	public ArrayList<ClassInfo> derivedClasses = new ArrayList<ClassInfo>();

	/**
	 * Create a details class for a class.
	 * Only a ClassDetailLoader could construct a ClassDetail
	 * @param baseType its base type
	 * @param interfaces interfaces
	 * @param accessFlags the access flags on the class
	 * @param methods methods, stored in a name-type map
	 * @param fields non-static fields, stored in a name-type map
	 * @param staticFields static fields, stored in a name-type map 
	 * @param isFrameworkClass whether it is a framework class
	 */
	private ClassDetail(ClassInfo baseType, List<ClassInfo> interfaces, int accessFlags,
                        Map<MethodSignature, MethodInfo> methods,
                        Map<String, ClassInfo> fields, Map<String, ClassInfo> staticFields, boolean isFrameworkClass) {
		this.accessFlags = accessFlags;
		this.baseType = baseType;
		this.interfaces = ImmutableList.copyOf(interfaces);
		this.methods = ImmutableMap.copyOf(methods);
		this.fields = ImmutableMap.copyOf(fields);
		this.staticFields = ImmutableMap.copyOf(staticFields);
		this.isFrameworkClass = isFrameworkClass;
	}

	public static ClassDetail create(ClassInfo baseType, List<ClassInfo> interfaces, int accessFlags,
                                     List<MethodInfo> methods, Map<String, ClassInfo> fields,
                                     Map<String, ClassInfo> staticFields, boolean isFrameworkClass) {
	    HashMap<MethodSignature, MethodInfo> methodsBuilder = new HashMap<MethodSignature, MethodInfo>();
	    for (MethodInfo method : methods) {
	        if (methodsBuilder.containsKey(method.signature)) {
	            Log.warn("Duplicate method: " + method);
	            continue;
            }
	        methodsBuilder.put(method.signature, method);
        }
        return new ClassDetail(baseType, interfaces, accessFlags,
                methodsBuilder, fields, staticFields, isFrameworkClass);
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
			return baseType.getDetails().getFieldType(fieldName);
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
			return baseType.getDetails().getStaticFieldType(fieldName);
		}
	}
	
	/**
	 * Find a concrete method given a method prototype
	 * 
	 * @param signature The signature of a method
	 * @return The method matching the prototype in the class
	 */
	public MethodInfo findMethod(MethodSignature signature) {
		Deque<ClassDetail> q = new ArrayDeque<ClassDetail>();
		q.push(this);
		while (!q.isEmpty()) {
			ClassDetail detail = q.pop();
			MethodInfo mi = detail.methods.get(signature);
			if (mi != null) {
				return mi;
			}
			if (detail.baseType != null)
				q.push(detail.baseType.getDetails());
			for (ClassInfo i : detail.interfaces)
				q.push(i.getDetails());
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
				q.push(detail.baseType.getDetails());
			for (ClassInfo i : detail.interfaces)
				q.push(i.getDetails());
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
			if (m.signature.name.equals(name)) {
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
		ClassDetail that = type.getDetails();
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
		ClassDetail details = new ClassDetail(baseType, interfaces, accessFlags,
				methods, fields, staticFields, isFrameworkClass);
		details.derivedClasses = this.derivedClasses;
		return details;
	}
	
	public final void updateDerivedClasses(ClassInfo ci) {
		ArrayDeque<ClassDetail> a = new ArrayDeque<ClassDetail>();
		if (baseType != null)
			a.add(baseType.getDetails());
		for (ClassInfo i : interfaces) {
			a.add(i.getDetails());
		}
		while (!a.isEmpty()) {
			ClassDetail detail = a.pop();
			detail.derivedClasses.add(ci);
			detail.derivedClasses.addAll(derivedClasses);
			if (detail.baseType != null)
				a.add(detail.baseType.getDetails());
			for (ClassInfo i : detail.interfaces) {
				a.add(i.getDetails());
			}
		}
	}

	public final void removeDerivedClasses(ClassInfo ci) {
		ArrayDeque<ClassDetail> a = new ArrayDeque<ClassDetail>();
		if (baseType != null)
			a.add(baseType.getDetails());
		for (ClassInfo i : interfaces) {
			a.add(i.getDetails());
		}
		while (!a.isEmpty()) {
			ClassDetail detail = a.pop();
			detail.derivedClasses.remove(ci);
			detail.derivedClasses.removeAll(derivedClasses);
			if (detail.baseType != null)
				a.add(detail.baseType.getDetails());
			for (ClassInfo i : detail.interfaces) {
				a.add(i.getDetails());
			}
		}
	}
}
