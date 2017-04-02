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

package patdroid;

import patdroid.util.Log;

import java.io.File;

public class Settings {
    /**
     * Minimum log level to be printed
     */
    public static int logLevel = Log.MODE_REPORT;
    /**
     * The report mode generates a JSON output
     */
    public static boolean enableReportMode = logLevel >= Log.MODE_REPORT;
}
