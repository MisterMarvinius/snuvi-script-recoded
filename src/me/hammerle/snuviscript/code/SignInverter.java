package me.hammerle.snuviscript.code;

public class SignInverter extends InputProvider
{
    private final InputProvider input;
    
    public SignInverter(InputProvider input)
    {
        this.input = input;
    }
    
    @Override
    public Object get(Script sc) throws Exception
    {
        return -input.getDouble(sc);
    }
    
    @Override
    public double getDouble(Script sc) throws Exception
    {
        return -input.getDouble(sc);
    }
    
    @Override
    public String getString(Script sc) throws Exception
    {
        return String.valueOf(get(sc));
    }
    
    @Override
    public String toString() 
    {
        return "-(" + input + ")";
    }
}
