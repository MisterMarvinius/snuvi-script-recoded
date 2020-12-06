package me.hammerle.snuviscript.code;

import me.hammerle.snuviscript.inputprovider.InputProvider;

public final class NamedFunction {
    private final String name;
    private final ExceptionBiFunction<Script, InputProvider[], Object> f;

    public NamedFunction(String name, ExceptionBiFunction<Script, InputProvider[], Object> f) {
        this.name = name;
        this.f = f;
    }

    public String getName() {
        return name;
    }

    public Object execute(Script sc, InputProvider[] input) throws Exception {
        return f.apply(sc, input);
    }
}
