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

package patdroid.util;

import patdroid.Settings;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class Log {
    public static final int MODE_VERBOSE = 0;
    public static final int MODE_MSG = 1;
    public static final int MODE_DEBUG = 2;
    public static final int MODE_WARNING = 3;
    public static final int MODE_SEVERE_WARNING = 4;
    public static final int MODE_ERROR = 5;
    public static final int MODE_REPORT = 6;
    public static final int MODE_CONCISE_REPORT = 7;
    public static final Writer stdout = new BufferedWriter(new OutputStreamWriter(System.out));
    public static final Writer stderr = new BufferedWriter(new OutputStreamWriter(System.err));
    public static Writer out = stdout;
    public static Writer err = stderr;

    private static ThreadLocal<String> indent = new ThreadLocal<String>() {
        @Override
        protected String initialValue() { return ""; }
    };

    private static void writeLog(int theLevel, String title, String msg, Writer w) {
        if (theLevel >= Settings.logLevel) {
            try {
                w.write(indent.get() + "[" + title + "]: " + msg + "\n");
            } catch (IOException e) {
                // logging system should never die
                exit(1);
            }
        }
    }

    public static void exit(int r) {
        try {
            Log.out.close();
            Log.err.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(r);
    }

    protected static void log(int theLevel, String title, String msg) {
        writeLog(theLevel, title, msg, out);
    }

    protected static void badlog(int theLevel, String title, String msg) {
        switch (theLevel) {
            case MODE_WARNING: Report.nWarnings++; break;
            case MODE_SEVERE_WARNING: Report.nSevereWarnings++; break;
            case MODE_ERROR: Report.nErrors++; break;
            default: break;
        }
        writeLog(theLevel, title, msg, err);
    }

    public static void increaseIndent() { indent.set(indent.get() + "  "); }
    public static void decreaseIndent() { indent.set(indent.get().substring(2)); }
    public static void resetIndent() { indent.remove(); }
    public static void doAssert(boolean b, String msg) { if (!b) { err(msg); } }
    public static void msg(String format, Object... args) { msg(String.format(format, args)); }
    public static void msg(String s) { log(MODE_MSG, "MSG", s);    }
    public static void debug(String format, Object... args) { debug(String.format(format, args));    }
    public static void debug(String s) { log(MODE_DEBUG, "DEBUG", s); }

    private static String exceptionToString(Exception e) {
        String s = e.toString() + "\n";
        StackTraceElement[] st = e.getStackTrace();
        for (StackTraceElement i : st) {
            s += i.toString() + "\n";
        }
        return s;
    }

    public static void warn(String format, Object... args) { warn(String.format(format, args));    }
    public static void warn(Exception e) { warn(exceptionToString(e)); }
    public static void warn(String s) { badlog(MODE_WARNING, "WARN", s); }
    // forgive me for these cute names
    public static void warnwarn(String format, Object... args) { warnwarn(String.format(format, args));    }
    public static void warnwarn(String s) { badlog(MODE_SEVERE_WARNING, "WARN*", s); }
    public static void warnwarn(boolean b, String s) { if (!b) { warnwarn(s); } }
    public static void err(Exception e) { err(exceptionToString(e)); }
    public static void err(String format, Object... args) { err(String.format(format, args)); }
    public static void err(String msg) { badlog(MODE_ERROR, "ERROR", msg); }
}
