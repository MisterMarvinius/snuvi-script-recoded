package me.hammerle.snuviscript.code;

public final class Vector {
    private double x;
    private double y;
    private double z;

    public Vector(double x, double y, double z) {
        set(x, y, z);
    }

    public void set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }
}
