package me.hammerle.snuviscript.code;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import me.hammerle.snuviscript.variable.Variable;

public class Script 
{
    protected int currentLine;
    protected Instruction[] code;
    
    protected final HashMap<String, Integer> labels;
    protected final Stack<Integer> returnStack;
    protected final Stack<HashMap<String, Variable>> localVars;
    
    protected Object returnValue;
    protected final boolean subScript;
    protected final String[] subScriptInput;
    protected final HashMap<String, Script> subScripts;
    
    public Script(List<String> code)
    {
        this.subScriptInput = null;
        this.subScripts = new HashMap<>();
        this.labels = new HashMap<>();
        this.returnStack = new Stack<>();
        this.localVars = new Stack<>();
        this.subScript = false;
        this.currentLine = 0;
        
        this.code = Compiler.compile(this, code, labels, subScript, 0);
        
        /*System.out.println("__________________________________");
        subScripts.forEach((k, v) -> 
        {
            System.out.println(k);
            for(Instruction in : v.code)
            {
                System.out.println(in);
            }
            System.out.println("__________________________________");
        });*/
    }
    
    public Script(List<String> code, String[] subScriptInput, Script sc, int lineOffset)
    {
        this.subScriptInput = subScriptInput;
        this.subScripts = sc.subScripts;
        this.labels = new HashMap<>();
        this.returnStack = new Stack<>();
        this.localVars = sc.localVars;
        this.subScript = true;
        
        this.code = Compiler.compile(this, code, labels, subScript, lineOffset);

        this.currentLine = 0;
    }
    
    public HashMap<String, Variable> getLocalVars()
    {
        return localVars.peek();
    }
    
    public Object run()
    {
        int length = code.length;
        returnValue = null;
        while(currentLine < length)
        {
            //System.out.println("EXECUTE: " + code[currentLine]);
            code[currentLine].execute(this);
            currentLine++;
        }
        return returnValue;
    }
    
    public void end()
    {
        currentLine = code.length;
    }
}
