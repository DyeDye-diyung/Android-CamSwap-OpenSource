package io.github.zensu357.camswap;

import org.junit.Test;

import static org.junit.Assert.*;

public class BufferTransformTest {
    // GLConsumer layout: FlipV * Crop * Orientation, column-major.
    private static final float[] FLIP_V = { 1, 0, 0, 0, 0, -1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 1 };
    private static final float[] CROP = { 0.98f, 0, 0, 0, 0, 0.97f, 0, 0, 0, 0, 1, 0, 0.01f, 0.015f, 0, 1 };

    @Test
    public void recoversEveryTransformAndKeepsOnlyCrop() {
        float[] expected = multiply(FLIP_V, CROP);
        for (int code = 0; code < 8; code++) {
            float[] matrix = multiply(expected, BufferTransform.orientation(code));
            assertEquals(code, BufferTransform.fromMatrix(matrix));
            float[] stripped = new float[16];
            BufferTransform.removeOrientation(matrix, code, stripped);
            assertArrayEquals("code " + code, expected, stripped, 1e-5f);
        }
    }

    @Test
    public void rejectsMatricesThatAreNotAxisAligned() {
        float[] skewed = multiply(FLIP_V, CROP);
        skewed[4] = 0.5f;
        assertEquals(-1, BufferTransform.fromMatrix(skewed));
    }

    private static float[] multiply(float[] a, float[] b) {
        float[] out = new float[16];
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                for (int k = 0; k < 4; k++) out[col * 4 + row] += a[k * 4 + row] * b[col * 4 + k];
            }
        }
        return out;
    }
}
