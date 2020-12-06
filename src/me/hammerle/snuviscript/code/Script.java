package me.hammerle.snuviscript.code;

import me.hammerle.snuviscript.inputprovider.InputProvider;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import java.util.function.Consumer;
import me.hammerle.snuviscript.exceptions.PreScriptException;
import me.hammerle.snuviscript.exceptions.StackTrace;
import me.hammerle.snuviscript.inputprovider.ReturnWrapper;
import me.hammerle.snuviscript.tokenizer.Tokenizer;
import me.hammerle.snuviscript.inputprovider.Variable;
import me.hammerle.snuviscript.instructions.Instruction;
import me.hammerle.snuviscript.instructions.UserFunction;

public final class Script {
    private static int idCounter = 0;

    private final int id;
    private final String name;
    private final ScriptManager scriptManager;

    private int lineIndex = 0;
    private final Instruction[] code;
    private final Stack<InputProvider> dataStack = new Stack<>();
    private final Stack<Integer> returnStack = new Stack<>();

    private final HashMap<String, Integer> labels = new HashMap<>();
    private final HashMap<String, HashMap<String, Integer>> localLabels = new HashMap<>();
    private final HashMap<String, Variable> vars = new HashMap<>();
    private final Stack<HashMap<String, Variable>> localVars = new Stack<>();
    private final HashMap<String, Integer> functions = new HashMap<>();

    private Stack<Boolean> ifState = new Stack<>();
    private Stack<String> inFunction = new Stack<>();
    private Stack<Boolean> returnVarPop = new Stack<>();

    // waiting scripts stop executing and run again on an event
    private boolean isWaiting;
    // holded scripts do not receive events
    private boolean isHolded;
    private boolean stackTrace;

    private HashSet<String> loadedEvents = new HashSet<>();

    private final Consumer<Script> onTerm;

    private final ArrayList<AutoCloseable> closeables = new ArrayList<>();

    public Script(ScriptManager sm, Consumer<Script> onTerm, String name, String... path) {
        ifState.push(true);
        this.id = idCounter++;
        this.name = name;
        this.scriptManager = sm;
        this.onTerm = onTerm;
        Tokenizer t = new Tokenizer();
        InputStream[] streams = new InputStream[path.length];
        for(int i = 0; i < streams.length; i++) {
            try {
                streams[i] = new FileInputStream(path[i]);
            } catch(FileNotFoundException ex) {
                throw new PreScriptException(ex.getMessage(), -1);
            }
        }
        Compiler c = new Compiler();
        this.code = c.compile(t.tokenize(streams), labels, vars, functions, localLabels);
        //for(Instruction in : code) {
        //    System.out.println(in);
        //}
    }

    private void pushIfNotNull(InputProvider in) {
        if(in != null) {
            dataStack.push(in);
        }
    }

