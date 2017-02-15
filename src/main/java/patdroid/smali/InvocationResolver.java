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

package patdroid.smali;

import com.google.common.collect.ImmutableList;
import org.jf.dexlib2.iface.reference.MethodReference;
import patdroid.core.ClassInfo;
import patdroid.core.MethodInfo;
import patdroid.core.MethodSignature;
import patdroid.core.Scope;
import patdroid.dalvik.Dalvik;
import patdroid.dalvik.Instruction;
import patdroid.util.Log;
import patdroid.util.Pair;

import java.lang.reflect.Modifier;
import java.util.ArrayList;

class InvocationResolver {
    private final ArrayList<Pair<MethodInfo, Integer>> a = new ArrayList<Pair<MethodInfo, Integer>>();
    private final Scope scope;

    InvocationResolver(Scope scope) {
        this.scope = scope;
    }

    private MethodInfo translateMethodReference(MethodReference method, boolean isStatic) {
        int accessFlags = isStatic ? Modifier.STATIC : 0;
        ClassInfo ci = Dalvik.findOrCreateClass(scope, method.getDefiningClass());
        ClassInfo retType = Dalvik.findOrCreateClass(scope, method.getReturnType());
        ImmutableList<ClassInfo> paramTypes = SmaliClassDetailLoader.findOrCreateClasses(scope, method.getParameterTypes());
        return new MethodInfo(ci, new MethodSignature(method.getName(), paramTypes), retType, accessFlags);
    }

    /**
     * Register an invocation instruction to be resolved
     * @param mi the method
     * @param pos the position of the invocation instruction in the instruction stream of the method
     */
    void registerForResolve(MethodInfo mi, int pos) {
        a.add(new Pair<MethodInfo, Integer>(mi, pos));
    }

    /**
     * resolve all invocation instructions
     */
    void resolveAll() {
        for (Pair<MethodInfo, Integer> p : a) {
            final int insn_idx = p.second;
            final Instruction i = p.first.insns[insn_idx];
            final Object[] params = (Object[]) i.extra;
            final MethodReference mr = (MethodReference) params[0];
            final boolean isStatic = i.opcode_aux == Instruction.OP_INVOKE_STATIC;
            final int[] args = (int[]) params[1];
            final MethodInfo realMethod = translateMethodReference(mr, isStatic);
            if (realMethod == null) {
                Log.debug("Cannot resolve method invocation, replace with HALT: " + mr);
                i.opcode = Instruction.OP_HALT;
            } else {
                params[0] = realMethod;
            }
        }
        a.clear();
    }
}
