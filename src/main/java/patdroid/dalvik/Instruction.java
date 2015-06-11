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

import java.util.Arrays;
import java.util.Map;

import patdroid.core.ClassInfo;
import patdroid.core.FieldInfo;
import patdroid.util.Pair;

/**
 * Unified Dalvik VM instruction
 * <p> With C/C++, union can help save space. This only is designed to be compact. </p>
 * <p> Plain-old opcode table is preferred than inheritance as compiler could
 * generate more efficient code with this </p>
 */
public final class Instruction {
	// major opcodes
	public final static byte OP_NOP = 0;
	public final static byte OP_MOV = 0x01;
	public final static byte OP_RETURN = 0x02;
	public final static byte OP_SPECIAL = 0x03;
	public final static byte OP_NEW = 0x04;
	public final static byte OP_EXCEPTION_OP = 0x05;
	public final static byte OP_GOTO = 0x06;
	public final static byte OP_CMP = 0x07;
	public final static byte OP_IF = 0x08;
	public final static byte OP_INSTANCE_OP = 0x09;
	public final static byte OP_ARRAY_OP = 0x0A;
	public final static byte OP_STATIC_OP = 0x0B;
	public final static byte OP_INVOKE_OP = 0x0C;
	public final static byte OP_ARITHETIC = 0x0D;
	public final static byte OP_SWITCH = 0x0E;
	public static final byte OP_HALT = 0x0F;
	
	// auxiliary opcodes
	public final static byte OP_MOV_REG = 0x01;
	public final static byte OP_MOV_CONST = 0x02;
	public final static byte OP_RETURN_VOID = 0x03;
	public final static byte OP_RETURN_SOMETHING = 0x04;
	public final static byte OP_MONITOR_ENTER = 0x05;
	public final static byte OP_MONITOR_EXIT = 0x06;
	public final static byte OP_SP_ARGUMENTS = 0x07;
	public final static byte OP_NEW_INSTANCE = 0x08;
	public final static byte OP_NEW_ARRAY = 0x09;
	public final static byte OP_NEW_FILLED_ARRAY = 0x0A;
	public final static byte OP_INVOKE_DIRECT = 0x0B;
	public final static byte OP_INVOKE_SUPER = 0x0C;
	public final static byte OP_INVOKE_VIRTUAL = 0x0D;
	public final static byte OP_INVOKE_STATIC = 0x0E;
	public final static byte OP_INVOKE_INTERFACE = 0x0F;
	public final static byte OP_A_INSTANCEOF = 0x10;
	public final static byte OP_A_ARRAY_LENGTH = 0x11;
	public final static byte OP_A_CHECKCAST = 0x12;
	public final static byte OP_A_NOT = 0x13;
	public final static byte OP_A_NEG = 0x14;
	public final static byte OP_MOV_RESULT = 0x15;
	public final static byte OP_MOV_EXCEPTION = 0x16;
	public final static byte OP_A_CAST = 0x17;
	public final static byte OP_IF_EQ = 0x18;
	public final static byte OP_IF_NE = 0x19;
	public final static byte OP_IF_LT = 0x1A;
	public final static byte OP_IF_GE = 0x1B;
	public final static byte OP_IF_GT = 0x1C;
	public final static byte OP_IF_LE = 0x1D;
	public final static byte OP_IF_EQZ = 0x1E;
	public final static byte OP_IF_NEZ = 0x1F;
	public final static byte OP_IF_LTZ = 0x20;
	public final static byte OP_IF_GEZ = 0x21;
	public final static byte OP_IF_GTZ = 0x22;
	public final static byte OP_IF_LEZ = 0x23;
	public final static byte OP_ARRAY_GET = 0x24;
	public final static byte OP_ARRAY_PUT = 0x25;
	public static final byte OP_A_ADD = 0x26;
	public static final byte OP_A_SUB = 0x27;
	public static final byte OP_A_MUL = 0x28;
	public static final byte OP_A_DIV = 0x29;
	public static final byte OP_A_REM = 0x2A;
	public static final byte OP_A_AND = 0x2B;
	public static final byte OP_A_OR = 0x2C;
	public static final byte OP_A_XOR = 0x2D;
	public static final byte OP_A_SHL = 0x2E;
	public static final byte OP_A_SHR = 0x2F;
	public static final byte OP_A_USHR = 0x30;
	public static final byte OP_CMP_LONG = 0x31;
	public static final byte OP_CMP_LESS = 0x32;
	public static final byte OP_CMP_GREATER = 0x33;
	public static final byte OP_STATIC_GET_FIELD = 0x34;
	public static final byte OP_STATIC_PUT_FIELD = 0x35;
	public static final byte OP_INSTANCE_GET_FIELD = 0x36;
	public static final byte OP_INSTANCE_PUT_FIELD = 0x37;
	public static final byte OP_EXCEPTION_TRYCATCH = 0x38;
	public static final byte OP_EXCEPTION_THROW = 0x39;

