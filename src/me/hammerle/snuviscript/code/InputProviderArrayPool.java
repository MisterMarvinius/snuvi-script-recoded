package me.hammerle.snuviscript.code;

import me.hammerle.snuviscript.inputprovider.InputProvider;

public class InputProviderArrayPool {
    private final static int POOL_SIZE = 10;
    private final static InputProvider[][] IN = new InputProvider[POOL_SIZE][];

    static {
        for(int i = 0; i < IN.length; i++) {
            IN[i] = new InputProvider[i];
        }
    }

    public static InputProvider[] get(int length) {
        if(length < 0 || length >= POOL_SIZE) {
            return new InputProvider[length];
        }
        return IN[length];
    }
}
