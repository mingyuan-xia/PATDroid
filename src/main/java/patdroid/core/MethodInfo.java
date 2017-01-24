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

import patdroid.dalvik.Instruction;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * The method representation.
 * <p>
 * This class contains an immutable function signature
 * and changeable info such as local variables, instructions
 * and analysis-specific tags
 * </p>
 * <p> Constructors have a special name "&lt;init&gt;" </p>
 * <p> The static initializer has a special name "&lt;clinit&gt;" </p>
 */
public final class MethodInfo {
	public static final String STATIC_INITIALIZER = "<clinit>";
	public static final String CONSTRUCTOR = "<init>";

	/**
	 * The class containing this method. For a method signature, this is null.
	 */
	public final ClassInfo myClass;
	/** 
	 * The name of the method
	 */
	public final String name;
	/**
	 * The modifiers, in the format of {@link java.lang.reflect.Modifier}
	 */
	public final int modifiers;
	/**
	 * The parameter types
	 */
	public final ClassInfo[] paramTypes;
	/**
	 * The return type
	 * <p> if the method is a constructor or a static initializer, this would be void </p>
	 */
	public final ClassInfo returnType;
	
	/**
	 * Instruction streamline
	 */
	public Instruction[] insns;
	/**
	 * Try blocks
	 */
	public TryBlockInfo[] tbs;
	/**
	 * Anything that should be attached to the method
	 */
	public Object extra;

	/**
	 * Create a method info that is part of a class
	 * @param myClass the class
	 * @param name the name 
	 * @param returnType the return type
	 * @param paramTypes the parameter types
	 * @param accessFlags the access flags
	 */
	public MethodInfo(ClassInfo myClass, String name, ClassInfo returnType,
			ClassInfo[] paramTypes, int accessFlags) {
		this.myClass = myClass;
		this.name = name;
		this.returnType = returnType;
		this.paramTypes = paramTypes;
		this.modifiers = accessFlags;
	}
	
	/**
	 * Construct a function prototype
	 * @param name the method name
	 * @param returnType the return type
	 * @param paramTypes the parameter types
	 * @param accessFlags access flags, zero if none 
	 * @return the method prototype
	 */
	public static MethodInfo makePrototype(String name, ClassInfo returnType,
			ClassInfo[] paramTypes, int accessFlags) {
		return new MethodInfo(null, name, returnType, paramTypes, accessFlags);
	}

	/**
	 *
	 * @return the method prototype of the method
	 */
	public MethodInfo getPrototype() {
		return MethodInfo.makePrototype(this.name, this.returnType, this.paramTypes, this.modifiers);
	}

	/**
	 * Compute the hash of the function signature. So if two functions have same
	 * signature, their signature hashes should be equal.
	 * <p> A signature contains the name and parameter types (no access flags and return type) </p>
	 * @return the signature hash
	 */
	public int computeSignatureHash() {
		final int prime = 31;
		int result = 1;
		result = result * prime + name.hashCode();
		for (ClassInfo t : paramTypes) {
			result = result * prime + System.identityHashCode(t);
		}
		return result;
	}

	/**
	 * Test if two functions have the same prototype
	 * @param m the prototype
	 * @return true if two functions have the same prototype
	 */
	public boolean hasSameSignature(MethodInfo m) {
		if (!m.name.equals(name) ||
				m.paramTypes.length != paramTypes.length)
			return false;
		for (int i = 0; i < this.paramTypes.length; ++i) {
			if (this.paramTypes[i] != m.paramTypes[i])
				return false;
		}
		return true;
	}

	/**
	 * Get the method in the superclass/interfaces that is overridden by the current method.
	 * If the current method is non-virtual, the result will be null.
	 * If the current method is not overriding any method from its parent classes, the result will
	 * be null too.
	 * @return the overriding method or null if none
	 */
	public MethodInfo getOverridingMethod() {
		if (this.isConstructor() || this.isStatic()) return null;
		final MethodInfo mproto = this.getPrototype();
		final ClassInfo superClass = this.myClass.getSuperClass();
		MethodInfo matching;
		if (superClass != null) {
			matching = superClass.findMethod(mproto);
			if (matching != null) return matching;
		}
		for (ClassInfo intf: this.myClass.getInterfaces()) {
			matching = intf.findMethod(mproto);
			if (matching != null) return matching;
		}
		return null;
	}

	@Override
	public String toString() {
		String s = (myClass == null ? "" : myClass.toString());
		return s + "/" + this.name
				+ Arrays.deepToString(paramTypes);
	}

	/**
	 * Check if this method can override another
     * (same signature and classes are inherited)
	 * @param m another method
	 * @return true if this method can override the other one
	 */
	public boolean canOverride(MethodInfo m) {
		return this == m ||
			(this.myClass.isConvertibleTo(m.myClass) && hasSameSignature(m));
	}

	/**
	 * @return true if the method is static
	 */
	public boolean isStatic() {
		return Modifier.isStatic(modifiers);
	}
	
	/**
	 * @return true if the method is native
	 */
	public boolean isNative() {
		return Modifier.isNative(modifiers);
	}

	/**
	 * @return true if the method is a constructor
	 */
	public boolean isConstructor() {
		return name.equals(CONSTRUCTOR);
	}

	/**
	 * @return true if the method is abstract
	 */
	public boolean isAbstract() {
		return Modifier.isAbstract(modifiers);
	}

	/**
	 * @return true if the method is final
	 */
	public boolean isFinal() {
		return Modifier.isFinal(modifiers);
	}
}
