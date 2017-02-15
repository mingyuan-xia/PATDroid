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
 * and mutable info such as local variables, instructions
 * and analysis-specific tags
 * </p>
 * <p> Constructors have a special name "&lt;init&gt;" </p>
 * <p> The static initializer has a special name "&lt;clinit&gt;" </p>
 */
public final class MethodInfo {
    public static final String STATIC_INITIALIZER = "<clinit>";
    public static final String CONSTRUCTOR = "<init>";

    /**
     * The type containing this method.
     */
    public final ClassInfo type;
    /**
     * The signature of the method
     */
    public final MethodSignature signature;
    /**
     * The modifiers, in the format of {@link java.lang.reflect.Modifier}
     */
    public final int modifiers;
    /**
     * The return type
     * <p> if the method is a constructor or a static initializer, this will always be void </p>
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
     * Anything that should be attached to the method, no guarantee of thread-safe update of this field
     */
    public Object extra;

    /**
     * Create a method info that is part of a class
     * @param type the class
     * @param signature the method signature
     * @param returnType the return type
     * @param accessFlags the access flags
     */
    public MethodInfo(ClassInfo type, MethodSignature signature, ClassInfo returnType, int accessFlags) {
        this.type = type;
        this.signature = signature;
        this.returnType = returnType;
        this.modifiers = accessFlags;
    }

    /**
     * Construct a function prototype
     * @param signature the method signature
     * @param returnType the return type
     * @param accessFlags access flags, zero if none
     * @return the method prototype
     */
    @Deprecated
    public static MethodInfo makePrototype(MethodSignature signature, ClassInfo returnType, int accessFlags) {
        return new MethodInfo(null, signature, returnType, accessFlags);
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
        final ClassInfo baseType = this.type.getBaseType();
        MethodInfo matching;
        if (baseType != null) {
            matching = baseType.findMethod(signature);
            if (matching != null) return matching;
        }
        for (ClassInfo intf: this.type.getInterfaces()) {
            matching = intf.findMethod(signature);
            if (matching != null) return matching;
        }
        return null;
    }

    @Override
    public String toString() {
        String s = (type == null ? "" : type.toString());
        return s + "/" + signature;
    }

    /**
     * Check if this method can override another
     * (same signature and classes are inherited)
     * @param m another method
     * @return true if this method can override the other one
     */
    public boolean canOverride(MethodInfo m) {
        return this == m ||
            (this.type.isConvertibleTo(m.type) && this.signature.equals(m.signature));
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
        return signature.name.equals(CONSTRUCTOR);
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
