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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import patdroid.util.Log;

/**
 * Low-level primitive type value (immutable).
 */
public final class PrimitiveInfo { 
	
	public static PrimitiveInfo trueValue = new PrimitiveInfo(true);
	public static PrimitiveInfo falseValue = new PrimitiveInfo(false);
	public static Map<ClassInfo, PrimitiveInfo> primitiveZeros;
	static {
		HashMap<ClassInfo, PrimitiveInfo> t = new HashMap<ClassInfo, PrimitiveInfo>();
		t.put(ClassInfo.primitiveVoid, new PrimitiveInfo(0));
		t.put(ClassInfo.primitiveLong, new PrimitiveInfo(0l));
		t.put(ClassInfo.primitiveBoolean, new PrimitiveInfo(false));
		t.put(ClassInfo.primitiveByte, new PrimitiveInfo((byte)0));
		t.put(ClassInfo.primitiveInt, new PrimitiveInfo(0));
		t.put(ClassInfo.primitiveChar, new PrimitiveInfo('\0'));
		t.put(ClassInfo.primitiveDouble, new PrimitiveInfo(0.0));
		t.put(ClassInfo.primitiveFloat, new PrimitiveInfo(0f));
		primitiveZeros = Collections.unmodifiableMap(t);
	}
	
	/**
	 * The class object of the value type
	 */
	final private ClassInfo kind;
	/**
	 * 64-bit store for primitive types
	 */
	public final int low32;
	public final int high32;
	
	/**
	 * Low-level internal constructor
	 * @param kind
	 * @param low32
	 * @param high32
	 */
	private PrimitiveInfo(ClassInfo kind, int low32, int high32) {
		this.kind = kind;
		this.low32 = low32;
		this.high32 = high32;
	}
	
	private PrimitiveInfo(ClassInfo kind, long l) {
		this.kind = kind;
		this.low32 = (int)l;
		this.high32 = (int)(l >> 32);
	}
	
	/**
	 * Create an integer
	 * 
	 * @param value the value
	 */
	public PrimitiveInfo(int value) {
		this(ClassInfo.primitiveInt, value, 0);
	}

	public PrimitiveInfo(long value) {
		this(ClassInfo.primitiveLong, value);
	}

	public PrimitiveInfo(double value) {
		this(ClassInfo.primitiveDouble, Double.doubleToLongBits(value));
	}

	public PrimitiveInfo(float value) {
		this(ClassInfo.primitiveFloat, Float.floatToIntBits(value), 0);
	}

	public PrimitiveInfo(boolean value) {
		this(ClassInfo.primitiveBoolean, value ? 1 : 0, 0);
	}

	/**
	 * Parse a Java built-in object
	 * 
	 * @param o an arbitrary object of primitive boxing type
	 * @return a PrimitiveInfo
	 */
	public static PrimitiveInfo fromObject(Object o) {
		if (o instanceof Integer) {
			return new PrimitiveInfo(((Integer) o).intValue());
		} else if (o instanceof Short) {
			return new PrimitiveInfo(((Short) o).intValue());
		} else if (o instanceof Byte) {
			return new PrimitiveInfo(((Byte) o).intValue());
		} else if (o instanceof Long) {
			return new PrimitiveInfo(((Long) o).longValue());
		} else if (o instanceof Float) {
			return new PrimitiveInfo(((Float) o).floatValue());
		} else if (o instanceof Double) {
			return new PrimitiveInfo(((Double) o).doubleValue());
		} else if (o instanceof Boolean) {
			return new PrimitiveInfo(((Boolean) o).booleanValue());
		} else {
			Log.err("unsupported object to ValueInfo" + o.getClass().toString());
			return null;
		}
	}

	public final ClassInfo getKind() {
		return kind;
	}
	
	private long getLong() {
		return (((long)high32) << 32) | (low32 & 0xffffffffL);
	}
	
	public final int intValue() {
		Log.doAssert(isInteger(), "invalid type");
		return low32;
	}

	public final long longValue() {
		Log.doAssert(isLong(), "invalid type");
		return getLong();
	}

	public final double doubleValue() {
		Log.doAssert(isDouble(), "invalid type");
		return Double.longBitsToDouble(getLong());
	}

	public final float floatValue() {
		Log.doAssert(isFloat(), "invalid type");
		return Float.intBitsToFloat(low32);
	}

	public final boolean booleanValue() {
		Log.doAssert(isBoolean(), "invalid type");
		return low32 == 1;
	}

	public PrimitiveInfo unsafeCastTo(ClassInfo target_type) {
		Log.doAssert(target_type.isPrimitive(), "must cast to a primitive type");
		if (target_type == ClassInfo.primitiveChar || target_type == ClassInfo.primitiveShort || target_type == ClassInfo.primitiveByte) {
			target_type = ClassInfo.primitiveInt;
		}
		return new PrimitiveInfo(target_type, low32, high32);
	}
	
