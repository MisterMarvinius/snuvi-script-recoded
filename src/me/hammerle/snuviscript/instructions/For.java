package me.hammerle.snuviscript.instructions;

import me.hammerle.snuviscript.inputprovider.InputProvider;
import me.hammerle.snuviscript.code.Script;

public class For extends Goto {
    public For(int line) {
        super(line, 1);
    }

    @Override
    public InputProvider execute(Script sc, InputProvider[] o) throws Exception {
        if(!o[0].getBoolean(sc)) {
            sc.jumpTo(getJump());
        }
        return null;
    }

    @Override
    public String getName() {
        return "for";
    }
}
