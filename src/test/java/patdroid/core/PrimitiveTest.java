package patdroid.core;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PrimitiveTest {
    private static final JavaScope javaScope = new JavaScope();

    @Test
    public void testPrimitiveType() {
        assertEquals(15, PrimitiveInfo.fromInt(javaScope, 15).intValue());
        assertEquals(20L, PrimitiveInfo.fromLong(javaScope, 20L).longValue());
        assertEquals(true, PrimitiveInfo.fromBoolean(javaScope, true).booleanValue());
        assertEquals(2.5f, PrimitiveInfo.fromFloat(javaScope, 2.5f).floatValue(), 0.0f);
        assertEquals(6.8, PrimitiveInfo.fromDouble(javaScope, 6.8).doubleValue(), 0.0);
    }

    @Test
    public void testConversion() {
        assertEquals(15L, PrimitiveInfo.fromInt(javaScope, 15).castTo(javaScope.primitiveLong).longValue());
        assertEquals(15, PrimitiveInfo.fromDouble(javaScope, 15.0).castTo(javaScope.primitiveInt).intValue());
    }
}
