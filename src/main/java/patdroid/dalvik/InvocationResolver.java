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

package patdroid.dalvik;

import patdroid.core.MethodInfo;
import patdroid.util.Log;
import patdroid.util.Pair;

import java.util.ArrayList;

public class InvocationResolver {
    public final ArrayList<Pair<MethodInfo, Integer>> a = new ArrayList<Pair<MethodInfo, Integer>>();

    /**
     * Register an invocation instruction to be resolved
     * @param mi the method
     * @param pos the position of the invocation instruction in the instruction stream of the method
     */
    public void registerForResolve(MethodInfo mi, int pos) {
        a.add(new Pair<MethodInfo, Integer>(mi, pos));
    }

    /**
     * resolve all invocation instructions
     */
    public void resolveAll() {
        for (Pair<MethodInfo, Integer> p : a) {
            final int insn_idx = p.second.intValue();
            final Instruction i = p.first.insns[insn_idx];
            final Object[] params = (Object[]) i.extra;
            final MethodInfo mproto = (MethodInfo) params[0];
            params[0] = mproto.myClass.findMethod(mproto);
            if (params[0] == null) {
                Log.debug("Cannot resolve method invocation, replace with HALT:" + mproto);
                i.opcode = Instruction.OP_HALT;
            }
        }
    }
}
