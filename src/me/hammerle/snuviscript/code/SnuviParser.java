package me.hammerle.snuviscript.code;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import me.hammerle.snuviscript.exceptions.PreScriptException;

public class SnuviParser 
{
    private final ISnuviLogger logger;
    private final ISnuviScheduler scheduler;
    
    private int idCounter;
    private final HashMap<Integer, Script> scripts;
    private final LinkedList<Integer> termQueue;
    
    public SnuviParser(ISnuviLogger logger, ISnuviScheduler scheduler)
    {
        this.logger = logger;
        this.scheduler = scheduler;
        
        scripts = new HashMap<>();
        termQueue = new LinkedList<>();
        idCounter = 0;
    }

    public ISnuviLogger getLogger() 
    {
        return logger;
    }

    public ISnuviScheduler getScheduler() 
    {
        return scheduler;
    }
    
    // -----------------------------------------------------------------------------------
    // function registry
    // -----------------------------------------------------------------------------------

    public void registerFunction(String s, BiFunction<Script, InputProvider[], Object> f)
    {
        FunctionLoader.registerFunction(s, f);
    }
    
    public void registerAlias(String original, String alias)
    {
        FunctionLoader.registerAlias(original, alias);
    }
    
    // -----------------------------------------------------------------------------------
    // script controller
    // -----------------------------------------------------------------------------------
    
    public Script getScript(int id)
    {
        return scripts.get(id);
    }
    
    public boolean termUnsafe(Script sc)
    {
        if(sc == null)
        {
            return false;
        }
        sc.isValid = false;
        sc.onTerm();
        return scripts.remove(sc.id) != null;
    }
    
    public void termSafe(Script sc)
    {
        if(sc == null)
        {
            return;
        }
        sc.isHolded = true;
        sc.isWaiting = true;
        sc.isValid = false;
        termQueue.add(sc.id);
    }
    
    private void term()
    {
        if(!termQueue.isEmpty())
        {
            termQueue.forEach(i -> 
            {
                Script sc = scripts.remove(i);
                if(sc != null)
                {
                    sc.onTerm();
                }
            });
            termQueue.clear();
        }
    }
    
    public void termAllUnsafe()
    {
        scripts.values().forEach(sc -> 
        {
            sc.onTerm();
            sc.isValid = false;
            
        });
        scripts.clear();
    }
    
    public Collection<Script> getScripts()
    {
        return scripts.values();
    }
    
    public Script startScript(boolean rEventBroadcast, Consumer<Script> onStart, Consumer<Script> onTerm, String end, String... paths)
    { 
        if(paths.length == 0)
        {
            return null;
        }
        try
        {            
            List<String> code = SnuviUtils.readCode(end, paths);
            String simpleName = paths[0].substring(paths[0].lastIndexOf('/') + 1);
            Script sc = new Script(this, code, simpleName, paths[0], idCounter++, onStart, onTerm, rEventBroadcast);
            scripts.put(sc.id, sc);
            sc.onStart();
            //long l = System.nanoTime();
            sc.run();
            //l = System.nanoTime() - l;
            //System.out.println("time " + l);
            term();
            return sc;
        }
        catch(PreScriptException ex)
        {
            ex.printStackTrace();
            logger.print(ex.getLocalizedMessage(), ex, null, paths[0], null, ex.getEndLine() + 1);
            return null;
        }
    }
    
    public Script startScript(boolean rEventBroadcast, String end, String... paths)
    { 
        return startScript(rEventBroadcast, null, null, end, paths);
    }
    
    // -----------------------------------------------------------------------------------
    // event
    // -----------------------------------------------------------------------------------
    
    public void callEvent(String name, Consumer<Script> before, Consumer<Script> after, Predicate<Script> check)
    {
        scripts.values().stream()
                .filter(sc -> sc.receiveEventBroadcast && !sc.isHolded && sc.isWaiting)
                .filter(sc -> sc.isEventLoaded(name))
                .filter(check)
                .forEach(sc -> 
                {
                    sc.setVar("event", name);
                    if(before != null)
                    {
                        before.accept(sc);
                    }
                    sc.run();
                    if(after != null)
                    {
                        after.accept(sc);
                    }
                });
        term();
    }
    
    public void callEvent(String name, Consumer<Script> before, Consumer<Script> after)
    {
        callEvent(name, before, after, sc -> true);
    }
    
    public boolean callEvent(String name, Script sc, Consumer<Script> before, Consumer<Script> after, boolean check)
    {
        if(sc.isEventLoaded(name) && !sc.isHolded && sc.isWaiting && check)
        {
            sc.setVar("event", name);
            if(before != null)
            {
                before.accept(sc);
            }
            sc.run();
            if(after != null)
            {
                after.accept(sc);
            }
            term();
            return true;
        }
        return false;
    }
    
    public boolean callEvent(String name, Script sc, Consumer<Script> before, Consumer<Script> after)
    {
        return callEvent(name, sc, before, after, true);
    }
}