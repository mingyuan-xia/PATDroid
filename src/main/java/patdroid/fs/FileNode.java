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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import patdroid.util.Pair;

/**
 * A FileNode represents a set of files and mount points
 */
public class FileNode {
	private final HashMap<String, FileNode> mountList = new HashMap<String, FileNode>();
	private final HashMap<String, byte[]> fileList = new HashMap<String, byte[]>();

	/**
	 * process a path into an array of folders
	 * @param path e.g. /foo/bar/.././haha
	 * @return e.g. ["foo", "haha"]
	 */
	private static String[] norm(String path) {
		String[] elements = path.split("[/\\\\]");
		ArrayList<String> stack = new ArrayList<String>();
		for (String e: elements) {
			if (e.isEmpty() || e.equals("."))
				continue;
			if (e.equals("..")) {
				if (!stack.isEmpty())
					stack.remove(stack.size() - 1);
				else
					return null;
				continue;
			}
			stack.add(e);
		}
		return stack.toArray(new String[stack.size()]);
	}

	private Pair<FileNode, String> dispatch(String path) {
		String[] norm = norm(path);
		if (norm == null)
			return null;

		FileNode node = this;
		String newPath = "";
		for (String e: norm) {
			newPath += "/" + e;
			if (node.mountList.containsKey(newPath)) {
				node = node.mountList.get(newPath);
				newPath = "";
			}
		}
		
		return new Pair<FileNode, String>(node, newPath);
	}
	
	public final void mount(String path, FileNode node) {
		Pair<FileNode, String> u = dispatch(path);
		u.first.mountHere(u.second, node);
	}
	
	protected void mountHere(String path, FileNode node) {
		mountList.put(path, node);
	}
	
	public final InputStream openRead(String path) {
		Pair<FileNode, String> u = dispatch(path);
		return u.first.openReadHere(u.second);
	}
	
	protected InputStream openReadHere(String path) {
		byte[] content = fileList.get(path);
		if (content == null)
			return null;
		return new ByteArrayInputStream(content);
	}

	/**
	 * Set the content of a file in the node.
	 * The new content will override existing content if any.
	 * @param path the path to the file
	 * @param content the content of the file
	 */
	public final void setContent(String path, byte[] content) {
		Pair<FileNode, String> u = dispatch(path);
		u.first.setContentHere(u.second, content);
	}

	public final void setContent(String path, String content) {
		setContent(path, content.getBytes());
	}
	
	protected void setContentHere(String path, byte[] content) {
		fileList.put(path, content);
	}
	
}
