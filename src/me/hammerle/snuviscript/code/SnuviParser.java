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
    
    private Script startScript(String path, String end, boolean rEventBroadcast, Runnable onStart, Runnable onTerm)
    { 
        try
        {            
            List<String> code = Utils.readCode(path, end);
            Script sc = new Script(logger, scheduler, code, path, idCounter++, onStart, onTerm, rEventBroadcast);
            scripts.put(sc.id, sc);
            sc.onStart();
            sc.run();
            term();
            return sc;
        }
        catch(PreScriptException ex)
        {
            logger.print(ex.getLocalizedMessage(), ex, null, path, null, ex.getLine());
            return null;
        }
    }
    
    public Script startScript(String path, String end, boolean rEventBroadcast)
    { 
        return startScript(path, end, rEventBroadcast, null, null);
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