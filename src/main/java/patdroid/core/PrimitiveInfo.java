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

import patdroid.util.Log;

/**
 * Low-level primitive type value (immutable).
 */
public final class PrimitiveInfo { 

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
			return fromInt(scope, ((Integer) o).intValue());
		} else if (o instanceof Short) {
			return fromInt(scope, ((Short) o).intValue());
		} else if (o instanceof Byte) {
			return fromInt(scope, ((Byte) o).intValue());
		} else if (o instanceof Long) {
			return fromLong(scope, ((Long) o).longValue());
		} else if (o instanceof Float) {
			return fromFloat(scope, ((Float) o).floatValue());
		} else if (o instanceof Double) {
			return fromDouble(scope, ((Double) o).doubleValue());
		} else if (o instanceof Boolean) {
			return fromBoolean(scope, ((Boolean) o).booleanValue());
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

	private final Scope scope() {
	    return (Scope) this.kind.scope;
    }

    public PrimitiveInfo unsafeCastTo(ClassInfo targetType) {
        Log.doAssert(targetType.isPrimitive(), "must cast to a primitive type");
        if (targetType == scope().primitiveChar ||
                targetType == scope().primitiveShort ||
                targetType == scope().primitiveByte) {
            targetType = scope().primitiveInt;
        }
        return new PrimitiveInfo(targetType, low32, high32);
    }

    public final PrimitiveInfo castTo(ClassInfo targetType) {
		Log.doAssert(targetType.isPrimitive(), "must cast to a primitive type");
        Log.doAssert(targetType.scope == scope(), "must be in the same scope");
		PrimitiveInfo v = null;
		if (targetType == scope().primitiveInt) {
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
			v = fromInt(scope(), val);
		} else if (targetType == scope().primitiveLong) {
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
			v = fromLong(scope(), val);
		} else if (targetType == scope().primitiveFloat) {
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
			v = fromFloat(scope(), val);
		} else if (targetType == scope().primitiveDouble) {
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
			v = fromDouble(scope(), val);
		} else { // short, boolean, char, byte
			v = new PrimitiveInfo(scope().primitiveInt, this.low32);
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
		return kind == scope().primitiveInt;
	}

	public final boolean isLong() {
		return kind == scope().primitiveLong;
	}
	
	public final boolean isDouble() {
		return kind == scope().primitiveDouble;
	}

	public final boolean isFloat() {
		return kind == scope().primitiveFloat;
	}

	public final boolean isBoolean() {
		return kind == scope().primitiveBoolean;
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
