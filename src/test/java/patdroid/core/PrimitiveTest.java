package patdroid.core;

import org.junit.Assert;
import org.junit.Test;

public class PrimitiveTest {
    @Test
    public void testPrimitiveType() {
        Assert.assertTrue(new PrimitiveInfo(15).intValue() == 15);
        Assert.assertTrue(new PrimitiveInfo(20l).longValue() == 20l);
        Assert.assertTrue(new PrimitiveInfo(true).booleanValue() == true);
        Assert.assertTrue(new PrimitiveInfo(2.5f).floatValue() == 2.5f);
        Assert.assertTrue(new PrimitiveInfo(6.8).doubleValue() == 6.8);
    }

    @Test
    public void testConversion() {
        Assert.assertTrue(new PrimitiveInfo(15).castTo(ClassInfo.globalScope.primitiveLong).longValue() == 15l);
        Assert.assertTrue(new PrimitiveInfo(15.0).castTo(ClassInfo.globalScope.primitiveInt).intValue() == 15);
    }
}
