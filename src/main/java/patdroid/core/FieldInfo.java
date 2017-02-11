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

import java.util.HashMap;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import patdroid.util.Log;

/**
 * The representation of a field in a class
 */
public final class FieldInfo {
	/**
	 * The owning class of this filed
	 */
	public final ClassInfo owner;
	/**
	 * The name of the field
	 */
	public final String fieldName;

	public FieldInfo(ClassInfo owner, String fieldName) {
		this.owner = owner;
		this.fieldName = fieldName;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof FieldInfo)) {
			return false;
		}
		final FieldInfo that = (FieldInfo)o;
		return owner == that.owner && this.fieldName.equals(that.fieldName);
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(owner, fieldName);
	}

	@Override
	public String toString() {
		return owner.toString() + "." + fieldName;
	}

	public final ClassInfo getFieldType() {
		return owner.getFieldType(fieldName);
	}
	
	public boolean isValid() {
		return owner.getFieldType(fieldName) != null;
	}

	/*
	 * Bind to the real owner of the field, which may not be loaded at
	 * decompiling stage, thus late bind is needed.
	 */
	public FieldInfo bind() {
		ClassInfo type = owner;
		while (true) {
			ImmutableMap<String, ClassInfo> fields = type.getAllFieldsHere();
			if (fields != null && fields.containsKey(fieldName))
				return new FieldInfo(type, fieldName);
			final ClassInfo baseType = type.getBaseType();
			if (baseType == null) {
				Log.warn("field bind failed");
				return new FieldInfo(type, fieldName);
			}
			type = baseType;
		}
	}
}
