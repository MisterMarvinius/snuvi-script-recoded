package me.hammerle.snuviscript.instructions;

import me.hammerle.snuviscript.inputprovider.ReturnWrapper;
import me.hammerle.snuviscript.inputprovider.InputProvider;
import me.hammerle.snuviscript.code.Script;

public class SignInverter extends Instruction {
    private final ReturnWrapper wrapper = new ReturnWrapper();

    public SignInverter(int line) {
        super(line);
    }

    @Override
    public int getArguments() {
        return 1;
    }

    @Override
    public InputProvider execute(Script sc, InputProvider[] o) throws Exception {
        wrapper.setValue(-o[0].getDouble(sc));
        return wrapper;
    }

    @Override
    public String getName() {
        return "sign invert";
    }
}
