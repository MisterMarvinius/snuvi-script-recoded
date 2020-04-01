package me.hammerle.snuviscript.inputprovider;

import me.hammerle.snuviscript.code.Script;

public class ArrayReturnWrapper extends InputProvider {
    private Object array;
    private int index;

    public void setValue(Object o, int index) {
        this.array = o;
        this.index = index;
    }

    @Override
    public Object get(Script sc) {
        return java.lang.reflect.Array.get(array, index);
    }

    @Override
    public void set(Script sc, Object o) {
        java.lang.reflect.Array.set(array, index, o);
    }

    @Override
    public double getDouble(Script sc) {
        return (double) get(sc);
    }

    @Override
    public String getString(Script sc) {
        return String.valueOf(get(sc));
    }

    @Override
    public boolean getBoolean(Script sc) {
        return (Boolean) get(sc);
    }

    @Override
    public String toString() {
        return String.format("ArrayReturnWrapper(%s, %d)", array, index);
    }
}
