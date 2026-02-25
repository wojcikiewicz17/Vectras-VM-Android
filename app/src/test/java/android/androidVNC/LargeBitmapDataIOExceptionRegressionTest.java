package android.androidVNC;

import android.graphics.Rect;

import com.antlersoft.android.drawing.RectList;
import com.antlersoft.util.ObjectPool;

import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;

import sun.misc.Unsafe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class LargeBitmapDataIOExceptionRegressionTest {

    @Test
    public void syncScrollShouldBackoffAndScheduleControlledFullRefreshAfterIOException() throws Exception {
        TestableLargeBitmapData bitmapData = allocateLargeBitmapDataWithoutConstructor();
        bitmapData.setNowMs(1000L);
        RfbProto rfb = mock(RfbProto.class);
        doThrow(new IOException("simulated-write-failure"))
                .when(rfb)
                .writeFramebufferUpdateRequest(anyInt(), anyInt(), anyInt(), anyInt(), anyBoolean());

        setField(bitmapData, "rfb", rfb);
        setField(bitmapData, "xoffset", 0);
        setField(bitmapData, "yoffset", 0);
        setField(bitmapData, "scrolledToX", 0);
        setField(bitmapData, "scrolledToY", 0);
        setField(bitmapData, "bitmapwidth", 64);
        setField(bitmapData, "bitmapheight", 64);
        setField(bitmapData, "bitmapRect", new Rect(0, 0, 64, 64));
        RectList invalidList = new RectList(newRectPool());
        invalidList.add(new Rect(0, 0, 16, 16));
        setField(bitmapData, "invalidList", invalidList);
        setField(bitmapData, "pendingList", new RectList(newRectPool()));

        bitmapData.syncScroll();
        bitmapData.syncScroll();

        verify(rfb, times(1))
                .writeFramebufferUpdateRequest(anyInt(), anyInt(), anyInt(), anyInt(), anyBoolean());
        assertTrue((Boolean) getField(bitmapData, "forceFullRefreshOnNextSync"));
        assertEquals(1, getIntField(bitmapData, "framebufferUpdateFailureCount"));
        assertTrue(getLongField(bitmapData, "nextFramebufferUpdateRetryAtMs") > 0L);
    }

    @Test
    public void syncScrollShouldRetryAfterBackoffWindowExpires() throws Exception {
        TestableLargeBitmapData bitmapData = allocateLargeBitmapDataWithoutConstructor();
        bitmapData.setNowMs(5000L);
        RfbProto rfb = mock(RfbProto.class);
        doThrow(new IOException("simulated-write-failure"))
                .when(rfb)
                .writeFramebufferUpdateRequest(anyInt(), anyInt(), anyInt(), anyInt(), anyBoolean());

        setField(bitmapData, "rfb", rfb);
        setField(bitmapData, "xoffset", 0);
        setField(bitmapData, "yoffset", 0);
        setField(bitmapData, "scrolledToX", 0);
        setField(bitmapData, "scrolledToY", 0);
        setField(bitmapData, "bitmapwidth", 64);
        setField(bitmapData, "bitmapheight", 64);
        setField(bitmapData, "bitmapRect", new Rect(0, 0, 64, 64));
        RectList invalidList = new RectList(newRectPool());
        invalidList.add(new Rect(0, 0, 16, 16));
        setField(bitmapData, "invalidList", invalidList);
        setField(bitmapData, "pendingList", new RectList(newRectPool()));

        bitmapData.syncScroll();
        bitmapData.setNowMs(5100L);
        bitmapData.syncScroll();
        bitmapData.setNowMs(5251L);
        bitmapData.syncScroll();

        verify(rfb, times(2))
                .writeFramebufferUpdateRequest(anyInt(), anyInt(), anyInt(), anyInt(), anyBoolean());
        assertEquals(2, getIntField(bitmapData, "framebufferUpdateFailureCount"));
    }

    @Test
    public void syncScrollShouldSignalReconnectAfterRepeatedIOException() throws Exception {
        TestableLargeBitmapData bitmapData = allocateLargeBitmapDataWithoutConstructor();
        bitmapData.setNowMs(10000L);
        RfbProto rfb = mock(RfbProto.class);
        VncCanvas vncCanvas = mock(VncCanvas.class);
        doThrow(new IOException("simulated-write-failure"))
                .when(rfb)
                .writeFramebufferUpdateRequest(anyInt(), anyInt(), anyInt(), anyInt(), anyBoolean());

        setField(bitmapData, "rfb", rfb);
        setField(bitmapData, "vncCanvas", vncCanvas);
        setField(bitmapData, "xoffset", 0);
        setField(bitmapData, "yoffset", 0);
        setField(bitmapData, "scrolledToX", 0);
        setField(bitmapData, "scrolledToY", 0);
        setField(bitmapData, "bitmapwidth", 64);
        setField(bitmapData, "bitmapheight", 64);
        setField(bitmapData, "bitmapRect", new Rect(0, 0, 64, 64));
        RectList invalidList = new RectList(newRectPool());
        invalidList.add(new Rect(0, 0, 16, 16));
        setField(bitmapData, "invalidList", invalidList);
        setField(bitmapData, "pendingList", new RectList(newRectPool()));

        long[] nowValues = new long[]{10000L, 10251L, 10752L, 11753L, 13754L};
        for (long now : nowValues) {
            bitmapData.setNowMs(now);
            bitmapData.syncScroll();
        }

        verify(vncCanvas, times(1)).closeConnection();
        assertTrue((Boolean) getField(bitmapData, "reconnectSuggested"));
        assertEquals(5, getIntField(bitmapData, "framebufferUpdateFailureCount"));
    }

    private static ObjectPool<Rect> newRectPool() {
        return new ObjectPool<Rect>() {
            @Override
            protected Rect itemForPool() {
                return new Rect();
            }
        };
    }

    private static TestableLargeBitmapData allocateLargeBitmapDataWithoutConstructor() throws Exception {
        Unsafe unsafe = getUnsafe();
        return (TestableLargeBitmapData) unsafe.allocateInstance(TestableLargeBitmapData.class);
    }

    private static class TestableLargeBitmapData extends LargeBitmapData {
        private long nowMs;

        private TestableLargeBitmapData() {
            super(null, null, 0, 0, 0);
        }

        void setNowMs(long nowMs) {
            this.nowMs = nowMs;
        }

        @Override
        long currentTimeMs() {
            return nowMs;
        }
    }

    private static Unsafe getUnsafe() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getField(Object target, String fieldName) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static int getIntField(Object target, String fieldName) throws Exception {
        return ((Integer) getField(target, fieldName)).intValue();
    }

    private static long getLongField(Object target, String fieldName) throws Exception {
        return ((Long) getField(target, fieldName)).longValue();
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
