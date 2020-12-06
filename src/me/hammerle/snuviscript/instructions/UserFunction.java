package me.hammerle.snuviscript.instructions;

import me.hammerle.snuviscript.inputprovider.InputProvider;
import me.hammerle.snuviscript.code.Script;

public class UserFunction extends Goto {
    private final String[] vars;
    private final String name;

    public UserFunction(int line, String name, String[] vars) {
        super(line, 0);
        this.vars = vars;
        this.name = name;
    }

    @Override
    public InputProvider execute(Script sc, InputProvider[] o) throws Exception {
        sc.jumpTo(getJump());
        return null;
    }

    public String[] getArgumentNames() {
        return vars;
    }

    @Override
    public String getName() {
        return name;
    }
}
