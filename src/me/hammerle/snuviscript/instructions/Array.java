package me.hammerle.snuviscript.instructions;

import me.hammerle.snuviscript.inputprovider.ArrayReturnWrapper;
import me.hammerle.snuviscript.inputprovider.InputProvider;
import me.hammerle.snuviscript.code.Script;
import me.hammerle.snuviscript.inputprovider.Variable;

public class Array extends Instruction {

    private final int arguments;
    private final ArrayReturnWrapper wrapper = new ArrayReturnWrapper();
    private final Variable v;

    public Array(int line, int arguments, Variable v) {
        super(line);
        this.arguments = arguments;
        this.v = v;
    }

    @Override
    public InputProvider execute(Script sc, InputProvider[] in) throws Exception {
        Object o = v.get(sc);
        for(int i = 0; i < in.length - 1; i++) {
            o = java.lang.reflect.Array.get(o, in[i].getInt(sc));
        }
        wrapper.setValue(o, in[in.length - 1].getInt(sc));
        return wrapper;
    }

    @Override
    public int getArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("push ");
        sb.append(v);
        if(arguments > 0) {
            sb.append("[");
            for(int i = 1; i < arguments; i++) {
                sb.append(",");
            }
            sb.append("]");
        }
        return sb.toString();
    }

    @Override
    public String getName() {
        return "array";
    }
}
