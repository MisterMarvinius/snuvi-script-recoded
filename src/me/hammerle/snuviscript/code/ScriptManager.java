package me.hammerle.snuviscript.code;

import java.util.ArrayList;
import me.hammerle.snuviscript.inputprovider.InputProvider;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Consumer;
import me.hammerle.snuviscript.exceptions.PreScriptException;

public class ScriptManager {
    private final ISnuviLogger logger;
    private final ISnuviScheduler scheduler;

    private final HashMap<Integer, Script> scripts = new HashMap<>();
    private final HashMap<String, HashSet<Script>> loadedEvents = new HashMap<>();

    private boolean isIterating = false;
    private final ArrayList<Script> addList = new ArrayList<>();
    private final ArrayList<Script> removeList = new ArrayList<>();
    private final ArrayList<Runnable> eventAddList = new ArrayList<>();

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

    // -------------------------------------------------------------------------
    // function registry
    // -------------------------------------------------------------------------
    public void registerFunction(String s, ExceptionBiFunction<Script, InputProvider[], Object> f) {
        FunctionRegistry.registerFunction(s, f);
    }

    public void registerConsumer(String s, ExceptionBiConsumer<Script, InputProvider[]> f) {
        FunctionRegistry.registerConsumer(s, f);
    }

    public void registerAlias(String original, String alias) {
        FunctionRegistry.registerAlias(original, alias);
    }

    // -------------------------------------------------------------------------
    // script controller
    // -------------------------------------------------------------------------
    public Script getScript(int id) {
        return scripts.get(id);
    }

    private void removeScriptUnsafe(Script sc) {
        sc.term();
        sc.onTerm();
        scripts.remove(sc.getId());
        loadedEvents.values().forEach(list -> list.remove(sc));
    }

    private void addScript(Script sc) {
        scripts.put(sc.getId(), sc);
        sc.onStart();
        sc.run();
        if(sc.shouldTerm()) {
            removeScriptUnsafe(sc);
        }
    }

    private void handleQueues() {
        if(!removeList.isEmpty()) {
            removeList.forEach(sc -> removeScriptUnsafe(sc));
            removeList.clear();
        }
        if(!addList.isEmpty()) {
            addList.forEach(sc -> addScript(sc));
            addList.clear();
        }
        if(!eventAddList.isEmpty()) {
            eventAddList.forEach(r -> r.run());
            eventAddList.clear();
        }
    }

    public void removeScriptSafe(Script sc) {
        if(isIterating) {
            removeList.add(sc);
        } else {
            removeScriptUnsafe(sc);
        }
    }

    public boolean removeScriptsSafe() {
        if(isIterating) {
            return false;
        }
        scripts.values().forEach(sc -> {
            sc.term();
            sc.onTerm();
        });
        scripts.clear();
        loadedEvents.values().forEach(list -> list.clear());
        return true;
    }

    public Collection<Script> getScripts() {
        return scripts.values();
    }

    public Script startScript(boolean rEventBroadcast, Consumer<Script> onStart, Consumer<Script> onTerm, String name, String... paths) {
        if(paths.length == 0) {
            return null;
        }
        try {
            Script sc = new Script(this, onStart, onTerm, name, paths);
            sc.setEventBroadcast(rEventBroadcast);
            if(isIterating) {
                addList.add(sc);
            } else {
                addScript(sc);
            }
            return sc;
        } catch(PreScriptException ex) {
            logger.print(null, ex, null, paths[0], null, ex.getLine());
            return null;
        }
    }

    public Script startScript(boolean rEventBroadcast, String name, String... paths) {
        return startScript(rEventBroadcast, null, null, name, paths);
    }

    // -------------------------------------------------------------------------
    // event
    // -------------------------------------------------------------------------
    private void loadEventUnsafe(String event, Script sc) {
        HashSet<Script> set = loadedEvents.get(event);
        if(set == null) {
            set = new HashSet<>();
            loadedEvents.put(event, set);
        }
        set.add(sc);
    }

    private void unloadEventUnsafe(String event, Script sc) {
        HashSet<Script> set = loadedEvents.get(event);
        if(set != null) {
            set.remove(sc);
        }
    }

    public void loadEventSafe(String event, Script sc) {
        if(isIterating) {
            eventAddList.add(() -> loadEventUnsafe(event, sc));
        } else {
            loadEventUnsafe(event, sc);
        }
    }

    public void unloadEventSafe(String event, Script sc) {
        if(isIterating) {
            eventAddList.add(() -> unloadEventUnsafe(event, sc));
        } else {
            unloadEventUnsafe(event, sc);
        }
    }

    public void callEvent(String name, Consumer<Script> before, Consumer<Script> after) {
        HashSet<Script> set = loadedEvents.get(name);
        if(set == null) {
            return;
        }

        isIterating = true;
        try {
            set.stream().filter(sc -> sc.shouldReceiveEventBroadcast() && !sc.isHolded() && sc.isWaiting())
                    .forEach(sc -> {
                        sc.setVar("event", name);
                        if(before != null) {
                            before.accept(sc);
                        }
                        sc.run();
                        if(after != null) {
                            after.accept(sc);
                        }
                    });
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        isIterating = false;
        handleQueues();
    }

    public boolean callEvent(String name, Script sc, Consumer<Script> before, Consumer<Script> after) {
        if(sc.isEventLoaded(name) && !sc.isHolded() && sc.isWaiting()) {
            sc.setVar("event", name);
            if(before != null) {
                before.accept(sc);
            }
            sc.run();
            if(after != null) {
                after.accept(sc);
            }
            if(sc.shouldTerm()) {
                removeScriptUnsafe(sc);
            }
            return true;
        }
        return false;
    }
}
