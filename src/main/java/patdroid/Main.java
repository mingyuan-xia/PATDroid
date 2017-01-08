package patdroid;

import patdroid.core.ClassInfo;
import patdroid.core.MethodInfo;
import patdroid.smali.SmaliClassDetailLoader;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

public class Main {
    /**
     * An example using the PATDroid APIs to print all classes
     * and containing methods in an APK file
     * @param args The first arg should be the path/to/apk
     * @throws IOException when the file is not OK
     */
    public static void main(String[] args) throws IOException {
        if (args.length <= 0) {
            System.out.println("Usage: patdroid path/to/apk");
            return;
        }
        // load all framework classes, choose an API level installed
        SmaliClassDetailLoader.getFrameworkClassLoader(19).loadAll();
        // pick an apk
        ZipFile apkFile = new ZipFile(new File(args[0]));
        // load all classes, methods, fields and instructions from an apk
        // we are using smali as the underlying engine
        new SmaliClassDetailLoader(apkFile, true).loadAll();
        // get the class representation for the MainActivity class in the apk
        for (ClassInfo c: ClassInfo.getAllClasses()) {
            if (!c.isFrameworkClass()) {
                System.out.println(c.fullName);
                for (MethodInfo m: c.getAllMethods()) {
                    System.out.println("\t" + m.name);
                }
            }
        }
    }
}
