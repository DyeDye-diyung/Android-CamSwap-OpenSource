package io.github.zensu357.camswap;

/**
 * Splits a SurfaceTexture matrix into the producer's buffer transform and the remaining crop.
 * GLConsumer builds the matrix as FlipV * Crop * Orientation, where Orientation is
 * FlipH^h * FlipV^v * Rot90^r for NATIVE_WINDOW_TRANSFORM bits FLIP_H=1, FLIP_V=2, ROT_90=4.
 */
final class BufferTransform {
    private static final float EPSILON = 1e-3f;

    private BufferTransform() {}

    /** Returns the NATIVE_WINDOW_TRANSFORM value encoded in the matrix, or -1 if none matches. */
    static int fromMatrix(float[] m) {
        for (int code = 0; code < 8; code++) {
            float[] o = orientation(code);
            // Expected linear part is diag(sx, -sy) * O with positive crop scales.
            if (matches(m[0], o[0]) && matches(m[4], o[4])
                    && matches(m[1], -o[1]) && matches(m[5], -o[5])) {
                return code;
            }
        }
        return -1;
    }

    /** Writes m * Orientation^-1 to out, leaving only the crop and the GL row flip. */
    static void removeOrientation(float[] m, int code, float[] out) {
        multiply(m, invert(orientation(code)), out);
    }

    private static boolean matches(float actual, float expectedSign) {
        if (expectedSign == 0f) return Math.abs(actual) < EPSILON;
        return actual * expectedSign > EPSILON;
    }

    /** Column-major 4x4 orientation matrix acting on (s, t, 0, 1). */
    static float[] orientation(int code) {
        float[] result = identity();
        if ((code & 1) != 0) result = product(result, affine(-1, 0, 0, 1, 1, 0));
        if ((code & 2) != 0) result = product(result, affine(1, 0, 0, -1, 0, 1));
        if ((code & 4) != 0) result = product(result, affine(0, 1, -1, 0, 1, 0));
        return result;
    }

    /** Column-major affine: columns (a, b), (c, d), translation (tx, ty). */
    private static float[] affine(float a, float b, float c, float d, float tx, float ty) {
        float[] m = identity();
        m[0] = a; m[1] = b; m[4] = c; m[5] = d; m[12] = tx; m[13] = ty;
        return m;
    }

    /** Orientation matrices are signed permutations, so the inverse is the transpose. */
    private static float[] invert(float[] m) {
        float[] inv = identity();
        inv[0] = m[0]; inv[1] = m[4]; inv[4] = m[1]; inv[5] = m[5];
        inv[12] = -(inv[0] * m[12] + inv[4] * m[13]);
        inv[13] = -(inv[1] * m[12] + inv[5] * m[13]);
        return inv;
    }

    private static float[] identity() {
        float[] m = new float[16];
        m[0] = m[5] = m[10] = m[15] = 1f;
        return m;
    }

    private static float[] product(float[] a, float[] b) {
        float[] out = new float[16];
        multiply(a, b, out);
        return out;
    }

    private static void multiply(float[] a, float[] b, float[] out) {
        float[] r = new float[16];
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                float sum = 0f;
                for (int k = 0; k < 4; k++) sum += a[k * 4 + row] * b[col * 4 + k];
                r[col * 4 + row] = sum;
            }
        }
        System.arraycopy(r, 0, out, 0, 16);
    }
}
