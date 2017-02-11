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
*   Lu Gong
*   Mingyuan Xia
*/

// tedious work has been accomplished here by Lu Gong

package patdroid.smali;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;

import patdroid.core.*;
import patdroid.dalvik.Dalvik;
import patdroid.dalvik.Instruction;
import patdroid.dalvik.InvocationResolver;
import patdroid.util.Log;
import patdroid.util.Pair;

/**
 * For every method implementation that needs to be translated, translate()
 * is called and every instruction is dispatched to the translator function
 * of its major opcode with a concrete type of the instruction as the
 * parameter (from org.jf.dexlib2.iface.instruction.formats namespace).
 *
 * The implementation of the translator function can be arbitrary, one can
 * either do all in a single function, or classify through concrete types,
 * or classify through abstract types (from iface.instruction namespace),
 * as long as all minor opcodes are properly translated.
 *
 * http://s.android.com/tech/dalvik/dalvik-bytecode.html provides an
 * official reference to the dalvik bytecode, also see dex2jar's reader
 * implementation at com.googlecode.dex2jar.reader.DexOpcodeAdapter.
 *
 * Some of the unused ops are actually used as ODEX opcode, need to be
 * implemented in the future.
 */
@SuppressWarnings("incomplete-switch")
public final class MethodImplementationTranslator {
	private final Scope scope;
	private final InvocationResolver resolver;
	private MethodInfo mi;
	private int currentCodeAddress;
	private int currentCodeIndex;
	private final HashMap<Integer, Integer> addressToIndex =
			new HashMap<Integer, Integer>();
	private final HashMap<Integer, ArrayList<Instruction>> unresolvedInsns =
			new HashMap<Integer, ArrayList<Instruction>>();
	private final HashMap<Integer, ArrayList<Instruction>> payloadDefers =
			new HashMap<Integer, ArrayList<Instruction>>();
	private final HashMap<Integer, PayloadInstruction> payloadCache =
			new HashMap<Integer, PayloadInstruction>();
	
	MethodImplementationTranslator(Scope scope, InvocationResolver resolver) {
		this.scope = scope;
	    this.resolver = resolver;
	}
	
