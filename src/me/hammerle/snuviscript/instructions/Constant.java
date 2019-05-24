package me.hammerle.snuviscript.instructions;

import me.hammerle.snuviscript.inputprovider.InputProvider;
import me.hammerle.snuviscript.code.Script;

public class Constant extends Instruction
{
    private final InputProvider constant;
    
    public Constant(int line, InputProvider constant)
    {
        super(line);
        this.constant = constant;
    }

    @Override
    public InputProvider execute(Script sc, InputProvider[] o) throws Exception
    {
        return constant;
    }

    @Override
    public String toString()
    {
        return String.format("push %s", constant.toString());
    }
}
