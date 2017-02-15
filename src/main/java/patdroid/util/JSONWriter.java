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

package patdroid.util;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Stack;

public final class JSONWriter implements Closeable, Flushable {
    private final Writer writer;
    private final Stack<String> enders = new Stack<String>();
    private int indent = 0;
    private boolean needComma = false;

    public JSONWriter(Writer writer) {
        this.writer = writer;
    }

    private final String getIndent() {
        return new String(new char[indent]).replace("\0", "\t");
    }

    private final JSONWriter writeItem(String k, String content) throws IOException {
        if (needComma) writer.write(",\n"); else writer.write("\n");
        writer.write(getIndent() + "\"" + k + "\": " + content);
        needComma = true;
        return this;
    }

    private final JSONWriter writeStarter(String starter, String ender, String k) throws IOException {
        if (needComma) writer.write(",\n"); else writer.write("\n");
        writer.write(getIndent());
        writer.write(k != null ? "\"" + k + "\": " : "");
        writer.write(starter);
        enders.push(ender);
        indent++;
        needComma = false;
        return this;
    }

    public final JSONWriter writeStartObject(String k) throws IOException {
        return writeStarter("{", "}", k);
    }

    public final JSONWriter writeStartObject() throws IOException {
        return writeStarter("{", "}", null);
    }

    public final JSONWriter writeStartArray(String k) throws IOException {
        return writeStarter("[", "]", k);
    }

    public final JSONWriter writeEnd() throws IOException {
        writer.write("\n");
        indent--;
        writer.write(getIndent() + enders.pop());
        needComma = true;
        return this;
    }

    public final JSONWriter write(String k, String str) throws IOException {
        return writeItem(k, "\"" + str + "\"");
    }

    public final JSONWriter write(String k, int v) throws IOException {
        return writeItem(k, Integer.toString(v));
    }

    public final JSONWriter write(String k, boolean b) throws IOException {
        return writeItem(k, Boolean.toString(b));
    }

    public final JSONWriter writeNull(String k) throws IOException {
        return writeItem(k, "null");
    }

    public final JSONWriter writeObjectAsString(String k, Object o) throws IOException {
        return (o == null ? writeNull(k) : write(k, o.toString()));
    }

    public JSONWriter writeArray(String k, ArrayList<?> list) throws IOException {
        String s = "";
        int counter = 0;
        s += "[";
        for (Object o : list) {
            s += "\"" + o.toString() + "\"";
            if (++counter != list.size()) {
                s += ", ";
            }
        }
        s += "]";
        return writeItem(k, s);
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        flush();
        writer.close();
    }
}
