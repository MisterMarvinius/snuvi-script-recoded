package me.hammerle.snuviscript.code;

public class Instruction
{
    private final int realLine;
    private final byte layer;   
    private final InputProvider input;
    
    public Instruction(int realLine, byte layer, InputProvider input)
    {
        this.realLine = realLine;
        this.layer = layer;
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
    
    public InputProvider[] getArguments()
    {
        if(input instanceof Function)
        {
            return ((Function) input).getArguments();
        }
        return null; 
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
        
        sb.append(input);
        return sb.toString();
    }
    
    public void execute(Script sc)
    {
        input.get(sc);
    }
}
