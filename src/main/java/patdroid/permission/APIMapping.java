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

package patdroid.permission;

import patdroid.core.MethodInfo;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Specify what permissions are needed by which APIs.
 * The mapping is bi-directional, i.e. from API(MethodInfo) to Permission(String)
 * and vice verse
 */
public class APIMapping {
    private final HashMap<MethodInfo, ArrayList<String>> mtoperm
            = new HashMap<MethodInfo, ArrayList<String>>();
    private final HashMap<String, ArrayList<MethodInfo>> permtom
            = new HashMap<String, ArrayList<MethodInfo>>();
    public void add(MethodInfo m, String perm) {
        if (mtoperm.containsKey(m)) {
            get(m).add(perm);
        } else {
            ArrayList<String> l = new ArrayList<String>();
            l.add(perm);
            mtoperm.put(m, l);
        }
        if (permtom.containsKey(perm)) {
            get(perm).add(m);
        } else {
            ArrayList<MethodInfo> l = new ArrayList<MethodInfo>();
            l.add(m);
            permtom.put(perm, l);
        }
    }
    public ArrayList<String> get(MethodInfo m) {
        return mtoperm.get(m);
    }
    public ArrayList<MethodInfo> get(String perm) {
        return permtom.get(perm);
    }
}
