package patdroid.smali;

import org.junit.Assert;
import org.junit.Test;
import patdroid.core.Scope;

public class SmaliLoaderTest {
    private final Scope scope = new Scope();

    @Test
    public void testLoadFrameworkClasses() {
        SmaliClassDetailLoader ldr = SmaliClassDetailLoader.fromFramework(19);
        if (ldr == null) {
            System.out.println("framework classes loader test skipped, API19 not available");
            return ;
        }
        ldr.loadAll(scope);
        Assert.assertNotNull(scope.findClass("android.app.Activity"));
        Assert.assertNotNull(scope.findClass("android.view.View"));
        Assert.assertNull(scope.findClass("android.bluetooth.le.ScanResult")); // api21
    }
}
