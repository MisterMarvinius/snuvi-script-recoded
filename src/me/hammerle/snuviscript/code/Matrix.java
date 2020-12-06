package me.hammerle.snuviscript.code;

public final class Matrix {
    private final double[] data;

    private Matrix(double[] data) {
        this.data = data;
    }

    public Matrix() {
        data = new double[9];
        setIdentity();
    }

    public static Matrix getRotationY(double angle) {
        double[] a = new double[9];
        a[0] = Math.cos(angle);
        a[1] = 0.0;
        a[2] = -Math.sin(angle);
        a[3] = 0.0;
        a[4] = 1.0;
        a[5] = 0.0;
        a[6] = -a[2];
        a[7] = 0.0;
        a[8] = a[0];
        return new Matrix(a);
    }
    
    public static Matrix getRotationX(double angle) {
        double[] a = new double[9];
        a[0] = 1.0;
        a[1] = 0.0;
        a[2] = 0.0;
        a[3] = 0.0;
        a[4] = Math.cos(angle);
        a[5] = -Math.sin(angle);
        a[6] = 0.0;
        a[7] = -a[5];
        a[8] = a[4];
        return new Matrix(a);
    }

    public void setIdentity() {
        data[0] = 1;
        data[1] = 0;
        data[2] = 0;
        data[3] = 0;
        data[4] = 1;
        data[5] = 0;
        data[6] = 0;
        data[7] = 0;
        data[8] = 1;
    }

    public void mul(Vector v) {
        double x = v.getX();
        double y = v.getY();
        double z = v.getZ();
        v.set(data[0] * x + data[3] * y + data[6] * z,
                data[1] * x + data[4] * y + data[7] * z,
                data[2] * x + data[5] * y + data[8] * z);
    }

    public Matrix mul(Matrix m) {
        double[] a = new double[9];
        a[0] = data[0] * m.data[0] + data[3] * m.data[1] + data[6] * m.data[2];
        a[1] = data[1] * m.data[0] + data[4] * m.data[1] + data[7] * m.data[2];
        a[2] = data[2] * m.data[0] + data[5] * m.data[1] + data[8] * m.data[2];
        a[3] = data[0] * m.data[3] + data[3] * m.data[4] + data[6] * m.data[5];
        a[4] = data[1] * m.data[3] + data[4] * m.data[4] + data[7] * m.data[5];
        a[5] = data[2] * m.data[3] + data[5] * m.data[4] + data[8] * m.data[5];
        a[6] = data[0] * m.data[6] + data[3] * m.data[7] + data[6] * m.data[8];
        a[7] = data[1] * m.data[6] + data[4] * m.data[7] + data[7] * m.data[8];
        a[8] = data[2] * m.data[6] + data[5] * m.data[7] + data[8] * m.data[8];
        return new Matrix(a);
    }

    @Override
    public String toString() {
        return String.format("[%.3f %.3f %.3f, %.3f %.3f %.3f, %.3f %.3f %.3f]", 
                data[0], data[3], data[6], data[1], data[4], data[7], data[2], data[5], data[8]);
    }
}
