package me.hammerle.snuviscript.code;

import me.hammerle.snuviscript.inputprovider.InputProvider;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

    private long endTime = 0;

    public Script(ScriptManager sm, Consumer<Script> onTerm, String name, String... path) {
        ifState.push(true);
        this.id = idCounter++;
        this.name = name;
        this.scriptManager = sm;
        this.onTerm = onTerm;

        try {
            List<InputStream> allStreams = new ArrayList<>();
            List<String> allFilePaths = new ArrayList<>();

            // Process each provided script file
            for(String scriptPath : path) {
                // Process imports for this script
                SimpleImportProcessor importProcessor = new SimpleImportProcessor(scriptPath);
                List<String> fileContents = importProcessor.processImportsToFileList(scriptPath);
                List<String> fileOrder = importProcessor.getFileOrder();

                // Convert to InputStreams
                for(String content : fileContents) {
                    allStreams.add(
                            new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                }

                // Track file paths for error reporting
                allFilePaths.addAll(fileOrder);
            }

            // Register file mappings (file counter starts at 1 in tokenizer)
            FileRegistry.registerFiles(1, allFilePaths);

            // Convert to array for tokenizer
            InputStream[] streams = allStreams.toArray(new InputStream[0]);

            Tokenizer t = new Tokenizer();
            Compiler c = new Compiler();
            c.setValidationEnabled(true);
            this.code = c.compile(t.tokenize(streams), labels, vars, functions, localLabels);

        } catch(PreScriptException ex) {
            throw ex;
        } catch(Exception ex) {
            throw new PreScriptException(ex.getMessage(), -1);
        }
    }

    private void pushIfNotNull(InputProvider in) {
        if(in != null) {
            dataStack.push(in);
        }
    }

    public void logException(Exception ex, String instructionName, int line) {
        if(stackTrace) {
            ex.printStackTrace();
        }
        scriptManager.getLogger().print(null, ex, instructionName, name, this,
                new StackTrace(line, returnStack, code));
    }

    public int getLine() {
        if(lineIndex < 0 || lineIndex >= code.length) {
            return -1;
        }
        return code[lineIndex].getLine();
    }

    public void run() {
        isWaiting = false;
        // System.out.println("_________________________");
        endTime = System.nanoTime() + 15_000_000;
        while(lineIndex < code.length && !isWaiting && !isHolded) {
            Instruction instr = code[lineIndex];
            try {
                // System.out.println("EXECUTE: " + instr + " " + dataStack);
                if(instr.getArguments() > 0) {
                    InputProvider[] args = InputProviderArrayPool.get(instr.getArguments());
                    for(int i = args.length - 1; i >= 0; i--) {
                        args[i] = dataStack.pop();
                    }
                    pushIfNotNull(instr.execute(this, args));
                } else {
                    pushIfNotNull(instr.execute(this, new InputProvider[0]));
                }
                // System.out.println("AFTER EXECUTE: " + dataStack);
                lineIndex++;
            } catch(Exception ex) {
                logException(ex, instr.getName(), instr.getLine());
                Integer errorCallback = labels.get("on_error");
                if(errorCallback != null) {
                    setVar("error_stacktrace",
                            new StackTrace(instr.getLine(), returnStack, code).toString());
                    setVar("error_function", instr.getName());
                    setVar("error_name", ex.getClass().getSimpleName());
                    setVar("error_message", ex.getMessage());
                    lineIndex = errorCallback + 1;
                    dataStack.clear();
                    returnStack.clear();
                    ifState.clear();
                    ifState.push(true);
                    localVars.clear();
                    inFunction.clear();
                    returnVarPop.clear();
                } else {
                    term();
                    break;
                }
            }

            if(System.nanoTime() > endTime) {
                isHolded = true;
                scriptManager.getScheduler().scheduleTask("auto", () -> {
                    if(!shouldTerm()) {
                        isHolded = false;
                        run();
                    }
                }, 5);
                scriptManager.getLogger().print("auto scheduler was activated", null,
                        instr.getName(), name, this,
                        new StackTrace(instr.getLine(), returnStack, code));
                break;
            }
        }
        // System.out.println(count + " " + (15_000_000 / count));
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
            throw new IllegalArgumentException(
                    String.format("function '%s' does not exist", function));
        }
        UserFunction uf = (UserFunction) code[sub];
        String[] args = uf.getArgumentNames();

        HashMap<String, Variable> lvars = new HashMap<>();
        if(in.length != args.length) {
            throw new IllegalArgumentException(
                    String.format("invalid number of arguments at function '%s'", function));
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
        if(onTerm != null) {
            onTerm.accept(this);
        }
        closeables.forEach(c -> {
            scriptManager.getLogger().print("prepared statement not closed", null, null, name, this,
                    null);
            try {
                c.close();
            } catch(Exception ex) {
                scriptManager.getLogger().print("cannot close closeable in script", ex, null, name,
                        this, null);
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

    public void addTimer(long l) {
        endTime -= l * 1000000;
    }

    public void mergeFunctions(HashMap<String, Integer> importedFunctions) {
        // Offset function addresses by current code length if needed
        // For now, assume functions are resolved at compile time
        functions.putAll(importedFunctions);
    }

    public void mergeVariables(HashMap<String, Variable> importedVariables) {
        vars.putAll(importedVariables);
    }

    public void mergeLabels(HashMap<String, Integer> importedLabels) {
        labels.putAll(importedLabels);
    }

}