	private static Instruction translateReturn(final Instruction10x i0) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_RETURN;
		i.opcode_aux = Instruction.OP_RETURN_VOID;
		return i;
	}
	
	private Instruction translateReturn(final Instruction11x i1) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_RETURN;
		i.opcode_aux = Instruction.OP_RETURN_SOMETHING;
		i.r0 = (short) i1.getRegisterA();
		switch (i1.getOpcode()) {
		case RETURN:
			i.type = scope.primitiveVoid;
			break;
		case RETURN_WIDE:
			i.type = scope.primitiveWide;
			break;
		case RETURN_OBJECT:
			i.type = scope.rootObject;
			break;
		}
		return i;
	}
	
	private Instruction translateMove(final OneRegisterInstruction i1) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_MOV;
		i.rdst = (short) i1.getRegisterA();
		switch (i1.getOpcode()) {
		case MOVE_RESULT:
			i.opcode_aux = Instruction.OP_MOV_RESULT;
			i.type = scope.primitiveVoid;
			break;
		case MOVE_RESULT_WIDE:
			i.opcode_aux = Instruction.OP_MOV_RESULT;
			i.type = scope.primitiveWide;
			break;
		case MOVE_RESULT_OBJECT:
			i.opcode_aux = Instruction.OP_MOV_RESULT;
			i.type = scope.rootObject;
			break;
		case MOVE_EXCEPTION:
			i.opcode_aux = Instruction.OP_MOV_EXCEPTION;
			i.type = scope.rootObject;
			break;
		}
		return i;
	}

	private Instruction translateMove(final TwoRegisterInstruction i2) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_MOV;
		i.opcode_aux = Instruction.OP_MOV_REG;
		i.rdst = (short) i2.getRegisterA();
		i.r0 = (short) i2.getRegisterB();
		switch (i2.getOpcode()) {
		case MOVE:
		case MOVE_FROM16:
		case MOVE_16:
			i.type = scope.primitiveVoid;
			break;
		case MOVE_WIDE:
		case MOVE_WIDE_FROM16:
		case MOVE_WIDE_16:
			i.type = scope.primitiveWide;
			break;
		case MOVE_OBJECT:
		case MOVE_OBJECT_FROM16:
		case MOVE_OBJECT_16:
			i.type = scope.rootObject;
			break;
		}
		return i;
	}
	
	private Instruction translateConst(final OneRegisterInstruction i1) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_MOV;
		i.opcode_aux = Instruction.OP_MOV_CONST;
		i.rdst = (short) i1.getRegisterA();
		switch (i1.getOpcode()) {
		case CONST_4:
		case CONST_16:
		case CONST:
		case CONST_HIGH16:
			i.type = scope.primitiveVoid;
			i.extra = PrimitiveInfo.fromInt(scope, ((NarrowLiteralInstruction) i1).getNarrowLiteral());
			break;
		case CONST_WIDE_16:
		case CONST_WIDE_32:
		case CONST_WIDE:
		case CONST_WIDE_HIGH16:
			i.type = scope.primitiveWide;
			i.extra = PrimitiveInfo.fromLong(scope, ((WideLiteralInstruction) i1).getWideLiteral());
			break;
		case CONST_STRING:
		case CONST_STRING_JUMBO:
			i.type = scope.findOrCreateClass(String.class);
			i.extra = ((StringReference) ((ReferenceInstruction) i1).getReference()).getString();
			break;
		case CONST_CLASS:
			i.type = scope.findOrCreateClass(Class.class);
			i.extra = Dalvik.findOrCreateClass(
					scope, ((TypeReference) ((ReferenceInstruction) i1).getReference()).getType());
			break;
		}
		return i;
	}
	
	private static Instruction translateSpecial(final Instruction11x i1) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_SPECIAL;
		i.rdst = (short) i1.getRegisterA();
		switch (i1.getOpcode()) {
		case MONITOR_ENTER:
			i.opcode_aux = Instruction.OP_MONITOR_ENTER;
			break;
		case MONITOR_EXIT:
			i.opcode_aux = Instruction.OP_MONITOR_EXIT;
			break;
		}
		return i;
	}
	
	private Instruction translateArithmetic(final OneRegisterInstruction i1) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_ARITHETIC;
		i.rdst = (short) i1.getRegisterA();
		switch (i1.getOpcode()) {
		case CHECK_CAST:
			i.opcode_aux = Instruction.OP_A_CHECKCAST;
			i.type = Dalvik.findOrCreateClass(
					scope, ((TypeReference) (((ReferenceInstruction) i1).getReference())).getType());
			break;
		}
		return i;
	}

	private Instruction translateArithmetic(final TwoRegisterInstruction i2) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_ARITHETIC;
		i.rdst = (short) i2.getRegisterA();
		i.r0 = (short) i2.getRegisterB();
		switch (i2.getOpcode()) {
		case INSTANCE_OF:
			i.opcode_aux = Instruction.OP_A_INSTANCEOF;
			i.type = Dalvik.findOrCreateClass(
					scope, ((TypeReference) (((ReferenceInstruction) i2).getReference())).getType());
			break;
		case ARRAY_LENGTH:
			i.opcode_aux = Instruction.OP_A_ARRAY_LENGTH;
			i.type = scope.primitiveInt;
			break;
		case NEG_INT:
			i.opcode_aux = Instruction.OP_A_NEG;
			i.type = scope.primitiveInt;
			break;
		case NOT_INT:
			i.opcode_aux = Instruction.OP_A_NOT;
			i.type = scope.primitiveInt;
			break;
		case NEG_LONG:
			i.opcode_aux = Instruction.OP_A_NEG;
			i.type = scope.primitiveLong;
			break;
		case NOT_LONG:
			i.opcode_aux = Instruction.OP_A_NOT;
			i.type = scope.primitiveLong;
			break;
		case NEG_FLOAT:
			i.opcode_aux = Instruction.OP_A_NEG;
			i.type = scope.primitiveFloat;
			break;
		case NEG_DOUBLE:
			i.opcode_aux = Instruction.OP_A_NEG;
			i.type = scope.primitiveDouble;
			break;
		case INT_TO_LONG:
			i.opcode_aux = Instruction.OP_A_CAST;
			i.type = scope.primitiveLong;
			i.extra = scope.primitiveInt;
			break;
		case INT_TO_FLOAT:
			i.opcode_aux = Instruction.OP_A_CAST;
			i.type = scope.primitiveFloat;
			i.extra = scope.primitiveInt;
			break;
		case INT_TO_DOUBLE:
			i.opcode_aux = Instruction.OP_A_CAST;
			i.type = scope.primitiveDouble;
			i.extra = scope.primitiveInt;
			break;
		case LONG_TO_INT:
			i.opcode_aux = Instruction.OP_A_CAST;
			i.type = scope.primitiveInt;
			i.extra = scope.primitiveLong;
			break;
		case LONG_TO_FLOAT:
			i.opcode_aux = Instruction.OP_A_CAST;
			i.type = scope.primitiveFloat;
			i.extra = scope.primitiveLong;
			break;
		case LONG_TO_DOUBLE:
			i.opcode_aux = Instruction.OP_A_CAST;
			i.type = scope.primitiveDouble;
			i.extra = scope.primitiveLong;
			break;
		case FLOAT_TO_INT:
			i.opcode_aux = Instruction.OP_A_CAST;
			i.type = scope.primitiveInt;
			i.extra = scope.primitiveFloat;
			break;
		case FLOAT_TO_LONG:
			i.opcode_aux = Instruction.OP_A_CAST;
			i.type = scope.primitiveLong;
			i.extra = scope.primitiveFloat;
			break;
		case FLOAT_TO_DOUBLE:
			i.opcode_aux = Instruction.OP_A_CAST;
			i.type = scope.primitiveDouble;
			i.extra = scope.primitiveFloat;
			break;
		case DOUBLE_TO_INT:
			i.opcode_aux = Instruction.OP_A_CAST;
			i.type = scope.primitiveInt;
			i.extra = scope.primitiveDouble;
			break;
		case DOUBLE_TO_LONG:
			i.opcode_aux = Instruction.OP_A_CAST;
			i.type = scope.primitiveLong;
			i.extra = scope.primitiveDouble;
			break;
		case DOUBLE_TO_FLOAT:
			i.opcode_aux = Instruction.OP_A_CAST;
			i.type = scope.primitiveFloat;
			i.extra = scope.primitiveDouble;
			break;
		case INT_TO_BYTE:
			i.opcode_aux = Instruction.OP_A_CAST;
			i.type = scope.primitiveByte;
			i.extra = scope.primitiveInt;
			break;
		case INT_TO_CHAR:
			i.opcode_aux = Instruction.OP_A_CAST;
			i.type = scope.primitiveChar;
			i.extra = scope.primitiveInt;
			break;
		case INT_TO_SHORT:
			i.opcode_aux = Instruction.OP_A_CAST;
			i.type = scope.primitiveShort;
			i.extra = scope.primitiveInt;
			break;
		}
		return i;
	}
	
	private Instruction translateArithmetic(final ThreeRegisterInstruction i3) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_ARITHETIC;
		i.rdst = (short) i3.getRegisterA();
		i.r0 = (short) i3.getRegisterB();
		i.r1 = (short) i3.getRegisterC();
		switch (i3.getOpcode()) {
		case ADD_INT:
			i.opcode_aux = Instruction.OP_A_ADD;
			i.type = scope.primitiveInt;
			break;
		case SUB_INT:
			i.opcode_aux = Instruction.OP_A_SUB;
			i.type = scope.primitiveInt;
			break;
		case MUL_INT:
			i.opcode_aux = Instruction.OP_A_MUL;
			i.type = scope.primitiveInt;
			break;
		case DIV_INT:
			i.opcode_aux = Instruction.OP_A_DIV;
			i.type = scope.primitiveInt;
			break;
		case REM_INT:
			i.opcode_aux = Instruction.OP_A_REM;
			i.type = scope.primitiveInt;
			break;
		case AND_INT:
			i.opcode_aux = Instruction.OP_A_AND;
			i.type = scope.primitiveInt;
			break;
		case OR_INT:
			i.opcode_aux = Instruction.OP_A_OR;
			i.type = scope.primitiveInt;
			break;
		case XOR_INT:
			i.opcode_aux = Instruction.OP_A_XOR;
			i.type = scope.primitiveInt;
			break;
		case SHL_INT:			
			i.opcode_aux = Instruction.OP_A_SHL;
			i.type = scope.primitiveInt;
			break;
		case SHR_INT:
			i.opcode_aux = Instruction.OP_A_SHR;
			i.type = scope.primitiveInt;
			break;
		case USHR_INT:
			i.opcode_aux = Instruction.OP_A_USHR;
			i.type = scope.primitiveInt;
			break;
		case ADD_LONG:
			i.opcode_aux = Instruction.OP_A_ADD;
			i.type = scope.primitiveLong;
			break;
		case SUB_LONG:
			i.opcode_aux = Instruction.OP_A_SUB;
			i.type = scope.primitiveLong;
			break;
		case MUL_LONG:
			i.opcode_aux = Instruction.OP_A_MUL;
			i.type = scope.primitiveLong;
			break;
		case DIV_LONG:
			i.opcode_aux = Instruction.OP_A_DIV;
			i.type = scope.primitiveLong;
			break;
		case REM_LONG:
			i.opcode_aux = Instruction.OP_A_REM;
			i.type = scope.primitiveLong;
			break;
		case AND_LONG:
			i.opcode_aux = Instruction.OP_A_AND;
			i.type = scope.primitiveLong;
			break;
		case OR_LONG:
			i.opcode_aux = Instruction.OP_A_OR;
			i.type = scope.primitiveLong;
			break;
		case XOR_LONG:
			i.opcode_aux = Instruction.OP_A_XOR;
			i.type = scope.primitiveLong;
			break;
		case SHL_LONG:
			i.opcode_aux = Instruction.OP_A_SHL;
			i.type = scope.primitiveLong;
			break;
		case SHR_LONG:
			i.opcode_aux = Instruction.OP_A_SHR;
			i.type = scope.primitiveLong;
			break;
		case USHR_LONG:
			i.opcode_aux = Instruction.OP_A_USHR;
			i.type = scope.primitiveLong;
			break;
		case ADD_FLOAT:
			i.opcode_aux = Instruction.OP_A_ADD;
			i.type = scope.primitiveFloat;
			break;
		case SUB_FLOAT:
			i.opcode_aux = Instruction.OP_A_SUB;
			i.type = scope.primitiveFloat;
			break;
		case MUL_FLOAT:
			i.opcode_aux = Instruction.OP_A_MUL;
			i.type = scope.primitiveFloat;
			break;
		case DIV_FLOAT:
			i.opcode_aux = Instruction.OP_A_DIV;
			i.type = scope.primitiveFloat;
			break;
		case REM_FLOAT:
			i.opcode_aux = Instruction.OP_A_REM;
			i.type = scope.primitiveFloat;
			break;
		case ADD_DOUBLE:
			i.opcode_aux = Instruction.OP_A_ADD;
			i.type = scope.primitiveDouble;
			break;
		case SUB_DOUBLE:
			i.opcode_aux = Instruction.OP_A_SUB;
			i.type = scope.primitiveDouble;
			break;
		case MUL_DOUBLE:
			i.opcode_aux = Instruction.OP_A_MUL;
			i.type = scope.primitiveDouble;
			break;
		case DIV_DOUBLE:
			i.opcode_aux = Instruction.OP_A_DIV;
			i.type = scope.primitiveDouble;
			break;
		case REM_DOUBLE:
			i.opcode_aux = Instruction.OP_A_REM;
			i.type = scope.primitiveDouble;
			break;
		}
		return i;
	}

	private Instruction translateArithmeticTwoAddr(final TwoRegisterInstruction i2) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_ARITHETIC;
		i.rdst = i.r0 = (short) i2.getRegisterA();
		i.r1 = (short) i2.getRegisterB();
		switch (i2.getOpcode()) {
		case ADD_INT_2ADDR:
			i.opcode_aux = Instruction.OP_A_ADD;
			i.type = scope.primitiveInt;
			break;
		case SUB_INT_2ADDR:
			i.opcode_aux = Instruction.OP_A_SUB;
			i.type = scope.primitiveInt;
			break;
		case MUL_INT_2ADDR:
			i.opcode_aux = Instruction.OP_A_MUL;
			i.type = scope.primitiveInt;
			break;
		case DIV_INT_2ADDR:
			i.opcode_aux = Instruction.OP_A_DIV;
			i.type = scope.primitiveInt;
			break;
		case REM_INT_2ADDR:
			i.opcode_aux = Instruction.OP_A_REM;
			i.type = scope.primitiveInt;
			break;
		case AND_INT_2ADDR:
			i.opcode_aux = Instruction.OP_A_AND;
			i.type = scope.primitiveInt;
			break;
		case OR_INT_2ADDR:
			i.opcode_aux = Instruction.OP_A_OR;
			i.type = scope.primitiveInt;
			break;
		case XOR_INT_2ADDR:
			i.opcode_aux = Instruction.OP_A_XOR;
			i.type = scope.primitiveInt;
			break;
		case SHL_INT_2ADDR:
			i.opcode_aux = Instruction.OP_A_SHL;
			i.type = scope.primitiveInt;
			break;
		case SHR_INT_2ADDR:
			i.opcode_aux = Instruction.OP_A_SHR;
			i.type = scope.primitiveInt;
			break;
		case USHR_INT_2ADDR:
			i.opcode_aux = Instruction.OP_A_USHR;
			i.type = scope.primitiveInt;
			break;
		case ADD_LONG_2ADDR:
			i.opcode_aux = Instruction.OP_A_ADD;
			i.type = scope.primitiveLong;
			break;
		case SUB_LONG_2ADDR:
			i.opcode_aux = Instruction.OP_A_SUB;
			i.type = scope.primitiveLong;
			break;
		case MUL_LONG_2ADDR:
			i.opcode_aux = Instruction.OP_A_MUL;
			i.type = scope.primitiveLong;
			break;
		case DIV_LONG_2ADDR:
			i.opcode_aux = Instruction.OP_A_DIV;
			i.type = scope.primitiveLong;
			break;
		case REM_LONG_2ADDR:
			i.opcode_aux = Instruction.OP_A_REM;
			i.type = scope.primitiveLong;
			break;
		case AND_LONG_2ADDR:
			i.opcode_aux = Instruction.OP_A_AND;
			i.type = scope.primitiveLong;
			break;
		case OR_LONG_2ADDR:
			i.opcode_aux = Instruction.OP_A_OR;
			i.type = scope.primitiveLong;
			break;
		case XOR_LONG_2ADDR:
			i.opcode_aux = Instruction.OP_A_XOR;
			i.type = scope.primitiveLong;
			break;
		case SHL_LONG_2ADDR:
			i.opcode_aux = Instruction.OP_A_SHL;
			i.type = scope.primitiveLong;
			break;
		case SHR_LONG_2ADDR:
			i.opcode_aux = Instruction.OP_A_SHR;
			i.type = scope.primitiveLong;
			break;
		case USHR_LONG_2ADDR:
			i.opcode_aux = Instruction.OP_A_USHR;
			i.type = scope.primitiveLong;
			break;
		case ADD_FLOAT_2ADDR:
			i.opcode_aux = Instruction.OP_A_ADD;
			i.type = scope.primitiveFloat;
			break;
		case SUB_FLOAT_2ADDR:
			i.opcode_aux = Instruction.OP_A_SUB;
			i.type = scope.primitiveFloat;
			break;
		case MUL_FLOAT_2ADDR:
			i.opcode_aux = Instruction.OP_A_MUL;
			i.type = scope.primitiveFloat;
			break;
		case DIV_FLOAT_2ADDR:
			i.opcode_aux = Instruction.OP_A_DIV;
			i.type = scope.primitiveFloat;
			break;
		case REM_FLOAT_2ADDR:
			i.opcode_aux = Instruction.OP_A_REM;
			i.type = scope.primitiveFloat;
			break;
		case ADD_DOUBLE_2ADDR:
			i.opcode_aux = Instruction.OP_A_ADD;
			i.type = scope.primitiveDouble;
			break;
		case SUB_DOUBLE_2ADDR:
			i.opcode_aux = Instruction.OP_A_SUB;
			i.type = scope.primitiveDouble;
			break;
		case MUL_DOUBLE_2ADDR:
			i.opcode_aux = Instruction.OP_A_MUL;
			i.type = scope.primitiveDouble;
			break;
		case DIV_DOUBLE_2ADDR:
			i.opcode_aux = Instruction.OP_A_DIV;
			i.type = scope.primitiveDouble;
			break;
		case REM_DOUBLE_2ADDR:
			i.opcode_aux = Instruction.OP_A_REM;
			i.type = scope.primitiveDouble;
			break;
		}
		return i;
	}

	private Instruction translateArithmeticLit(final TwoRegisterInstruction i2) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_ARITHETIC;
		i.rdst = (short) i2.getRegisterA();
		i.r0 = (short) i2.getRegisterB();
		i.extra = PrimitiveInfo.fromInt(scope, ((NarrowLiteralInstruction) i2).getNarrowLiteral());
		switch (i2.getOpcode()) {
		case ADD_INT_LIT16:
		case ADD_INT_LIT8:
			i.opcode_aux = Instruction.OP_A_ADD;
			break;
		case RSUB_INT:
		case RSUB_INT_LIT8:
			// TODO This is incorrect, so as the dex2jar version
			i.opcode_aux = Instruction.OP_A_SUB;
			break;
		case MUL_INT_LIT16:
		case MUL_INT_LIT8:
			i.opcode_aux = Instruction.OP_A_MUL;
			break;
		case DIV_INT_LIT16:
		case DIV_INT_LIT8:
			i.opcode_aux = Instruction.OP_A_DIV;
			break;
		case REM_INT_LIT16:
		case REM_INT_LIT8:
			i.opcode_aux = Instruction.OP_A_REM;
			break;
		case AND_INT_LIT16:
		case AND_INT_LIT8:
			i.opcode_aux = Instruction.OP_A_AND;
			break;
		case OR_INT_LIT16:
		case OR_INT_LIT8:
			i.opcode_aux = Instruction.OP_A_OR;
			break;
		case XOR_INT_LIT16:
		case XOR_INT_LIT8:
			i.opcode_aux = Instruction.OP_A_XOR;
			break;
		case SHL_INT_LIT8:
			i.opcode_aux = Instruction.OP_A_SHL;
			break;
		case SHR_INT_LIT8:
			i.opcode_aux = Instruction.OP_A_SHR;
			break;
		case USHR_INT_LIT8:
			i.opcode_aux = Instruction.OP_A_USHR;
			break;
		}
		return i;
	}
	
	private Instruction translateNew(final Instruction21c i1) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_NEW;
		i.opcode_aux = Instruction.OP_NEW_INSTANCE;
		i.rdst = (short) i1.getRegisterA();
		i.type = Dalvik.findOrCreateClass(
				scope, ((TypeReference) i1.getReference()).getType());
		return i;
	}

	private Instruction translateNew(final Instruction22c i2) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_NEW;
		i.opcode_aux = Instruction.OP_NEW_ARRAY;
		i.rdst = (short) i2.getRegisterA();
		i.r0 = (short) i2.getRegisterB();
		i.type = Dalvik.findOrCreateClass(
				scope, ((TypeReference) i2.getReference()).getType());
		return i;
	}
	
	private static int[] getArguments(final FiveRegisterInstruction i5) {
		final int[] args = new int[i5.getRegisterCount()];
		if (args.length > 0) args[0] = i5.getRegisterC();
		if (args.length > 1) args[1] = i5.getRegisterD();
		if (args.length > 2) args[2] = i5.getRegisterE();
		if (args.length > 3) args[3] = i5.getRegisterF();
		if (args.length > 4) args[4] = i5.getRegisterG();
		return args;
	}
	
	private Instruction translateNew(final Instruction35c i5) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_NEW;
		i.opcode_aux = Instruction.OP_NEW_FILLED_ARRAY;
		i.rdst = -1;
		i.type = Dalvik.findOrCreateClass(
				scope, ((TypeReference) i5.getReference()).getType());
		i.extra = getArguments(i5);
		return i;
	}
	
	private static int[] getArguments(final RegisterRangeInstruction ir) {
		final int[] args = new int[ir.getRegisterCount()];
		final int start = ir.getStartRegister();
		for (int i = 0; i < args.length; ++i) {
			args[i] = start + i;
		}
		return args;
	}

	private Instruction translateNew(final Instruction3rc ir) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_NEW;
		i.opcode_aux = Instruction.OP_NEW_FILLED_ARRAY;
		i.rdst = -1;
		i.type = Dalvik.findOrCreateClass(
				scope, ((TypeReference) ir.getReference()).getType());
		i.extra = getArguments(ir);
		return i;
	}
	
	private Instruction translateNew(final Instruction31t i1) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_NEW;
		i.opcode_aux = Instruction.OP_NEW_FILLED_ARRAY;
		i.rdst = (short) i1.getRegisterA();
		final int payloadAddress = currentCodeAddress + i1.getCodeOffset();
		PayloadInstruction p = payloadCache.get(payloadAddress);
		if (p != null) {
			Log.doAssert(p.getOpcode() == Opcode.ARRAY_PAYLOAD, "payload type mismatch");
			applyPayload(i, (ArrayPayload)p);
		} else {
			ArrayList<Instruction> defers = payloadDefers.get(payloadAddress);
			if (defers == null) {
				defers = new ArrayList<Instruction>();
				payloadDefers.put(payloadAddress, defers);
			}
			defers.add(i);
		}
		return i;
	}
	
	private Instruction translateExceptionOp(final Instruction11x i1) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_EXCEPTION_OP;
		i.opcode_aux = Instruction.OP_EXCEPTION_THROW;
		i.r0 = (short) i1.getRegisterA();
		i.type = scope.rootObject;
		return i;
	}
	
	private Instruction translateGoto(final OffsetInstruction io) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_GOTO;
		final int destAddress = currentCodeAddress + io.getCodeOffset();
		i.extra = addressToIndex.get(destAddress);
		if (i.extra == null) {
			ArrayList<Instruction> insns = unresolvedInsns.get(destAddress);
			if (insns == null) {
				insns = new ArrayList<Instruction>();
				unresolvedInsns.put(destAddress, insns);
			}
			insns.add(i);
		}
		return i;
	}
	
	private Instruction translateSwitch(final Instruction31t i1) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_SWITCH;
		i.r0 = (short) i1.getRegisterA();
		
		/*
		 *  The translation of switch operation is composed with three stages
		 *  
		 *  In stage 1, extra contains the address of the switch instruction
		 *  In stage 2, extra contains the address and a list of SwitchElement
		 *  In stage 3, extra contains a map from conditions to indexes
		 */
		i.extra = currentCodeAddress;
		
		final int payloadAddress = currentCodeAddress + i1.getCodeOffset();
		PayloadInstruction p = payloadCache.get(payloadAddress);
		if (p != null) {
			final Opcode opcode = p.getOpcode();
			Log.doAssert(opcode == Opcode.PACKED_SWITCH_PAYLOAD ||
					opcode == Opcode.SPARSE_SWITCH_PAYLOAD, "payload type mismatch");
			applyPayload(i, (SwitchPayload)p);
		} else {
			ArrayList<Instruction> defers = payloadDefers.get(payloadAddress);
			if (defers == null) {
				defers = new ArrayList<Instruction>();
				payloadDefers.put(payloadAddress, defers);
			}
			defers.add(i);
		}
		return i;
	}
	
	private Instruction translateCmp(final Instruction23x i3) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_CMP;
		i.rdst = (short) i3.getRegisterA();
		i.r0 = (short) i3.getRegisterB();
		i.r1 = (short) i3.getRegisterC();
		switch (i3.getOpcode()) {
		case CMPL_FLOAT:
			i.opcode_aux = Instruction.OP_CMP_LESS;
			i.type = scope.primitiveFloat;
			break;
		case CMPG_FLOAT:
			i.opcode_aux = Instruction.OP_CMP_GREATER;
			i.type = scope.primitiveFloat;
			break;
		case CMPL_DOUBLE:
			i.opcode_aux = Instruction.OP_CMP_LESS;
			i.type = scope.primitiveDouble;
			break;
		case CMPG_DOUBLE:
			i.opcode_aux = Instruction.OP_CMP_GREATER;
			i.type = scope.primitiveDouble;
			break;
		case CMP_LONG:
			i.opcode_aux = Instruction.OP_CMP_LONG;
			i.type = scope.primitiveLong;
			break;
		}
		return i;
	}
	
	private Instruction translateIf(final Instruction22t i2) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_IF;
		switch (i2.getOpcode()) {
		case IF_EQ:
			i.opcode_aux = Instruction.OP_IF_EQ;
			break;
		case IF_NE:
			i.opcode_aux = Instruction.OP_IF_NE;
			break;
		case IF_LT:
			i.opcode_aux = Instruction.OP_IF_LT;
			break;
		case IF_GE:
			i.opcode_aux = Instruction.OP_IF_GE;
			break;
		case IF_GT:
			i.opcode_aux = Instruction.OP_IF_GT;
			break;
		case IF_LE:
			i.opcode_aux = Instruction.OP_IF_LE;
			break;
		}
		i.r0 = (short) i2.getRegisterA();
		i.r1 = (short) i2.getRegisterB();
		final int destAddress = currentCodeAddress + i2.getCodeOffset();
		i.extra = addressToIndex.get(destAddress);
		if (i.extra == null) {
			ArrayList<Instruction> insns = unresolvedInsns.get(destAddress);
			if (insns == null) {
				insns = new ArrayList<Instruction>();
				unresolvedInsns.put(destAddress, insns);
			}
			insns.add(i);
		}
		return i;
	}
	
	private Instruction translateIf(final Instruction21t i1) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_IF;
		switch (i1.getOpcode()) {
		case IF_EQZ:
			i.opcode_aux = Instruction.OP_IF_EQZ;
			break;
		case IF_NEZ:
			i.opcode_aux = Instruction.OP_IF_NEZ;
			break;
		case IF_LTZ:
			i.opcode_aux = Instruction.OP_IF_LTZ;
			break;
		case IF_GEZ:
			i.opcode_aux = Instruction.OP_IF_GEZ;
			break;
		case IF_GTZ:
			i.opcode_aux = Instruction.OP_IF_GTZ;
			break;
		case IF_LEZ:
			i.opcode_aux = Instruction.OP_IF_LEZ;
			break;
		}
		i.r0 = (short) i1.getRegisterA();
		final int destAddress = currentCodeAddress + i1.getCodeOffset();
		i.extra = addressToIndex.get(destAddress);
		if (i.extra == null) {
			ArrayList<Instruction> insns = unresolvedInsns.get(destAddress);
			if (insns == null) {
				insns = new ArrayList<Instruction>();
				unresolvedInsns.put(destAddress, insns);
			}
			insns.add(i);
		}
		return i;
	}
	
	private Instruction translateArrayOp(final Instruction23x i3) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_ARRAY_OP;
		i.rdst = (short) i3.getRegisterA(); // value register, may be source or dest
		i.r0 = (short) i3.getRegisterB(); // array register
		i.r1 = (short) i3.getRegisterC(); // index register
		switch (i3.getOpcode()) {
		case AGET:
			i.opcode_aux = Instruction.OP_ARRAY_GET;
			i.type = scope.primitiveVoid;
			break;
		case AGET_WIDE:
			i.opcode_aux = Instruction.OP_ARRAY_GET;
			i.type = scope.primitiveWide;
			break;
		case AGET_OBJECT:
			i.opcode_aux = Instruction.OP_ARRAY_GET;
			i.type = scope.rootObject;
			break;
		case AGET_BOOLEAN:
			i.opcode_aux = Instruction.OP_ARRAY_GET;
			i.type = scope.primitiveBoolean;
			break;
		case AGET_BYTE:
			i.opcode_aux = Instruction.OP_ARRAY_GET;
			i.type = scope.primitiveByte;
			break;
		case AGET_CHAR:
			i.opcode_aux = Instruction.OP_ARRAY_GET;
			i.type = scope.primitiveChar;
			break;
		case AGET_SHORT:
			i.opcode_aux = Instruction.OP_ARRAY_GET;
			i.type = scope.primitiveShort;
			break;
		case APUT:
			i.opcode_aux = Instruction.OP_ARRAY_PUT;
			i.type = scope.primitiveVoid;
			break;
		case APUT_WIDE:
			i.opcode_aux = Instruction.OP_ARRAY_PUT;
			i.type = scope.primitiveWide;
			break;
		case APUT_OBJECT:
			i.opcode_aux = Instruction.OP_ARRAY_PUT;
			i.type = scope.rootObject;
			break;
		case APUT_BOOLEAN:
			i.opcode_aux = Instruction.OP_ARRAY_PUT;
			i.type = scope.primitiveBoolean;
			break;
		case APUT_BYTE:
			i.opcode_aux = Instruction.OP_ARRAY_PUT;
			i.type = scope.primitiveByte;
			break;
		case APUT_CHAR:
			i.opcode_aux = Instruction.OP_ARRAY_PUT;
			i.type = scope.primitiveChar;
			break;
		case APUT_SHORT:
			i.opcode_aux = Instruction.OP_ARRAY_PUT;
			i.type = scope.primitiveShort;
			break;
		}
		return i;
	}
	
	private Instruction translateInstanceOp(final Instruction22c i2) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_INSTANCE_OP;
		i.r0 = (short) i2.getRegisterB(); // object register
		i.r1 = (short) i2.getRegisterA(); // value register, may be source or dest
		final FieldReference field = (FieldReference) i2.getReference();
		final ClassInfo owner = Dalvik.findOrCreateClass(scope, field.getDefiningClass());
		i.extra = new FieldInfo(owner, field.getName());
		// TODO The field type information is not used, which can be acquired
		// through field.getType(), so as the dex2jar version
		switch (i2.getOpcode()) {
		case IGET:
			i.opcode_aux = Instruction.OP_INSTANCE_GET_FIELD;
			i.type = scope.primitiveVoid;
			break;
		case IGET_WIDE:
			i.opcode_aux = Instruction.OP_INSTANCE_GET_FIELD;
			i.type = scope.primitiveWide;
			break;
		case IGET_OBJECT:
			i.opcode_aux = Instruction.OP_INSTANCE_GET_FIELD;
			i.type = scope.rootObject;
			break;
		case IGET_BOOLEAN:
			i.opcode_aux = Instruction.OP_INSTANCE_GET_FIELD;
			i.type = scope.primitiveBoolean;
			break;
		case IGET_BYTE:
			i.opcode_aux = Instruction.OP_INSTANCE_GET_FIELD;
			i.type = scope.primitiveByte;
			break;
		case IGET_CHAR:
			i.opcode_aux = Instruction.OP_INSTANCE_GET_FIELD;
			i.type = scope.primitiveChar;
			break;
		case IGET_SHORT:
			i.opcode_aux = Instruction.OP_INSTANCE_GET_FIELD;
			i.type = scope.primitiveShort;
			break;
		case IPUT:
			i.opcode_aux = Instruction.OP_INSTANCE_PUT_FIELD;
			i.type = scope.primitiveVoid;
			break;
		case IPUT_WIDE:
			i.opcode_aux = Instruction.OP_INSTANCE_PUT_FIELD;
			i.type = scope.primitiveWide;
			break;
		case IPUT_OBJECT:
			i.opcode_aux = Instruction.OP_INSTANCE_PUT_FIELD;
			i.type = scope.rootObject;
			break;
		case IPUT_BOOLEAN:
			i.opcode_aux = Instruction.OP_INSTANCE_PUT_FIELD;
			i.type = scope.primitiveBoolean;
			break;
		case IPUT_BYTE:
			i.opcode_aux = Instruction.OP_INSTANCE_PUT_FIELD;
			i.type = scope.primitiveByte;
			break;
		case IPUT_CHAR:
			i.opcode_aux = Instruction.OP_INSTANCE_PUT_FIELD;
			i.type = scope.primitiveChar;
			break;
		case IPUT_SHORT:
			i.opcode_aux = Instruction.OP_INSTANCE_PUT_FIELD;
			i.type = scope.primitiveShort;
			break;
		}
		return i;
	}
	
	private Instruction translateStaticOp(final Instruction21c i1) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_STATIC_OP;
		i.r0 = (short) i1.getRegisterA(); // value register, may be source or dest
		final FieldReference field = (FieldReference) i1.getReference();
		final ClassInfo owner = Dalvik.findOrCreateClass(scope, field.getDefiningClass());
		i.extra = new Pair<ClassInfo, String>(owner, field.getName());
		// TODO The field type information is not used, which can be acquired
		// through field.getType(), so as the dex2jar version
		switch (i1.getOpcode()) {
		case SGET:
			i.opcode_aux = Instruction.OP_STATIC_GET_FIELD;
			i.type = scope.primitiveVoid;
			break;
		case SGET_WIDE:
			i.opcode_aux = Instruction.OP_STATIC_GET_FIELD;
			i.type = scope.primitiveWide;
			break;
		case SGET_OBJECT:
			i.opcode_aux = Instruction.OP_STATIC_GET_FIELD;
			i.type = scope.rootObject;
			break;
		case SGET_BOOLEAN:
			i.opcode_aux = Instruction.OP_STATIC_GET_FIELD;
			i.type = scope.primitiveBoolean;
			break;
		case SGET_BYTE:
			i.opcode_aux = Instruction.OP_STATIC_GET_FIELD;
			i.type = scope.primitiveByte;
			break;
		case SGET_CHAR:
			i.opcode_aux = Instruction.OP_STATIC_GET_FIELD;
			i.type = scope.primitiveChar;
			break;
		case SGET_SHORT:
			i.opcode_aux = Instruction.OP_STATIC_GET_FIELD;
			i.type = scope.primitiveShort;
			break;
		case SPUT:
			i.opcode_aux = Instruction.OP_STATIC_PUT_FIELD;
			i.type = scope.primitiveVoid;
			break;
		case SPUT_WIDE:
			i.opcode_aux = Instruction.OP_STATIC_PUT_FIELD;
			i.type = scope.primitiveWide;
			break;
		case SPUT_OBJECT:
			i.opcode_aux = Instruction.OP_STATIC_PUT_FIELD;
			i.type = scope.rootObject;
			break;
		case SPUT_BOOLEAN:
			i.opcode_aux = Instruction.OP_STATIC_PUT_FIELD;
			i.type = scope.primitiveBoolean;
			break;
		case SPUT_BYTE:
			i.opcode_aux = Instruction.OP_STATIC_PUT_FIELD;
			i.type = scope.primitiveByte;
			break;
		case SPUT_CHAR:
			i.opcode_aux = Instruction.OP_STATIC_PUT_FIELD;
			i.type = scope.primitiveChar;
			break;
		case SPUT_SHORT:
			i.opcode_aux = Instruction.OP_STATIC_PUT_FIELD;
			i.type = scope.primitiveShort;
			break;
		}
		return i;
	}
	
	private int[] rebuildArgs(MethodInfo mi, int[] args) {
		final int realSize = mi.signature.paramTypes.size() + (mi.isStatic() ? 0 : 1);
		if (realSize == args.length)
			return args;
		final int[] realArgs = new int[realSize];
		int i = 0, j = 0;
		if (!mi.isStatic())
			realArgs[i++] = args[j++];
		for (ClassInfo ci: mi.signature.paramTypes) {
			realArgs[i++] = args[j++];
			if (ci == scope.primitiveLong || ci == scope.primitiveDouble)
				++j;
		}
		Log.doAssert(j == args.length, "argument size mismatch");
		return realArgs;
	}
	
	private Instruction translateInvoke(final Instruction35c i5) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_INVOKE_OP;
		int accessFlags = 0;
		switch (i5.getOpcode()) {
		case INVOKE_VIRTUAL:
			i.opcode_aux = Instruction.OP_INVOKE_VIRTUAL;
			break;
		case INVOKE_SUPER:
			i.opcode_aux = Instruction.OP_INVOKE_SUPER;
			break;
		case INVOKE_DIRECT:
			i.opcode_aux = Instruction.OP_INVOKE_DIRECT;
			break;
		case INVOKE_STATIC:
			i.opcode_aux = Instruction.OP_INVOKE_STATIC;
			accessFlags = Modifier.STATIC;
			break;
		case INVOKE_INTERFACE:
			i.opcode_aux = Instruction.OP_INVOKE_INTERFACE;
			break;
		}
		final MethodReference method = (MethodReference) i5.getReference();
		final MethodInfo mi = SmaliClassDetailLoader.translateMethodReference(scope, method, accessFlags);
		final int[] args = rebuildArgs(mi, getArguments(i5));
		i.extra = new Object[] {mi, args};
		resolver.registerForResolve(this.mi, currentCodeIndex);
		return i;
	}
	
	private Instruction translateInvoke(final Instruction3rc ir) {
		final Instruction i = new Instruction();
		i.opcode = Instruction.OP_INVOKE_OP;
		int accessFlags = 0;
		switch (ir.getOpcode()) {
		case INVOKE_VIRTUAL_RANGE:
			i.opcode_aux = Instruction.OP_INVOKE_VIRTUAL;
			break;
		case INVOKE_SUPER_RANGE:
			i.opcode_aux = Instruction.OP_INVOKE_SUPER;
			break;
		case INVOKE_DIRECT_RANGE:
			i.opcode_aux = Instruction.OP_INVOKE_DIRECT;
			break;
		case INVOKE_STATIC_RANGE:
			i.opcode_aux = Instruction.OP_INVOKE_STATIC;
			accessFlags = Modifier.STATIC;
			break;
		case INVOKE_INTERFACE_RANGE:
			i.opcode_aux = Instruction.OP_INVOKE_INTERFACE;
			break;
		}
		final MethodReference method = (MethodReference) ir.getReference();
		final MethodInfo mi = SmaliClassDetailLoader.translateMethodReference(scope, method, accessFlags);
		final int[] args = rebuildArgs(mi, getArguments(ir));
		i.extra = new Object[] {mi, args};
		resolver.registerForResolve(this.mi, currentCodeIndex);
		return i;
	}

	private void applyPayload(final Instruction i, final PayloadInstruction p) {
		final Opcode opcode = p.getOpcode();
		if (opcode == Opcode.ARRAY_PAYLOAD) {
			Log.doAssert(i.opcode == Instruction.OP_NEW &&
					i.opcode_aux == Instruction.OP_NEW_FILLED_ARRAY, "payload type mismatch");
			final List<Number> elements = ((ArrayPayload) p).getArrayElements();
			final PrimitiveInfo[] array = new PrimitiveInfo[elements.size()];
			for (int j = 0; j < array.length; ++j) {
				array[j] = PrimitiveInfo.fromObject(scope, elements.get(j));
			}
			i.extra = array;
		} else if (opcode == Opcode.PACKED_SWITCH_PAYLOAD ||
				opcode == Opcode.SPARSE_SWITCH_PAYLOAD) {
			Log.doAssert(i.opcode == Instruction.OP_SWITCH, "payload type mismatch");
			final int switchAddress = ((Integer) i.extra).intValue();
			final List<? extends SwitchElement> table = ((SwitchPayload) p).getSwitchElements();
			boolean resolvable = true;
			int maxAddress = -1;
			for (final SwitchElement e: table) {
				final int destAddress = switchAddress + e.getOffset();
				if (!addressToIndex.containsKey(destAddress)) {
					if (resolvable) resolvable = false;
					if (destAddress > maxAddress) maxAddress = destAddress;
				}
			}
			if (resolvable) {
				i.extra = resolveSwitchTable(switchAddress, table);
			} else {
				i.extra = new Pair<Integer, List<? extends SwitchElement>>(
						switchAddress, table);
				ArrayList<Instruction> insns = unresolvedInsns.get(maxAddress);
				if (insns == null) {
					insns = new ArrayList<Instruction>();
					unresolvedInsns.put(maxAddress, insns);
				}
				insns.add(i);
			}
		}
	}
	
	private Map<Integer, Integer> resolveSwitchTable(final int switchAddress,
			final List<? extends SwitchElement> table) {
		final HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
		for (final SwitchElement e: table) {
			final int key = e.getKey();
			final int destAddress = switchAddress + e.getOffset();
			final int destIndex = addressToIndex.get(destAddress);
			map.put(key, destIndex);
		}
		return Collections.unmodifiableMap(map);
	}

	private void translatePayload(final PayloadInstruction p) {
		final ArrayList<Instruction> defers = payloadDefers.remove(currentCodeAddress);
		if (defers != null) {
			for (final Instruction i: defers) {
				applyPayload(i, p);
			}
		}
		payloadCache.put(currentCodeAddress, p);
	}

	public void translate(final MethodInfo mi, final MethodImplementation impl) {
		this.mi = mi;
		currentCodeAddress = 0;
		final ArrayList<Instruction> insns = new ArrayList<Instruction>();
		
		{
			int reg = impl.getRegisterCount();
			int[] args;
			
			if (!mi.isStatic()) {
				args = new int[mi.signature.paramTypes.size() + 1];
				for (int i = mi.signature.paramTypes.size() - 1; i >= 0; --i) {
					ClassInfo paramType = mi.signature.paramTypes.get(i);
					if (paramType == scope.primitiveLong || paramType == scope.primitiveDouble)
						--reg;
					args[i + 1] = --reg;
				}
				args[0] = --reg;
			} else {
				args = new int[mi.signature.paramTypes.size()];
				for (int i = mi.signature.paramTypes.size() - 1; i >= 0; --i) {
                    ClassInfo paramType = mi.signature.paramTypes.get(i);
					if (paramType == scope.primitiveLong || paramType == scope.primitiveDouble)
						--reg;
					args[i] = --reg;
				}
			}
			
			Instruction i = new Instruction();
			i.opcode = Instruction.OP_SPECIAL;
			i.opcode_aux = Instruction.OP_SP_ARGUMENTS;
			i.extra = args;
			insns.add(i);
		}
		
		// TODO: enumerate try-catch blocks and insert pseudo instructions
		for (final org.jf.dexlib2.iface.instruction.Instruction i: impl.getInstructions()) {
			currentCodeIndex = insns.size();
			addressToIndex.put(currentCodeAddress, currentCodeIndex);

			// Resolve previous address reference
			final ArrayList<Instruction> uis = unresolvedInsns.remove(currentCodeAddress);
			if (uis != null) {
				for (final Instruction ui: uis) {
					switch (ui.opcode) {
					case Instruction.OP_GOTO:
					case Instruction.OP_IF:
						ui.extra = currentCodeIndex;
						break;
					case Instruction.OP_SWITCH:
						@SuppressWarnings("unchecked")
						final Pair<Integer, List<? extends SwitchElement>> extra =
								(Pair<Integer, List<? extends SwitchElement>>) ui.extra;
						ui.extra = resolveSwitchTable(extra.first, extra.second);
						break;
					}
				}
			}
			
			switch (i.getOpcode()) {
			
			/* 00 */ case NOP:
				break;
				
			/* 01 */ case MOVE:
			/* 04 */ case MOVE_WIDE:
			/* 07 */ case MOVE_OBJECT:
				insns.add(translateMove((Instruction12x) i));
				break;
			/* 02 */ case MOVE_FROM16:
			/* 05 */ case MOVE_WIDE_FROM16:
			/* 08 */ case MOVE_OBJECT_FROM16:
				insns.add(translateMove((Instruction22x) i));
				break;
			/* 03 */ case MOVE_16:
			/* 06 */ case MOVE_WIDE_16:
			/* 09 */ case MOVE_OBJECT_16:
				insns.add(translateMove((Instruction32x) i));
				break;
			/* 0a */ case MOVE_RESULT:
			/* 0b */ case MOVE_RESULT_WIDE:
			/* 0c */ case MOVE_RESULT_OBJECT:
			/* 0d */ case MOVE_EXCEPTION:
				insns.add(translateMove((Instruction11x) i));
				break;
				
			/* 0e */ case RETURN_VOID:
				insns.add(translateReturn((Instruction10x) i));
				break;
			/* 0f */ case RETURN:
			/* 10 */ case RETURN_WIDE:
			/* 11 */ case RETURN_OBJECT:
				insns.add(translateReturn((Instruction11x) i));
				break;
	
			/* 12 */ case CONST_4:
				insns.add(translateConst((Instruction11n) i));
				break;
			/* 13 */ case CONST_16:
				insns.add(translateConst((Instruction21s) i));
				break;
			/* 14 */ case CONST:
				insns.add(translateConst((Instruction31i) i));
				break;
			/* 15 */ case CONST_HIGH16:
				insns.add(translateConst((Instruction21ih) i));
				break;
			/* 16 */ case CONST_WIDE_16:
				insns.add(translateConst((Instruction21s) i));
				break;
			/* 17 */ case CONST_WIDE_32:
				insns.add(translateConst((Instruction31i) i));
				break;
			/* 18 */ case CONST_WIDE:
				insns.add(translateConst((Instruction51l) i));
				break;
			/* 19 */ case CONST_WIDE_HIGH16:
				insns.add(translateConst((Instruction21lh) i));
				break;
			/* 1a */ case CONST_STRING:
				insns.add(translateConst((Instruction21c) i));
				break;
			/* 1b */ case CONST_STRING_JUMBO:
				insns.add(translateConst((Instruction31c) i));
				break;
			/* 1c */ case CONST_CLASS:
				insns.add(translateConst((Instruction21c) i));
				break;
				
			/* 1d */ case MONITOR_ENTER:
			/* 1e */ case MONITOR_EXIT:
				insns.add(translateSpecial((Instruction11x) i));
				break;
				
			/* 1f */ case CHECK_CAST:
				insns.add(translateArithmetic((Instruction21c) i));
				break;
			/* 20 */ case INSTANCE_OF:
				insns.add(translateArithmetic((Instruction22c) i));
				break;
			/* 21 */ case ARRAY_LENGTH:
				insns.add(translateArithmetic((Instruction12x) i));
				break;
			/* 22 */ case NEW_INSTANCE:
				insns.add(translateNew((Instruction21c) i));
				break;
			/* 23 */ case NEW_ARRAY:
				insns.add(translateNew((Instruction22c) i));
				break;
			/* 24 */ case FILLED_NEW_ARRAY:
				insns.add(translateNew((Instruction35c) i));
				break;
			/* 25 */ case FILLED_NEW_ARRAY_RANGE:
				insns.add(translateNew((Instruction3rc) i));
				break;
			/* 26 */ case FILL_ARRAY_DATA:
				insns.add(translateNew((Instruction31t) i));
				break;
	
			/* 27 */ case THROW:
				insns.add(translateExceptionOp((Instruction11x) i));
				break;
			
			/* 28 */ case GOTO:
				insns.add(translateGoto((Instruction10t) i));
				break;
			/* 29 */ case GOTO_16:
				insns.add(translateGoto((Instruction20t) i));
				break;
			/* 2a */ case GOTO_32:
				insns.add(translateGoto((Instruction30t) i));
				break;
			
			// 2b..2c 31t
			/* 2b */ case PACKED_SWITCH:
			/* 2c */ case SPARSE_SWITCH:
				insns.add(translateSwitch((Instruction31t) i));
				break;
			
			// 2d..31 23x
			/* 2d */ case CMPL_FLOAT:
			/* 2e */ case CMPG_FLOAT:
			/* 2f */ case CMPL_DOUBLE:
			/* 30 */ case CMPG_DOUBLE:
			/* 31 */ case CMP_LONG:
				insns.add(translateCmp((Instruction23x) i));
				break;
				
			// 32..37 22t
			/* 32 */ case IF_EQ:
			/* 33 */ case IF_NE:
			/* 34 */ case IF_LT:
			/* 35 */ case IF_GE:
			/* 36 */ case IF_GT:
			/* 37 */ case IF_LE:
				insns.add(translateIf((Instruction22t) i));
				break;
				
			// 38..3d 21t
			/* 38 */ case IF_EQZ:
			/* 39 */ case IF_NEZ:
			/* 3a */ case IF_LTZ:
			/* 3b */ case IF_GEZ:
			/* 3c */ case IF_GTZ:
			/* 3d */ case IF_LEZ:
				insns.add(translateIf((Instruction21t) i));
				break;
			
			// 3e..43 unused
			
			// 44..51 23x
			/* 44 */ case AGET:
			/* 45 */ case AGET_WIDE:
			/* 46 */ case AGET_OBJECT:
			/* 47 */ case AGET_BOOLEAN:
			/* 48 */ case AGET_BYTE:
			/* 49 */ case AGET_CHAR:
			/* 4a */ case AGET_SHORT:
			/* 4b */ case APUT:
			/* 4c */ case APUT_WIDE:
			/* 4d */ case APUT_OBJECT:
			/* 4e */ case APUT_BOOLEAN:
			/* 4f */ case APUT_BYTE:
			/* 50 */ case APUT_CHAR:
			/* 51 */ case APUT_SHORT:
				insns.add(translateArrayOp((Instruction23x) i));
				break;
			
			// 52..5f 22c
			/* 52 */ case IGET:
			/* 53 */ case IGET_WIDE:
			/* 54 */ case IGET_OBJECT:
			/* 55 */ case IGET_BOOLEAN:
			/* 56 */ case IGET_BYTE:
			/* 57 */ case IGET_CHAR:
			/* 58 */ case IGET_SHORT:
			/* 59 */ case IPUT:
			/* 5a */ case IPUT_WIDE:
			/* 5b */ case IPUT_OBJECT:
			/* 5c */ case IPUT_BOOLEAN:
			/* 5d */ case IPUT_BYTE:
			/* 5e */ case IPUT_CHAR:
			/* 5f */ case IPUT_SHORT:
				insns.add(translateInstanceOp((Instruction22c) i));
				break;
				
			// 60..6d 21c
			/* 60 */ case SGET:
			/* 61 */ case SGET_WIDE:
			/* 62 */ case SGET_OBJECT:
			/* 63 */ case SGET_BOOLEAN:
			/* 64 */ case SGET_BYTE:
			/* 65 */ case SGET_CHAR:
			/* 66 */ case SGET_SHORT:
			/* 67 */ case SPUT:
			/* 68 */ case SPUT_WIDE:
			/* 69 */ case SPUT_OBJECT:
			/* 6a */ case SPUT_BOOLEAN:
			/* 6b */ case SPUT_BYTE:
			/* 6c */ case SPUT_CHAR:
			/* 6d */ case SPUT_SHORT:
				insns.add(translateStaticOp((Instruction21c) i));
				break;
			
			// 6e..72 35c
			/* 6e */ case INVOKE_VIRTUAL:
			/* 6f */ case INVOKE_SUPER:
			/* 70 */ case INVOKE_DIRECT:
			/* 71 */ case INVOKE_STATIC:
			/* 72 */ case INVOKE_INTERFACE:
				insns.add(translateInvoke((Instruction35c) i));
				break;
			
			// 74..78 3rc
			/* 74 */ case INVOKE_VIRTUAL_RANGE:
			/* 75 */ case INVOKE_SUPER_RANGE:
			/* 76 */ case INVOKE_DIRECT_RANGE:
			/* 77 */ case INVOKE_STATIC_RANGE:
			/* 78 */ case INVOKE_INTERFACE_RANGE:
				insns.add(translateInvoke((Instruction3rc) i));
				break;
				
			// 7b..8f 12x
			/* 7b */ case NEG_INT:
			/* 7c */ case NOT_INT:
			/* 7d */ case NEG_LONG:
			/* 7e */ case NOT_LONG:
			/* 7f */ case NEG_FLOAT:
			/* 80 */ case NEG_DOUBLE:
			/* 81 */ case INT_TO_LONG:
			/* 82 */ case INT_TO_FLOAT:
			/* 83 */ case INT_TO_DOUBLE:
			/* 84 */ case LONG_TO_INT:
			/* 85 */ case LONG_TO_FLOAT:
			/* 86 */ case LONG_TO_DOUBLE:
			/* 87 */ case FLOAT_TO_INT:
			/* 88 */ case FLOAT_TO_LONG:
			/* 89 */ case FLOAT_TO_DOUBLE:
			/* 8a */ case DOUBLE_TO_INT:
			/* 8b */ case DOUBLE_TO_LONG:
			/* 8c */ case DOUBLE_TO_FLOAT:
			/* 8d */ case INT_TO_BYTE:
			/* 8e */ case INT_TO_CHAR:
			/* 8f */ case INT_TO_SHORT:
				insns.add(translateArithmetic((Instruction12x) i));
				break;
	
			// 90..af 23x
			/* 90 */ case ADD_INT:
			/* 91 */ case SUB_INT:
			/* 92 */ case MUL_INT:
			/* 93 */ case DIV_INT:
			/* 94 */ case REM_INT:
			/* 95 */ case AND_INT:
			/* 96 */ case OR_INT:
			/* 97 */ case XOR_INT:
			/* 98 */ case SHL_INT:
			/* 99 */ case SHR_INT:
			/* 9a */ case USHR_INT:
			/* 9b */ case ADD_LONG:
			/* 9c */ case SUB_LONG:
			/* 9d */ case MUL_LONG:
			/* 9e */ case DIV_LONG:
			/* 9f */ case REM_LONG:
			/* a0 */ case AND_LONG:
			/* a1 */ case OR_LONG:
			/* a2 */ case XOR_LONG:
			/* a3 */ case SHL_LONG:
			/* a4 */ case SHR_LONG:
			/* a5 */ case USHR_LONG:
			/* a6 */ case ADD_FLOAT:
			/* a7 */ case SUB_FLOAT:
			/* a8 */ case MUL_FLOAT:
			/* a9 */ case DIV_FLOAT:
			/* aa */ case REM_FLOAT:
			/* ab */ case ADD_DOUBLE:
			/* ac */ case SUB_DOUBLE:
			/* ad */ case MUL_DOUBLE:
			/* ae */ case DIV_DOUBLE:
			/* af */ case REM_DOUBLE:
				insns.add(translateArithmetic((Instruction23x) i));
				break;
				
			// b0..cf 12x
			/* b0 */ case ADD_INT_2ADDR:
			/* b1 */ case SUB_INT_2ADDR:
			/* b2 */ case MUL_INT_2ADDR:
			/* b3 */ case DIV_INT_2ADDR:
			/* b4 */ case REM_INT_2ADDR:
			/* b5 */ case AND_INT_2ADDR:
			/* b6 */ case OR_INT_2ADDR:
			/* b7 */ case XOR_INT_2ADDR:
			/* b8 */ case SHL_INT_2ADDR:
			/* b9 */ case SHR_INT_2ADDR:
			/* ba */ case USHR_INT_2ADDR:
			/* bb */ case ADD_LONG_2ADDR:
			/* bc */ case SUB_LONG_2ADDR:
			/* bd */ case MUL_LONG_2ADDR:
			/* be */ case DIV_LONG_2ADDR:
			/* bf */ case REM_LONG_2ADDR:
			/* c0 */ case AND_LONG_2ADDR:
			/* c1 */ case OR_LONG_2ADDR:
			/* c2 */ case XOR_LONG_2ADDR:
			/* c3 */ case SHL_LONG_2ADDR:
			/* c4 */ case SHR_LONG_2ADDR:
			/* c5 */ case USHR_LONG_2ADDR:
			/* c6 */ case ADD_FLOAT_2ADDR:
			/* c7 */ case SUB_FLOAT_2ADDR:
			/* c8 */ case MUL_FLOAT_2ADDR:
			/* c9 */ case DIV_FLOAT_2ADDR:
			/* ca */ case REM_FLOAT_2ADDR:
			/* cb */ case ADD_DOUBLE_2ADDR:
			/* cc */ case SUB_DOUBLE_2ADDR:
			/* cd */ case MUL_DOUBLE_2ADDR:
			/* ce */ case DIV_DOUBLE_2ADDR:
			/* cf */ case REM_DOUBLE_2ADDR:
				insns.add(translateArithmeticTwoAddr((Instruction12x) i));
				break;
				
			// d0..d7 22s
			/* d0 */ case ADD_INT_LIT16:
			/* d1 */ case RSUB_INT:
			/* d2 */ case MUL_INT_LIT16:
			/* d3 */ case DIV_INT_LIT16:
			/* d4 */ case REM_INT_LIT16:
			/* d5 */ case AND_INT_LIT16:
			/* d6 */ case OR_INT_LIT16:
			/* d7 */ case XOR_INT_LIT16:
				insns.add(translateArithmeticLit((Instruction22s) i));
				break;
				
			// d8..e2 22b
			/* d8 */ case ADD_INT_LIT8:
			/* d9 */ case RSUB_INT_LIT8:
			/* da */ case MUL_INT_LIT8:
			/* db */ case DIV_INT_LIT8:
			/* dc */ case REM_INT_LIT8:
			/* dd */ case AND_INT_LIT8:
			/* de */ case OR_INT_LIT8:
			/* df */ case XOR_INT_LIT8:
			/* e0 */ case SHL_INT_LIT8:
			/* e1 */ case SHR_INT_LIT8:
			/* e2 */ case USHR_INT_LIT8:
				insns.add(translateArithmeticLit((Instruction22b) i));
				break;
			
			// e3..ff (unused)
			/* e3 */ case IGET_VOLATILE:
			/* e4 */ case IPUT_VOLATILE:
			/* e5 */ case SGET_VOLATILE:
			/* e6 */ case SPUT_VOLATILE:
			/* e7 */ case IGET_OBJECT_VOLATILE:
			/* e8 */ case IGET_WIDE_VOLATILE:
			/* e9 */ case IPUT_WIDE_VOLATILE:
			/* ea */ case SGET_WIDE_VOLATILE:
			/* eb */ case SPUT_WIDE_VOLATILE:
			/* ec */ 
			/* ed */ case THROW_VERIFICATION_ERROR:
			/* ee */ case EXECUTE_INLINE:
			/* ef */ case EXECUTE_INLINE_RANGE:
			/* f0 */ case INVOKE_DIRECT_EMPTY:
			/* f0 */ case INVOKE_OBJECT_INIT_RANGE:
			/* f1 */ case RETURN_VOID_BARRIER:
			/* f2 */ case IGET_QUICK:
			/* f3 */ case IGET_WIDE_QUICK:
			/* f4 */ case IGET_OBJECT_QUICK:
			/* f5 */ case IPUT_QUICK:
			/* f6 */ case IPUT_WIDE_QUICK:
			/* f7 */ case IPUT_OBJECT_QUICK:
			/* f8 */ case INVOKE_VIRTUAL_QUICK:
			/* f9 */ case INVOKE_VIRTUAL_QUICK_RANGE:
			/* fa */ case INVOKE_SUPER_QUICK:
			/* fb */ case INVOKE_SUPER_QUICK_RANGE:
			/* fc */ case IPUT_OBJECT_VOLATILE:
			/* fd */ case SGET_OBJECT_VOLATILE:
			/* fe */ case SPUT_OBJECT_VOLATILE:
				Log.err("meet unused op");
				break;
			
			// payloads
			/* 100 */ case PACKED_SWITCH_PAYLOAD:
				translatePayload((PackedSwitchPayload) i);
				break;
			/* 200 */ case SPARSE_SWITCH_PAYLOAD:
				translatePayload((SparseSwitchPayload) i);
				break;
			/* 300 */ case ARRAY_PAYLOAD:
				translatePayload((ArrayPayload) i);
				break;
	
			default:
				Log.err("meet unknown op");
				break;
			}
			
			currentCodeAddress += i.getCodeUnits();
		}
		
		Log.doAssert(unresolvedInsns.isEmpty(), "unresolved instruction");
		Log.doAssert(payloadDefers.isEmpty(), "unresolved payload");
		mi.insns = insns.toArray(new Instruction[insns.size()]);

		// try catch blocks
        ArrayList<TryBlockInfo> tbis = new ArrayList<TryBlockInfo>();
		for (TryBlock tb: impl.getTryBlocks()) {
            final TryBlockInfo tbi = new TryBlockInfo();
            final int start_addr = tb.getStartCodeAddress();
            final int end_addr = start_addr + tb.getCodeUnitCount();
            tbi.startInsnIndex = addressToIndex.get(start_addr);
            if (addressToIndex.containsKey(end_addr)) {
                tbi.endInsnIndex = addressToIndex.get(end_addr);
            } else {
                // the last insn could be partially covered
                int next_insn_addr = Integer.MAX_VALUE;
                for (int addr: addressToIndex.keySet()) {
                    if (addr > end_addr) {
                        next_insn_addr = Math.min(next_insn_addr, addr);
                    }
                }
                tbi.endInsnIndex = next_insn_addr;
            }
			List ehs = tb.getExceptionHandlers();
            ArrayList<TryBlockInfo.ExceptionHandler> l = new ArrayList<TryBlockInfo.ExceptionHandler>();
            for (Object i : ehs) {
                final ExceptionHandler eh = (ExceptionHandler) i;
                final int handler_start_addr = eh.getHandlerCodeAddress();
                ClassInfo exception_type;
                if (eh.getExceptionType() == null) {
                    exception_type = null; // the catch-all handler
                } else {
                    exception_type = Dalvik.findOrCreateClass(scope, eh.getExceptionType());
                }
                final TryBlockInfo.ExceptionHandler translated = new TryBlockInfo.ExceptionHandler();
                translated.exceptionType = exception_type;
                translated.handlerInsnIndex = addressToIndex.get(handler_start_addr);
                l.add(translated);
            }
            tbi.handlers = l.toArray(new TryBlockInfo.ExceptionHandler[l.size()]);
            tbis.add(tbi);
		}
        mi.tbs = tbis.toArray(new TryBlockInfo[tbis.size()]);
	}
}
