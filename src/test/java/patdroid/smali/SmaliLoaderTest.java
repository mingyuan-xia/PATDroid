package patdroid.smali;

import org.junit.Assert;
import org.junit.Test;
import patdroid.Settings;
import patdroid.core.ClassInfo;

public class SmaliLoaderTest {
    @Test
    public void testLoadFrameworkClasses() {
        SmaliClassDetailLoader ldr = SmaliClassDetailLoader.getFrameworkClassLoader(19);
        if (ldr == null) {
            System.out.println("framework classes loader test skipped, API19 not available");
            return ;
        }
        ldr.loadAll();
        Assert.assertNotNull(ClassInfo.findClass("android.app.Activity"));
        Assert.assertNotNull(ClassInfo.findClass("android.view.View"));
        Assert.assertNull(ClassInfo.findClass("android.bluetooth.le.ScanResult")); // api21
    }
}