    public void run() {
        isWaiting = false;
        //System.out.println("_________________________");
        long endTime = System.nanoTime() + 15_000_000;
        while(lineIndex < code.length && !isWaiting && !isHolded) {
            Instruction instr = code[lineIndex];
            try {
                //System.out.println("EXECUTE: " + instr + " " + dataStack);
                if(instr.getArguments() > 0) {
                    InputProvider[] args = InputProviderArrayPool.get(instr.getArguments());
                    for(int i = args.length - 1; i >= 0; i--) {
                        args[i] = dataStack.pop();
                    }
                    pushIfNotNull(instr.execute(this, args));
                } else {
                    pushIfNotNull(instr.execute(this, new InputProvider[0]));
                }
                //System.out.println("AFTER EXECUTE: " + dataStack);
                lineIndex++;
            } catch(Exception ex) {
                if(stackTrace) {
                    ex.printStackTrace();
                }
                scriptManager.getLogger().print(null, ex, instr.getName(), name, this, new StackTrace(instr.getLine(), returnStack, code));
                break;
            }

            if(System.nanoTime() > endTime) {
                isHolded = true;
                scriptManager.getScheduler().scheduleTask(() -> {
                    if(!shouldTerm()) {
                        isHolded = false;
                        run();
                    }
                }, 1);
                scriptManager.getLogger().print("auto scheduler was activated", null, instr.getName(), name, this, new StackTrace(instr.getLine(), returnStack, code));
                break;
            }
        }
        //System.out.println(count + " " + (15_000_000 / count));
        if(shouldTerm() && !dataStack.isEmpty()) {
            scriptManager.getLogger().print(String.format("data stack is not empty %s", dataStack));
        }
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public StackTrace getStackTrace() {
        if(lineIndex >= 0 && lineIndex < code.length) {
            return new StackTrace(code[lineIndex].getLine(), returnStack, code);
        }
        return null;
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    private HashMap<String, Integer> getLabels() {
        return inFunction.isEmpty() ? labels : localLabels.get(inFunction.peek());
    }

    public void gotoLabel(String label, boolean error, int add) {
        lineIndex = getLabels().getOrDefault(label, error ? null : lineIndex) + add;
    }

    public void gotoLabel(String label, boolean error) {
        gotoLabel(label, error, 0);
    }

    public void goSub(String label) {
        int line = getLabels().get(label);
        returnStack.push(lineIndex);
        lineIndex = line;
        returnVarPop.push(false);
    }

    public void jumpTo(int jump) {
        lineIndex = jump;
    }

    public void setIfState(boolean state) {
        ifState.pop();
        ifState.push(state);
    }

    public boolean getIfState() {
        return ifState.peek();
    }

    public void handleFunction(String function, InputProvider[] in) throws Exception {
        Integer sub = functions.get(function);
        if(sub == null) {
            throw new IllegalArgumentException(String.format("function '%s' does not exist", function));
        }
        UserFunction uf = (UserFunction) code[sub];
        String[] args = uf.getArgumentNames();

        HashMap<String, Variable> lvars = new HashMap<>();
        if(in.length != args.length) {
            throw new IllegalArgumentException(String.format("invalid number of arguments at function '%s'", function));
        }

        for(int i = 0; i < in.length; i++) {
            Variable v = new Variable(args[i]);
            v.set(this, in[i].get(this));
            lvars.put(args[i], v);
        }

        ifState.push(true);
        localVars.push(lvars);
        returnStack.push(lineIndex);
        lineIndex = sub;
        inFunction.push(function);
        returnVarPop.push(true);
    }

    public void handleReturn(ReturnWrapper wrapper) {
        lineIndex = returnStack.pop();
        if(returnVarPop.pop()) {
            ifState.pop();
            inFunction.pop();
            localVars.pop();
            if(wrapper != null && !code[lineIndex].shouldNotReturnValue()) {
                dataStack.add(wrapper);
            }
        }
    }

    public Variable getOrAddLocalVariable(String name) {
        HashMap<String, Variable> map = localVars.peek();
        Variable v = map.get(name);
        if(v != null) {
            return v;
        }
        v = new Variable(name);
        map.put(name, v);
        return v;
    }

    public InputProvider peekDataStack() {
        return dataStack.peek();
    }

    public void term() {
        lineIndex = code.length;
        isWaiting = false;
    }

    public boolean shouldTerm() {
        return (lineIndex < 0 || lineIndex >= code.length) && !isWaiting;
    }

    public void onTerm() {
        onTerm.accept(this);
        closeables.forEach(c -> {
            scriptManager.getLogger().print("prepared statement not closed", null, null, name, this, null);
            try {
                c.close();
            } catch(Exception ex) {
                scriptManager.getLogger().print("cannot close closeable in script", ex, null, name, this, null);
            }
        });
    }

    public void setHolded(boolean b) {
        isHolded = b;
    }

    public boolean isHolded() {
        return isHolded;
    }

    public void setWaiting() {
        isWaiting = true;
    }

    public boolean isWaiting() {
        return isWaiting;
    }

    public void setVar(String name, Object value) {
        Variable v = vars.get(name);
        if(v != null) {
            v.set(this, value);
        }
    }

    public Variable getVar(String name) {
        return vars.get(name);
    }

    public boolean isEventLoaded(String event) {
        return loadedEvents.contains(event);
    }

    public boolean loadEvent(String event) {
        return loadedEvents.add(event);
    }

    public boolean unloadEvent(String event) {
        return loadedEvents.remove(event);
    }

    public void setStackTrace(boolean b) {
        stackTrace = b;
    }

    public synchronized void addCloseable(AutoCloseable closeable) {
        closeables.add(closeable);
    }

    public synchronized void removeCloseable(AutoCloseable closeable) {
        closeables.remove(closeable);
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof Script && ((Script) o).id == id;
    }
}
