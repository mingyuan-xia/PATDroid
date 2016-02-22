package patdroid.dalvik;

import org.junit.Assert;
import org.junit.Test;

public class DalvikTest {
    @Test
    public void testNameConversion() {
        Assert.assertEquals(Dalvik.toDalvikName("java.lang.String"), "Ljava/lang/String;");
        Assert.assertEquals(Dalvik.toDalvikName("byte"), "B");
        Assert.assertEquals(Dalvik.toDalvikName("[Ljava.lang.Object;"), "[Ljava/lang/Object;");
        Assert.assertEquals(Dalvik.toDalvikName("[[[[[[[I"), "[[[[[[[I");
        Assert.assertEquals("java.lang.String", Dalvik.toCanonicalName("Ljava/lang/String;"));
        Assert.assertEquals("byte", Dalvik.toCanonicalName("B"));
        Assert.assertEquals("[Ljava.lang.Object;", Dalvik.toCanonicalName("[Ljava/lang/Object;"));
        Assert.assertEquals("[[[[[[[I", Dalvik.toCanonicalName("[[[[[[[I"));
    }
}