	private static String[] opname = { "NOP", "MOV", "RETURN", "SPECIAL",
			"NEW", "EXCEPTION", "GOTO", "CMP", "IF", "INSTANCE", "ARRAY", "STATIC",
			"INVOKE", "ARITHMETIC", "SWITCH", "HALT" };
	private static String[] opaux_name = { "NIL", "REG", "CONST", "VOID",
			"VALUE", "MONITOR_ENTER", "MONITOR_EXIT", "ARGUMENT_SET",
			"INSTANCE", "ARRAY", "FILLED_ARRAY", "DIRECT", "SUPER", "VIRTUAL",
			"STATIC", "INTERFACE", "INSTANCE_OF", "ARRAY_LENGTH",
			"CHECK-AND-CAST", "NOT", "NEG", "RESULT", "EXCETPION", "CAST",
			"EQ", "NE", "LT", "GE", "GT", "LE", "EQZ", "NEZ", "LTZ", "GEZ",
			"GTZ", "LEZ", "AGET", "APUT", "ADD", "SUB", "MUL", "DIV", "REM",
			"AND", "OR", "XOR", "SHL", "SHR", "USHR", "CMPLONG", "CMPL", "CMPG",
			"SGET", "SPUT", "IGET", "IPUT", "TRYCATCH", "THROW" };

	/**
	 * Major opcode
	 */
	public byte opcode = OP_NOP;
	/**
	 * Auxiliary opcode
	 */
	public byte opcode_aux = OP_NOP;
	/**
	 * Register operands
	 */
	public short rdst = -1, r0 = -1, r1 = -1;
	/**
	 * Type field
	 */
	public ClassInfo type = null;
	/**
	 * Instruction-specific data
	 */
	public Object extra = null;

	@Override
	public String toString() {
		String s = "<";
		s += opname[opcode];
		if (opcode_aux != OP_NOP) {
			s += "," + opaux_name[opcode_aux];
		}
		if (rdst != -1) {
			s += ",dst=r" + rdst;
		}
		if (r0 != -1) {
			s += ",r0=r" + r0;
		}
		if (r1 != -1) {
			s += ",r1=r" + r1;
		}
		if (type != null) {
			s += ",type=" + type.toString();
		}
		if (extra != null) {
			s += ",extra=" + extraToString();
		}
		return s += ">";
	}

	private String extraToString() {
		if (extra instanceof int[]) {
			return Arrays.toString((int[]) extra);
		} else if (extra instanceof Integer) {
			return "index:" + extra.toString();
		} else if (extra instanceof String) {
			return "\""+(String) extra +"\"";
		} else if (extra instanceof Pair<?,?>) {
			return extra.toString();
		} else if (extra instanceof Map<?,?>) {
			return extra.toString();
		} else if (extra instanceof Object[]) {
			return Arrays.deepToString((Object[]) extra);
		} else if (extra instanceof FieldInfo) {
			return extra.toString();
		} else {
			return "?" + extra.toString();
		}
	}
}
