package me.hammerle.snuviscript.code;

public class SignInverter extends InputProvider
{
    private final InputProvider input;
    
    public SignInverter(InputProvider input)
    {
        this.input = input;
    }
    
    @Override
    public Object get(Script sc) 
    {
        return -input.getDouble(sc);
    }
    
    @Override
    public double getDouble(Script sc) 
    {
        return -input.getDouble(sc);
    }
    
    @Override
    public String getString(Script sc) 
    {
        return String.valueOf(get(sc));
    }
    
    @Override
    public String toString() 
    {
        return "invertSign(" + input + ")";
    }
}
