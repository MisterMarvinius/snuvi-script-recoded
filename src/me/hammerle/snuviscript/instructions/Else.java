package me.hammerle.snuviscript.instructions;

import me.hammerle.snuviscript.inputprovider.InputProvider;
import me.hammerle.snuviscript.code.Script;

public class Else extends Goto {
    public Else(int line) {
        super(line, 0);
    }

    @Override
    public InputProvider execute(Script sc, InputProvider[] o) throws Exception {
        if(sc.getIfState()) {
            sc.jumpTo(getJump());
        }
        return null;
    }

    @Override
    public String getName() {
        return "else";
    }
}
