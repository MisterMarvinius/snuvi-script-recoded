package me.hammerle.snuviscript.code;

public class Instruction
{
    private final int realLine;
    private final byte layer;
    
    private final BasicFunction function;
    private final InputProvider[] input;
    
    public Instruction(int realLine, byte layer, BasicFunction function, InputProvider[] input)
    {
        this.realLine = realLine;
        this.layer = layer;
        this.function = function;
        this.input = input;
    }

    public int getLayer() 
    {
        return layer;
    }
    
    public int getRealLine() 
    {
        return realLine;
    }

    public InputProvider[] getParameters() 
    {
        return input;
    }
    
    @Override
    public String toString() 
    {
        StringBuilder sb = new StringBuilder();
        sb.append(realLine);
        
        for(int j = sb.length(); j < 10; j++)
        {
            sb.insert(0, "0");
        }
        
        sb.append(" | ");
        
        for(int j = 0; j < layer; j++)
        {
            sb.append("    ");
        }
        
        sb.append(function.getName());
        sb.append("(");
        
        for(InputProvider in : input)
        {
            sb.append(in);
            sb.append(", ");
        }
        if(input.length > 0)
        {
            sb.delete(sb.length() - 2, sb.length());
        }
        
        sb.append(")");
        
        return sb.toString();
    }
    
    public void execute(Script sc)
    {
        function.execute(sc, input);
    }
}
