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
*/

package patdroid.util;

public final class Pair<T1, T2> {
	final public T1 first;
	final public T2 second;
	
	public Pair(T1 first, T2 second) {
		this.first = first;
		this.second = second;
	}
	
	@Override
	final public boolean equals(Object o) {
		if (o instanceof Pair) {
			Pair<?, ?> u = (Pair<?, ?>)o;
			return first.equals(u.first) && second.equals(u.second);
		}
		return false;
	}
	
	@Override
	final public int hashCode() {
		return 997 * first.hashCode() ^ 991 * second.hashCode();
	}
	
	@Override
	final public String toString() {
		return ("(" + first.toString() + ", " + second.toString() + ")");
	}
}
