package patdroid.smali;

import org.junit.Assert;
import org.junit.Test;
import patdroid.Settings;
import patdroid.core.ClassInfo;

public class SmaliLoaderTest {
    @Test
    public void testLoadFrameworkClasses() {
        SmaliClassDetailLoader ldr = SmaliClassDetailLoader.getFrameworkClassLoader(Settings.apiLevel);
        if (ldr == null) {
            return ;
        }
        ldr.loadAll();
        Assert.assertNotNull(ClassInfo.findClass("android.app.Activity"));
        Assert.assertNotNull(ClassInfo.findClass("android.view.View"));
    }
}
