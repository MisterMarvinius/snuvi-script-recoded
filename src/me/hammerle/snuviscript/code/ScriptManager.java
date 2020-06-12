package me.hammerle.snuviscript.code;

import me.hammerle.snuviscript.inputprovider.InputProvider;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import me.hammerle.snuviscript.exceptions.PreScriptException;
import me.hammerle.snuviscript.exceptions.StackTrace;

public class ScriptManager {
    private final ISnuviLogger logger;
    private final ISnuviScheduler scheduler;

    private final ConcurrentHashMap<Integer, Script> scripts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<Script>> loadedEvents = new ConcurrentHashMap<>();

    public ScriptManager(ISnuviLogger logger, ISnuviScheduler scheduler) {
        this.logger = logger;
        this.scheduler = scheduler;
    }

    public ISnuviLogger getLogger() {
        return logger;
    }

    public ISnuviScheduler getScheduler() {
        return scheduler;
    }

    public void registerFunction(String s, ExceptionBiFunction<Script, InputProvider[], Object> f) {
        FunctionRegistry.registerFunction(s, f);
    }

    public void registerConsumer(String s, ExceptionBiConsumer<Script, InputProvider[]> f) {
        FunctionRegistry.registerConsumer(s, f);
    }

    public void registerAlias(String original, String alias) {
        FunctionRegistry.registerAlias(original, alias);
    }

    public Script getScript(int id) {
        return scripts.get(id);
    }

    public void removeScript(Script sc) {
        sc.term();
        sc.onTerm();
        scripts.remove(sc.getId());
        loadedEvents.values().forEach(list -> list.remove(sc));
    }

    private void addScript(Script sc) {
        scripts.put(sc.getId(), sc);
        sc.run();
        if(sc.shouldTerm()) {
            removeScript(sc);
        }
    }

    public void removeScripts() {
        scripts.values().forEach(sc -> {
            sc.term();
            sc.onTerm();
        });
        scripts.clear();
        loadedEvents.values().forEach(list -> list.clear());
    }

    public Collection<Script> getScripts() {
        return scripts.values();
    }

    public Script startScript(Consumer<Script> onTerm, String name, String... paths) {
        if(paths.length == 0) {
            return null;
        }
        try {
            Script sc = new Script(this, onTerm, name, paths);
            addScript(sc);
            return sc;
        } catch(PreScriptException ex) {
            logger.print(null, ex, null, paths[0], null, new StackTrace(ex.getLine()));
            return null;
        }
    }

    public Script startScript(String name, String... paths) {
        return startScript(sc -> {
        }, name, paths);
    }

    public void loadEvent(String event, Script sc) {
        Set<Script> set = loadedEvents.get(event);
        if(set == null) {
            set = Collections.newSetFromMap(new ConcurrentHashMap<>());
            loadedEvents.put(event, set);
        }
        set.add(sc);
    }

    public void unloadEvent(String event, Script sc) {
        Set<Script> set = loadedEvents.get(event);
        if(set != null) {
            set.remove(sc);
        }
    }

    public void callEvent(String name, Consumer<Script> before, Consumer<Script> after) {
        Set<Script> set = loadedEvents.get(name);
        if(set == null) {
            return;
        }
        try {
            set.stream()
                    .filter(sc -> !sc.isHolded() && sc.isWaiting())
                    .forEach(sc -> runEvent(name, sc, before, after));
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public boolean callEvent(String name, Script sc, Consumer<Script> before, Consumer<Script> after) {
        if(sc.isEventLoaded(name) && !sc.isHolded() && sc.isWaiting()) {
            runEvent(name, sc, before, after);
            return true;
        }
        return false;
    }

    private void runEvent(String name, Script sc, Consumer<Script> before, Consumer<Script> after) {
        sc.setVar("event", name);
        before.accept(sc);
        sc.run();
        after.accept(sc);
        if(sc.shouldTerm()) {
            removeScript(sc);
        }
    }
}
