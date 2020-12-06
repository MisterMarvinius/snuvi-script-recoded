package me.hammerle.snuviscript.instructions;

import me.hammerle.snuviscript.inputprovider.InputProvider;
import me.hammerle.snuviscript.code.Script;

public class Catch extends Goto {
    public Catch(int line) {
        super(line, 0);
    }

    @Override
    public InputProvider execute(Script sc, InputProvider[] o) throws Exception {
        sc.jumpTo(getJump());
        sc.setErrorLine(-1);
        return null;
    }

    @Override
    public String getName() {
        return "catch";
    }
}
