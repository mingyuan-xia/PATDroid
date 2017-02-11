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
import patdroid.util.Log;

/**
 * The details of a class, including its methods, fields, inheritance relationship.
 * These details are only available when the class is loaded. 
 * The details of a class are only supposed to be filled by class loader.
 */
public final class ClassDetail {
	final static ClassDetail missingDetail
		= new ClassDetail(null, ImmutableList.<ClassInfo>of(), 0, new MethodInfo[0],
			new HashMap<String, ClassInfo>(), new HashMap<String, ClassInfo>(), true);
	private final ClassInfo superClass;
    public final ImmutableList<ClassInfo> interfaces;
	public final HashMap<MethodSignature, MethodInfo> methods;
	private final HashMap<String, ClassInfo> fields;
	private final HashMap<String, ClassInfo> staticFields;
	private final boolean isFrameworkClass;
	/**
	 * A list of classes inherit this class.
	 * Note that derivedClasses only covers loaded classes.
	 * Use with care!
	 */
	public ArrayList<ClassInfo> derivedClasses = new ArrayList<ClassInfo>();
	public final int accessFlags;
	/**
	 * Create a details class for a class.
	 * Only a ClassDetailLoader could construct a ClassDetail
	 * @param superClass its base class
	 * @param interfaces interfaces
	 * @param accessFlags the access flags on the class
	 * @param methods methods, stored in a name-type map
	 * @param fields non-static fields, stored in a name-type map
	 * @param staticFields static fields, stored in a name-type map 
	 * @param isFrameworkClass whether it is a framework class
	 */
	ClassDetail(ClassInfo superClass, List<ClassInfo> interfaces,
					   int accessFlags, MethodInfo[] methods,
					   HashMap<String, ClassInfo> fields,
					   HashMap<String, ClassInfo> staticFields, boolean isFrameworkClass) {
		this.accessFlags = accessFlags;
		this.superClass = superClass;
		this.interfaces = ImmutableList.copyOf(interfaces);
		this.methods = new HashMap<MethodSignature, MethodInfo>();
		for (MethodInfo mi : methods) {
			this.methods.put(mi.signature, mi);
		}
		this.fields = fields;
		this.staticFields = new HashMap<String, ClassInfo>(staticFields);
		this.isFrameworkClass = isFrameworkClass;
	}

	/**
	 * Only for {@link #changeSuperClass(ClassInfo)}, should not be called by any other class.
	 */
	private ClassDetail(ClassInfo superClass, List<ClassInfo> interfaces,
				int accessFlags, HashMap<MethodSignature, MethodInfo> methods,
				HashMap<String, ClassInfo> fields,
				HashMap<String, ClassInfo> staticFields, boolean isFrameworkClass) {
		this.accessFlags = accessFlags;
		this.superClass = superClass;
		this.interfaces = ImmutableList.copyOf(interfaces);
		this.methods = new HashMap<MethodSignature, MethodInfo>(methods);
		this.fields = fields;
		this.staticFields = new HashMap<String, ClassInfo>(staticFields);
		this.isFrameworkClass = isFrameworkClass;
	}

	/**
	 *
	 * @return all methods in the class
     */
	public Collection<MethodInfo> getAllMethods() {
		return methods.values();
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
			if (superClass == null) {
				Log.warnwarn("failed to find field: "+ fieldName);
				return null;
			}
			return superClass.getDetails().getFieldType(fieldName);
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
			if (superClass == null) {
				Log.warnwarn("failed to find static field: "+ fieldName);
				return null;
			}
			return superClass.getDetails().getStaticFieldType(fieldName);
		}
	}
	
	/**
	 * Get all non-static fields declared in this class.
	 * @return all non-static fields stored in a name-type map
	 */
	public HashMap<String, ClassInfo> getAllFieldsHere() {
		return fields;
	}
	
	/**
	 * Get all static fields declared in this class.
	 * @return all static fields stored in a name-type map
	 */
	public HashMap<String, ClassInfo> getAllStaticFieldsHere() {
		return staticFields;
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
			if (detail.superClass != null) 
				q.push(detail.superClass.getDetails());
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
			
			if (detail.superClass != null) 
				q.push(detail.superClass.getDetails());
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
	 * superclass or an (indirect) interface of TypeA.
	 *  
	 * @param type typeB
	 * @return if this class can be converted to the other.
	 */
	public final boolean isConvertibleTo(ClassInfo type) {
		ClassDetail that = type.getDetails();
		if (this == that) {
			return true;
		}
		if (superClass != null && superClass.isConvertibleTo(type)) {
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
	
	public final ClassInfo getSuperClass() {
		return superClass;
	}

	public ClassDetail changeSuperClass(ClassInfo superClass) {
		ClassDetail details = new ClassDetail(superClass, interfaces, accessFlags,
				methods, fields, staticFields, isFrameworkClass);
		details.derivedClasses = this.derivedClasses;
		return details;
	}
	
	public final boolean isFrameworkClass() {
		return this.isFrameworkClass;
	}

	public final void updateDerivedClasses(ClassInfo ci) {
		ArrayDeque<ClassDetail> a = new ArrayDeque<ClassDetail>();
		if (superClass != null)
			a.add(superClass.getDetails());
		for (ClassInfo i : interfaces) {
			a.add(i.getDetails());
		}
		while (!a.isEmpty()) {
			ClassDetail detail = a.pop();
			detail.derivedClasses.add(ci);
			detail.derivedClasses.addAll(derivedClasses);
			if (detail.superClass != null)
				a.add(detail.superClass.getDetails());
			for (ClassInfo i : detail.interfaces) {
				a.add(i.getDetails());
			}
		}
	}

	public final void removeDerivedClasses(ClassInfo ci) {
		ArrayDeque<ClassDetail> a = new ArrayDeque<ClassDetail>();
		if (superClass != null)
			a.add(superClass.getDetails());
		for (ClassInfo i : interfaces) {
			a.add(i.getDetails());
		}
		while (!a.isEmpty()) {
			ClassDetail detail = a.pop();
			detail.derivedClasses.remove(ci);
			detail.derivedClasses.removeAll(derivedClasses);
			if (detail.superClass != null)
				a.add(detail.superClass.getDetails());
			for (ClassInfo i : detail.interfaces) {
				a.add(i.getDetails());
			}
		}
	}

}