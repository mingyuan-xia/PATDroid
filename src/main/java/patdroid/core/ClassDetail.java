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
*/

package patdroid.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import patdroid.util.Log;

/**
 * The details of a class, including its methods, fields, inheritance relationship.
 * These details are only available when the class is loaded. 
 * The details of a class are only supposed to be filled by class loader.
 */
public final class ClassDetail {
	/**
	 * This wrapper relies on the signature hash of MethodInfo
	 */
	private final class MethodInfoWrapper {
		private final MethodInfo m;
		private final int signatureHash;
		public MethodInfoWrapper(MethodInfo m) {
			this.m = m;
			this.signatureHash = m.computeSignatureHash();
		}
		@Override
		public int hashCode() { return signatureHash; }
		@Override
		public boolean equals(Object o) { return this.signatureHash == (((MethodInfoWrapper)o).signatureHash); }
		@Override
		public String toString() { return m.toString(); }
	}
	
	final static ClassDetail missingDetail
		= new ClassDetail(null, new ClassInfo[0], 0, new MethodInfo[0],
			new HashMap<String, ClassInfo>(), new HashMap<String, ClassInfo>(), true);
	private final ClassInfo superClass;
	private final ClassInfo[] interfaces;
	private final HashMap<MethodInfoWrapper, MethodInfo> methods;
	private final HashMap<String, ClassInfo> fields;
	private final HashMap<String, ClassInfo> staticFields;
	private final boolean isFrameworkClass;
	public ArrayList<ClassInfo> derivedClasses = new ArrayList<ClassInfo>();
	public final int accessFlags;
	/**
	 * Create a details class for a class
	 * @param superClass its base class
	 * @param interfaces interfaces
	 * @param accessFlags 
	 * @param methods methods, stored in a name-type map
	 * @param fields non-static fields, stored in a name-type map
	 * @param staticFields static fields, stored in a name-type map 
	 * @param isFrameworkClass whether it is a framework class
	 */
	public ClassDetail(ClassInfo superClass, ClassInfo[] interfaces,
					   int accessFlags, MethodInfo[] methods,
					   HashMap<String, ClassInfo> fields,
					   HashMap<String, ClassInfo> staticFields, boolean isFrameworkClass) {
		this.accessFlags = accessFlags;
		this.superClass = superClass;
		this.interfaces = interfaces;
		this.methods = new HashMap<MethodInfoWrapper, MethodInfo>();
		for (MethodInfo mi : methods) {
			this.methods.put(new MethodInfoWrapper(mi), mi);
		}
		this.fields = fields;
		this.staticFields = new HashMap<String, ClassInfo>(staticFields);
		this.isFrameworkClass = isFrameworkClass;
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
	 * Find a method declared in this class
	 * @param mproto the method prototype
	 * @return the method in the class, or null if not found
	 */
	public MethodInfo findMethodHere(MethodInfo mproto) {
		return methods.get(new MethodInfoWrapper(mproto));
	}
	
	/**
	 * Find a concrete method given a method prototype
	 * 
	 * @param m The method
	 * @return The rebound method
	 */
	public MethodInfo findMethod(MethodInfo mproto) {
		Deque<ClassDetail> q = new ArrayDeque<ClassDetail>();
		q.push(this);
		while (!q.isEmpty()) {
			ClassDetail detail = q.pop();
			MethodInfo mi = detail.findMethodHere(mproto);
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
			if (m.name.equals(name)) {
				result.add(m);
			}
		}
		return result.toArray(new MethodInfo[result.size()]);
	}
	
	/**
	 * TypeA is convertible to TypeB if and only if TypeB is a (indirect) 
	 * superclass or an (indirect) interface of TypeA.
	 *  
	 * @param type
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
	
	public final boolean isFrameworkClass() {
		return this.isFrameworkClass;
	}

	public final void updateDerivedClasses(ClassInfo ci) {
		// TODO derivedClasses only covers loaded classes
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

}