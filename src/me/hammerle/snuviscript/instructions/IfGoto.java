package me.hammerle.snuviscript.instructions;

import me.hammerle.snuviscript.inputprovider.InputProvider;
import me.hammerle.snuviscript.code.Script;

public class IfGoto extends Goto {
    private final boolean check;

    public IfGoto(int line, boolean check) {
        super(line, 0);
        this.check = check;
    }

    @Override
    public InputProvider execute(Script sc, InputProvider[] o) throws Exception {
        boolean b = sc.peekDataStack().getBoolean(sc);
        if(b == check) {
            sc.jumpTo(getJump());
        }
        return null;
    }

    @Override
    public String getName() {
        return "if goto";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        sb.append("(");
        sb.append(getJump());
        sb.append(", ");
        sb.append(check);
        sb.append(")");
        return sb.toString();
    }
}
