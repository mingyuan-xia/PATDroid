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

import java.util.zip.ZipFile;

/**
 * Emulating an Android file system, containing apps and some system-wise folders (e.g. /proc/).
 * Currently, the emulated fs does not have access control.
 * For apps, their apks are mounted as if they are installed onto the emulated filesystem.
 *
 */
public class EmulatedFS {
    /**
     * The root of the file system
     */
    public final FileNode root = new FileNode();
    /**
     * Emulate a proc file system
     */
    private final FileNode procFS = new FileNode();
    /**
     * The content of /proc/cpuinfo
     */
    private final static String CPUINFO = "Serial: 0000000000000000";
    /**
     * Emulate an external storage card
     */
    private final FileNode sdcardFS = new FileNode();
    public ZipBackedNode apkAssets;

    /**
     * Create an empty Android file system with only system folders
     */
    public EmulatedFS() {
        procFS.setContent("/cpuinfo", CPUINFO);
        root.mount("/proc", procFS);
        root.mount("/sdcard", sdcardFS);
        root.mount("/mnt/sdcard", sdcardFS);
    }

    /**
     * Create an emulated Android file system and load the apk content
     * @param apkFile
     */
    public EmulatedFS(ZipFile apkFile) {
        this();
        loadApk("TODO", apkFile); // TODO: obtain the package name to mount it
    }

    /**
     * Load an apk file to the emulated file system.
     * By spec, the apk file would be located at /data/app/[package_name].apk;
     * Its data would be accessible at /data/data/[package_name]/;
     * @param pkgName
     * @param apkFile
     */
    public void loadApk(String pkgName, ZipFile apkFile) {
        apkAssets = new ZipBackedNode(apkFile, "assets/");
        root.mount("/data/" + pkgName, apkAssets);
    }
}
