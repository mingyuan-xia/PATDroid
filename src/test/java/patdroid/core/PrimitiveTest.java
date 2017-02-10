package patdroid.core;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PrimitiveTest {
    private static final Scope scope = new Scope();

    @Test
    public void testPrimitiveType() {
        assertEquals(15, PrimitiveInfo.fromInt(scope, 15).intValue());
        assertEquals(20L, PrimitiveInfo.fromLong(scope, 20L).longValue());
        assertEquals(true, PrimitiveInfo.fromBoolean(scope, true).booleanValue());
        assertEquals(2.5f, PrimitiveInfo.fromFloat(scope, 2.5f).floatValue(), 0.0f);
        assertEquals(6.8, PrimitiveInfo.fromDouble(scope, 6.8).doubleValue(), 0.0);
    }

    @Test
    public void testConversion() {
        assertEquals(15L, PrimitiveInfo.fromInt(scope, 15).castTo(scope.primitiveLong).longValue());
        assertEquals(15, PrimitiveInfo.fromDouble(scope, 15.0).castTo(scope.primitiveInt).intValue());
    }
}
