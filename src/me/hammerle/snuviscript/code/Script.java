package me.hammerle.snuviscript.code;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import me.hammerle.snuviscript.variable.Variable;

public final class Script 
{
    protected ISnuviLogger logger;
    
    protected int currentLine;
    protected Instruction[] code;
    protected boolean isWaiting;
    
    protected final HashMap<String, Integer> labels;
    protected final Stack<Integer> returnStack;
    protected HashMap<String, Variable> vars;
    protected final Stack<HashMap<String, Variable>> localVars;
    private final HashSet<String> events;
    
    protected Object returnValue;
    protected final boolean subScript;
    protected final String[] subScriptInput;
    protected final HashMap<String, Script> subScripts;
    
    protected boolean printStackTrace;
    
    protected final String name;
    
    public Script(ISnuviLogger logger, List<String> code, String name)
    {
        this.logger = logger;
        this.subScriptInput = null;
        this.subScripts = new HashMap<>();
        this.labels = new HashMap<>();
        this.returnStack = new Stack<>();
        this.localVars = new Stack<>();
        this.events = new HashSet<>();
        this.subScript = false;
        this.currentLine = 0;
        this.isWaiting = false;
        this.printStackTrace = false;
        this.name = name;
        
        this.code = Compiler.compile(this, code, labels, subScript, 0);
    }
    
    public Script(List<String> code, String[] subScriptInput, Script sc, int lineOffset)
    {
        this.logger = sc.logger;
        this.subScriptInput = subScriptInput;
        this.subScripts = sc.subScripts;
        this.labels = new HashMap<>();
        this.returnStack = new Stack<>();
        this.localVars = sc.localVars;
        this.events = sc.events;
        this.subScript = true;
        this.currentLine = 0;
        this.isWaiting = sc.isWaiting;
        this.printStackTrace = false;
        this.name = sc.name;
        
        this.code = Compiler.compile(this, code, labels, subScript, lineOffset); 
    }
    
    public HashMap<String, Variable> getLocalVars()
    {
        return localVars.peek();
    }
    
    // -------------------------------------------------------------------------
    // flow handling
    // -------------------------------------------------------------------------
    
    public Object run()
    {
        int length = code.length;
        returnValue = null;
        while(currentLine < length && !isWaiting)
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
    
    public void setWaiting(boolean isWaiting)
    {
        this.isWaiting = isWaiting;
    }
    
    // -------------------------------------------------------------------------
    // general stuff
    // -------------------------------------------------------------------------

    public String getName() 
    {
        return name;
    }

    public ISnuviLogger getLogger()
    {
        return logger;
    }
    
    public Variable getVar(String name)
    {
        HashMap<String, Variable> map;
        if(subScript)
        {
            map = localVars.peek();
        }
        else
        {
            map = vars;
        }
        return map.get(name);
    }

    // -------------------------------------------------------------------------
    // event handling
    // -------------------------------------------------------------------------
    
    public void loadEvent(String s)
    {
        events.add(s);
    }
    
    public boolean isEventLoaded(String s)
    {
        return events.contains(s);
    }
    
    public void unloadEvent(String s)
    {
        events.remove(s);
    }
}
