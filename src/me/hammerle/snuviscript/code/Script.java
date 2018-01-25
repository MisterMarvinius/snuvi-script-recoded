package me.hammerle.snuviscript.code;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import me.hammerle.snuviscript.variable.LocalVariable;
import me.hammerle.snuviscript.variable.Variable;

public final class Script 
{
    protected final String name;
    protected final int id;
    
    protected ISnuviLogger logger;
    protected ISnuviScheduler scheduler;
    
    protected int currentLine;
    protected Instruction[] code;
    // waiting scripts stop executing and run again on an event
    protected boolean isWaiting;
    // holded scripts do not receive events
    protected boolean isHolded;
    // not valid means the script is waiting for its termination
    protected boolean isValid;
    // states if event broadcasts should be received, otherwise only direct event calls work
    protected boolean receiveEventBroadcast;
    // stores the used cpuTime, schedules the script if too high
    protected long cpuTime;
    
    protected int catchLine;
    protected String currentFunction;
    protected boolean ifState;
    
    protected final HashMap<String, Integer> labels;
    protected final Stack<Integer> returnStack;
    protected HashMap<String, Variable> vars;
    protected final Stack<HashMap<String, Variable>> localVars;
    protected final HashSet<String> events;
    
    protected Object returnValue;
    protected final boolean subScript;
    protected final String[] subScriptInput;
    protected final HashMap<String, Script> subScripts;
    
    protected boolean printStackTrace;
    
    private final Runnable onStart;
    private final Runnable onTerm;
    
    public Script(ISnuviLogger logger, ISnuviScheduler scheduler, List<String> code, String name, 
            int id, Runnable onStart, Runnable onTerm, boolean receiveEventBroadcast)
    {
        this.logger = logger;
        this.scheduler = scheduler;
        this.subScriptInput = null;
        this.subScripts = new HashMap<>();
        this.labels = new HashMap<>();
        this.returnStack = new Stack<>();
        this.localVars = new Stack<>();
        this.events = new HashSet<>();
        this.subScript = false;
        this.currentLine = 0;
        this.isWaiting = false;
        this.isHolded = false;
        this.isValid = true;
        this.receiveEventBroadcast = receiveEventBroadcast;
        this.cpuTime = 0;
        this.catchLine = -1;
        this.currentFunction = null;
        this.ifState = true;
        this.printStackTrace = false;
        this.name = name;
        this.id = id;
        this.onStart = onStart;
        this.onTerm = onTerm;
        
        this.code = Compiler.compile(this, code, labels, subScript, 0);
    }
    
    public Script(List<String> code, String[] subScriptInput, Script sc, int lineOffset)
    {
        this.logger = sc.logger;
        this.scheduler = sc.scheduler;
        this.subScriptInput = subScriptInput;
        this.subScripts = sc.subScripts;
        this.labels = new HashMap<>();
        this.returnStack = new Stack<>();
        this.localVars = sc.localVars;
        this.events = sc.events;
        this.subScript = true;
        this.currentLine = 0;
        this.isWaiting = sc.isWaiting;
        this.isHolded = sc.isHolded;
        this.isValid = sc.isValid;
        this.receiveEventBroadcast = sc.receiveEventBroadcast;
        this.catchLine = -1;
        this.printStackTrace = false;
        this.name = sc.name;
        this.id = sc.id;
        this.onStart = sc.onStart;
        this.onTerm = sc.onTerm;
        
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
        if(isHolded)
        {
            return returnValue;
        }
        int length = code.length;
        returnValue = null;
        isWaiting = false;
        cpuTime = 0;
        long time;
        while(currentLine < length && !isWaiting)
        {
            time = System.nanoTime();
            try
            {
                //System.out.println("EXECUTE: " + code[currentLine]);
                code[currentLine].execute(this);
                currentLine++;
            }
            catch(Exception ex)
            {
                if(printStackTrace)
                {
                    ex.printStackTrace();
                }
                if(catchLine != -1)
                {
                    currentLine = catchLine + 1; // + 1 because currentLine++ isn't happening
                    catchLine = -1;
                    setVar("error", ex.getClass().getSimpleName());
                    continue;
                }
                logger.print(ex.getLocalizedMessage(), ex, currentFunction, name, this, code[currentLine].getRealLine() + 1);
                ex.printStackTrace();
                return returnValue;
            }
            time = System.nanoTime() - time;
            cpuTime += time;
            if(cpuTime > 10_000_000)
            {
                isWaiting = true;
                isHolded = true;
                scheduler.scheduleTask(() -> 
                {           
                    if(isValid)
                    {
                        isHolded = false;
                        run();
                    }
                }, 1);
                return Void.TYPE;
            }
        }
        return returnValue;
    }
    
    public void end()
    {
        currentLine = code.length;
    }
    
    // -------------------------------------------------------------------------
    // general stuff
    // -------------------------------------------------------------------------

    public String getName() 
    {
        return name;
    }
    
    public int getId() 
    {
        return id;
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
    
    public void setVar(String name, Object value)
    {
        HashMap<String, Variable> map;
        if(subScript)
        {
            map = localVars.peek();
            Variable var = map.get(name);
            if(var == null)
            {
                var = new LocalVariable(name);
                map.put(name, var);
            }
            var.set(this, value);
        }
        else
        {
            map = vars;
            Variable var = map.get(name);
            if(var == null)
            {
                var = new Variable(name);
                map.put(name, var);
            }
            var.set(this, value);
        }
    }

    // -------------------------------------------------------------------------
    // event handling
    // -------------------------------------------------------------------------
    
    public boolean isEventLoaded(String s)
    {
        return events.contains(s);
    }
    
    // -------------------------------------------------------------------------
    // onStart onTerm
    // -------------------------------------------------------------------------
    
    public void onStart()
    {
        if(onStart != null)
        {
            onStart.run();
        }
    }
    
    public void onTerm()
    {
        if(onTerm != null)
        {
            onTerm.run();
        }
    }
}
