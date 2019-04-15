package me.hammerle.snuviscript.code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;
import me.hammerle.snuviscript.variable.LocalVariable;
import me.hammerle.snuviscript.variable.Variable;

public final class Script 
{
    protected final String simpleName;
    protected final String name;
    protected final int id;
    
    protected SnuviParser parser;
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
    protected String currentCommand;
    protected boolean ifState;
    
    private final HashMap<String, Integer> labels;
    protected final Stack<Integer> returnStack;
    protected HashMap<String, Variable> vars;
    protected final HashSet<String> events;
    
    // local function stuff
    protected final Stack<HashMap<String, Variable>> localVars;
    protected final HashMap<String, Integer> functions;
    protected final HashMap<String, HashMap<String, Integer>> localLabels;
    protected String currentFunction = null;
    
    protected Object returnValue;
    protected boolean printStackTrace;
    
    private final Consumer<Script> onStart;
    private final Consumer<Script> onTerm;
    
    private final List<AutoCloseable> closeables = new ArrayList<>();
    
    public Script(SnuviParser parser, List<String> code, String simpleName, String name,  int id, 
            Consumer<Script> onStart, Consumer<Script> onTerm, boolean receiveEventBroadcast)
    {
        this.parser = parser;
        this.logger = parser.getLogger();
        this.scheduler = parser.getScheduler();
        this.labels = new HashMap<>();
        this.returnStack = new Stack<>();       
        this.events = new HashSet<>();
        this.currentLine = 0;
        this.isWaiting = false;
        this.isHolded = false;
        this.isValid = true;
        this.receiveEventBroadcast = receiveEventBroadcast;
        this.cpuTime = 0;
        this.catchLine = -1;
        this.currentCommand = null;
        this.ifState = true;
        this.printStackTrace = false;
        this.simpleName = simpleName;
        this.name = name;
        this.id = id;
        this.onStart = onStart;
        this.onTerm = onTerm;
        
        this.localVars = new Stack<>();
        this.functions = new HashMap<>();
        this.localLabels = new HashMap<>();
        
        this.code = Compiler.compile(this, code, labels, functions, localLabels);
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
                //System.out.println("LINE 1: " + currentLine);
                code[currentLine].execute(this);
                //System.out.println("LINE 2: " + currentLine);
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
                int line = (currentLine < length) ? code[currentLine].getRealLine() + 1 : -1;
                logger.print(ex.getLocalizedMessage(), ex, currentCommand, name, this, line);
                //ex.printStackTrace();
                return returnValue;
            }
            time = System.nanoTime() - time;
            cpuTime += time;
            if(cpuTime > 15_000_000)
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
        if(currentLine >= length && !isWaiting && localVars.empty())
        {
            parser.termSafe(this);
        }
        return returnValue;
    }
    
    public void end()
    {
        currentLine = code.length;
    }
    
    public int getActiveRealLine()
    {
        return code[currentLine].getRealLine();
    }
    
    // -------------------------------------------------------------------------
    // general stuff
    // -------------------------------------------------------------------------

    public String getSimpleName() 
    {
        return simpleName;
    }

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
    
    public boolean isStackTracePrinted()
    {
        return printStackTrace;
    }
    
    public Variable getVar(String name)
    {
        HashMap<String, Variable> map;
        if(!localVars.isEmpty())
        {
            map = localVars.peek();
            Variable var = map.get(name);
            if(var == null)
            {
                var = new LocalVariable(name);
                map.put(name, var);
            }
            return var;
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
            return var;
        }
    }
    
    public void setVar(String name, Object value)
    {
        HashMap<String, Variable> map;
        if(!localVars.isEmpty())
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
    
    protected Integer getLabel(String name)
    {
        if(localVars.isEmpty())
        {
            return labels.get(name);
        }
        else
        {
            return localLabels.get(currentFunction).get(name);
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
            onStart.accept(this);
        }
    }
    
    public synchronized void onTerm()
    {
        if(onTerm != null)
        {
            onTerm.accept(this);
        }
        closeables.forEach(c -> 
        {
            logger.print("prepared statement not closed", null, null, name, this, -1);
            try
            {
                c.close();
            }
            catch(Exception ex)
            {
                logger.print("cannot close closeable in script", ex, null, name, this, -1);
            }
        });
    }
    
    public synchronized void addCloseable(AutoCloseable closeable)
    {
        closeables.add(closeable);
    }
    
    public synchronized void removeCloseable(AutoCloseable closeable)
    {
        closeables.remove(closeable);
    }
}