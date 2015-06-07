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

package patdroid.fs;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ZipBackedNode extends FileNode {
	private final ZipFile zip;
	private final String prefix;
	
	public ZipBackedNode(ZipFile zip) {
		this(zip, "");
	}
	
	public ZipBackedNode(ZipFile zip, String prefix) {
		this.zip = zip;
		this.prefix = prefix;
	}
	
	@Override
	protected InputStream openReadHere(String path) {
		ZipEntry entry = zip.getEntry(prefix + path.substring(1));
		if (entry == null)
			return null;
		try {
			return zip.getInputStream(entry);
		} catch (IOException e) {
			return null;
		}
	}
}