	public final PrimitiveInfo castTo(ClassInfo target_type) {
		Log.doAssert(target_type.isPrimitive(), "must cast to a primitive type");
		PrimitiveInfo v = null;
		if (target_type == ClassInfo.primitiveInt) {
			int val = 0;
			if (isLong()) {
				val = (int) longValue();
			} else if (isInteger()) {
				val = intValue();
			} else if (isBoolean()) {
				val = booleanValue() ? 1 : 0;
			} else if (isFloat()) {
				val = (int) floatValue();
			} else if (isDouble()) {
				val = (int) doubleValue();
			}
			v = new PrimitiveInfo(val);
		} else if (target_type == ClassInfo.primitiveLong) {
			long val = 0l;
			if (isLong()) {
				val = (long) longValue();
			} else if (isInteger()) {
				val = (long) intValue();
			} else if (isBoolean()) {
				val = booleanValue() ? 1 : 0;
			} else if (isFloat()) {
				val = (long) floatValue();
			} else if (isDouble()) {
				val = (long) doubleValue();
			}
			v = new PrimitiveInfo(val);
		} else if (target_type == ClassInfo.primitiveFloat) {
			float val = 0f;
			if (isLong()) {
				val = (float) longValue();
			} else if (isInteger()) {
				val = (float) intValue();
			} else if (isBoolean()) {
				val = booleanValue() ? 1 : 0;
			} else if (isFloat()) {
				val = (float) floatValue();
			} else if (isDouble()) {
				val = (float) doubleValue();
			}
			v = new PrimitiveInfo(val);
		} else if (target_type == ClassInfo.primitiveDouble) {
			double val = 0.0;
			if (isLong()) {
				val = (double) longValue();
			} else if (isInteger()) {
				val = (double) intValue();
			} else if (isBoolean()) {
				val = booleanValue() ? 1 : 0;
			} else if (isFloat()) {
				val = (double) floatValue();
			} else if (isDouble()) {
				val = (double) doubleValue();
			}
			v = new PrimitiveInfo(val);
		} else { // short, boolean, char, byte
			v = new PrimitiveInfo(ClassInfo.primitiveInt, this.low32);
		}
		return v;
	}

	public static PrimitiveInfo twoIntsToDouble(int vlow, int vhigh) {
		return new PrimitiveInfo(ClassInfo.primitiveDouble, vlow, vhigh);
	}

	public static PrimitiveInfo twoIntsToLong(int vlow, int vhigh) {
		return new PrimitiveInfo(ClassInfo.primitiveLong, vlow, vhigh);
	}

	public final boolean isInteger() {
		return kind == ClassInfo.primitiveInt;
	}

	public final boolean isLong() {
		return kind == ClassInfo.primitiveLong;
	}
	
	public final boolean isDouble() {
		return kind == ClassInfo.primitiveDouble;
	}

	public final boolean isFloat() {
		return kind == ClassInfo.primitiveFloat;
	}

	public final boolean isBoolean() {
		return kind == ClassInfo.primitiveBoolean;
	}

	public final boolean isZero() {
		return low32 == 0 && high32 == 0;
	}
	/**
	 * Check if two value are of the same type
	 * 
	 * @param that another value info
	 * @return true if the two values are of same type
	 */
	public final boolean isSameType(PrimitiveInfo that) {
		return this.kind == that.kind;
	}

	@Override
	public final String toString() {
		String prefix = "";
		if (isInteger()) {
			return prefix + intValue();
		} else if (isLong()) {
			return prefix + longValue()+"l";
		} else if (isBoolean()) {
			return prefix + booleanValue();
		} else if (isFloat()) {
			return prefix + floatValue()+"f";
		} else if (isDouble()) {
			return prefix + doubleValue();
		} else {
			Log.err("unsupported ValueInfo kind " + kind);
			return "";
		}
	}

	public String castToString() {
		if (isInteger()) {
			return ""+intValue();
		} else if (isLong()) {
			return ""+longValue();
		} else if (isBoolean()) {
			return ""+booleanValue();
		} else if (isFloat()) {
			return ""+floatValue();
		} else if (isDouble()) {
			return ""+doubleValue();
		} else {
			Log.err("unsupported ValueInfo kind " + kind);
			return "";
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof PrimitiveInfo) {
			final PrimitiveInfo v = (PrimitiveInfo) o;
			return v.kind == kind && v.low32 == low32 && v.high32 == high32;
		} else {
			Log.warnwarn("ValueInfo compared with a non-ValueInfo");
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return (int) (kind.hashCode() * 667 + low32 * 333 + high32);
	}

}
