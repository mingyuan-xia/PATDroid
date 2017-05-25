package patdroid.smali;

import org.junit.Assert;
import org.junit.Test;
import patdroid.core.ClassInfo;
import patdroid.core.MethodInfo;
import patdroid.core.Scope;

import java.io.File;
import java.util.logging.Logger;

public class ClassInfoTest {
    private static final File FRAMEWORK_CLASSES_FOLDER = new File("apilevels");
    private static final int API_LEVEL = 19;
    private final Scope scope = new Scope();
    private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    @Test
    public void testClassInfo() {
        SmaliClassDetailLoader ldr;
        try {
            ldr = SmaliClassDetailLoader.fromFramework(FRAMEWORK_CLASSES_FOLDER, API_LEVEL);
        } catch (RuntimeException e) {
            logger.info("framework classes loader test skipped, API19 not available");
            return ;
        }
        ldr.loadAll(scope);
        ClassInfo urlConnection = scope.findClass("java.net.URLConnection");
        ClassInfo httpUrlConnection = scope.findClass("java.net.HttpURLConnection");
        Assert.assertTrue((httpUrlConnection.isConvertibleTo(urlConnection)));
        Assert.assertFalse(urlConnection.isConvertibleTo(httpUrlConnection));

        MethodInfo[] urlConnectionConnect = urlConnection.findMethodsHere("connect");
        MethodInfo[] httpUrlConnectionConnect = httpUrlConnection.findMethodsHere("connect");
        Assert.assertTrue(urlConnectionConnect.length == 1);
        Assert.assertTrue(httpUrlConnectionConnect.length == 0);

    }
}
