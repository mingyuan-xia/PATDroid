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

import com.google.common.base.Objects;
import patdroid.util.Log;

/**
 * Low-level primitive type value (immutable).
 */
public final class PrimitiveInfo { 

	/**
	 * The class object of the value type
	 */
	public final ClassInfo type;
	/**
	 * 64-bit store for primitive types
	 */
	public final int low32;
	public final int high32;
	
	private PrimitiveInfo(ClassInfo type, int low32, int high32) {
		this.type = type;
		this.low32 = low32;
		this.high32 = high32;
	}
	
	private PrimitiveInfo(ClassInfo type, long l) {
		this.type = type;
		this.low32 = (int)l;
		this.high32 = (int)(l >> 32);
	}
	
	public static PrimitiveInfo fromInt(Scope scope, int value) {
		return new PrimitiveInfo(scope.primitiveInt, value, 0);
	}

	public static PrimitiveInfo fromLong(Scope scope, long value) {
        return new PrimitiveInfo(scope.primitiveLong, value);
	}

	public static PrimitiveInfo fromDouble(Scope scope, double value) {
        return new PrimitiveInfo(scope.primitiveDouble, Double.doubleToLongBits(value));
	}

	public static PrimitiveInfo fromFloat(Scope scope, float value) {
        return new PrimitiveInfo(scope.primitiveFloat, Float.floatToIntBits(value), 0);
	}

	public static PrimitiveInfo fromBoolean(Scope scope, boolean value) {
        return new PrimitiveInfo(scope.primitiveBoolean, value ? 1 : 0, 0);
	}

	/**
	 * Parse a Java built-in object
	 * 
	 * @param o an arbitrary object of primitive boxing type
	 * @return a PrimitiveInfo
	 */
	public static PrimitiveInfo fromObject(Scope scope, Object o) {
		if (o instanceof Integer) {
			return fromInt(scope, (Integer) o);
		} else if (o instanceof Short) {
			return fromInt(scope, (Short) o);
		} else if (o instanceof Byte) {
			return fromInt(scope, (Byte) o);
		} else if (o instanceof Long) {
			return fromLong(scope, (Long) o);
		} else if (o instanceof Float) {
			return fromFloat(scope, (Float) o);
		} else if (o instanceof Double) {
			return fromDouble(scope, (Double) o);
		} else if (o instanceof Boolean) {
			return fromBoolean(scope, (Boolean) o);
		} else {
			Log.err("unsupported object to ValueInfo" + o.getClass().toString());
			return null;
		}
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

    public PrimitiveInfo unsafeCastTo(ClassInfo targetType) {
        Log.doAssert(targetType.isPrimitive(), "must cast to a primitive type");
        if (targetType == type.scope.primitiveChar ||
                targetType == type.scope.primitiveShort ||
                targetType == type.scope.primitiveByte) {
            targetType = type.scope.primitiveInt;
        }
        return new PrimitiveInfo(targetType, low32, high32);
    }

    public final PrimitiveInfo castTo(ClassInfo targetType) {
		Log.doAssert(targetType.isPrimitive(), "must cast to a primitive type");
        Log.doAssert(targetType.scope == type.scope, "must be in the same scope");
		PrimitiveInfo v = null;
		if (targetType == type.scope.primitiveInt) {
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
			v = fromInt(type.scope, val);
		} else if (targetType == type.scope.primitiveLong) {
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
			v = fromLong(type.scope, val);
		} else if (targetType == type.scope.primitiveFloat) {
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
			v = fromFloat(type.scope, val);
		} else if (targetType == type.scope.primitiveDouble) {
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
			v = fromDouble(type.scope, val);
		} else { // short, boolean, char, byte
			v = new PrimitiveInfo(type.scope.primitiveInt, this.low32);
		}
		return v;
	}

	public static PrimitiveInfo twoIntsToDouble(Scope scope, int vlow, int vhigh) {
		return new PrimitiveInfo(scope.primitiveDouble, vlow, vhigh);
	}

	public static PrimitiveInfo twoIntsToLong(Scope scope, int vlow, int vhigh) {
		return new PrimitiveInfo(scope.primitiveLong, vlow, vhigh);
	}

	public final boolean isInteger() {
		return type == type.scope.primitiveInt;
	}

	public final boolean isLong() {
		return type == type.scope.primitiveLong;
	}
	
	public final boolean isDouble() {
		return type == type.scope.primitiveDouble;
	}

	public final boolean isFloat() {
		return type == type.scope.primitiveFloat;
	}

	public final boolean isBoolean() {
		return type == type.scope.primitiveBoolean;
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
		return this.type == that.type;
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
			Log.err("unsupported ValueInfo type " + type);
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
			Log.err("unsupported ValueInfo type " + type);
			return "";
		}
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(type, low32, high32);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PrimitiveInfo)) {
			return false;
		}
		final PrimitiveInfo v = (PrimitiveInfo) o;
		return v.type == type && v.low32 == low32 && v.high32 == high32;
	}
}
