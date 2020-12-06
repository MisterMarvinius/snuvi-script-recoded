package me.hammerle.snuviscript.instructions;

import me.hammerle.snuviscript.inputprovider.InputProvider;
import me.hammerle.snuviscript.code.Script;

public class Try extends Goto {
    public Try(int line) {
        super(line, 0);
    }

    @Override
    public InputProvider execute(Script sc, InputProvider[] o) throws Exception {
        sc.setErrorLine(getJump());
        return null;
    }

    @Override
    public String getName() {
        return "try";
    }
}
