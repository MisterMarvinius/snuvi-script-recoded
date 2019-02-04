package me.hammerle.snuviscript.code;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import me.hammerle.snuviscript.constants.ConstantDouble;
import me.hammerle.snuviscript.constants.ConstantNull;
import me.hammerle.snuviscript.constants.ConstantString;
import me.hammerle.snuviscript.array.DynamicArray;
import me.hammerle.snuviscript.constants.ConstantBoolean;
import me.hammerle.snuviscript.variable.ArrayVariable;
import me.hammerle.snuviscript.variable.LocalArrayVariable;
import me.hammerle.snuviscript.variable.LocalVariable;
import me.hammerle.snuviscript.variable.Variable;
import me.hammerle.snuviscript.exceptions.PreScriptException;

public class Compiler 
{
    public static Instruction[] compile(
            Script sc, List<String> sCode, HashMap<String, Integer> labels, 
            HashMap<String, Integer> functions, HashMap<String, HashMap<String, Integer>> localLabels)
    {
        Compiler compiler = new Compiler(sCode, labels, functions, localLabels);
        Instruction[] instructions = compiler.compile();
        sc.vars = compiler.vars;
        return instructions;
    }
    
    private final List<String> sCode;
    private final HashMap<String, Variable> vars = new HashMap<>();
    private final HashMap<String, Integer> labels;
    
    private final HashMap<String, Variable> localVars = new HashMap<>();
    private final HashMap<String, Integer> functions;
    private final HashMap<String, HashMap<String, Integer>> localLabels;
    private String currentFunction = null;
    
    private final LinkedList<Instruction> code = new LinkedList<>();;
    private int line = 0;
    private int layer = 0;
    
    private JumpData tryState = null;
    
    private class JumpWrapper
    {
        private final JumpData data;
        private final String function;
        
        public JumpWrapper(JumpData data, String function)
        {
            this.data = data;
            this.function = function;
        }
    }
    
    private final Stack<JumpWrapper> jumps = new Stack<>();
    private final Stack<JumpWrapper> loopJumps = new Stack<>();
    private final LinkedList<JumpData> breakContinueJumps = new LinkedList<>();
    
    private final HashMap<String, String> strings = new HashMap<>();
    private int stringCounter = 0;
    
    private Compiler(List<String> sCode, HashMap<String, Integer> labels, 
            HashMap<String, Integer> functions, HashMap<String, HashMap<String, Integer>> localLabels)
    {
        this.sCode = sCode;
        this.labels = labels;       
        this.functions = functions;
        this.localLabels = localLabels;
    }
    
    private void addCodeInstruction(String function, InputProvider[] input)
    {
        code.add(new Instruction(line + 1, (byte) layer, new Function(FunctionLoader.getFunction(function), input)));
    }
    
    private void addLabel(String name, int line)
    {
        if(currentFunction != null)
        {
            HashMap<String, Integer> map = localLabels.get(currentFunction);
            if(map.put(name, line) != null)
            {
                throw new PreScriptException("label duplicate", line);
            }
        }
        else if(labels.put(name, line) != null)
        {
            throw new PreScriptException("label duplicate", line);
        }
    }
    
    private Instruction[] compile()
    {
        int size = sCode.size();
        //System.out.println("__________________________________");
        
        StringBuilder sb = new StringBuilder();
        String replacement;
        String check;
        int pos;
        int old = 0;
        boolean text = false;
        boolean comment = false;
        int labelIndex;
        
        for(line = 0; line < size; line++)
        {
            pos = sb.length();
            sb.append(sCode.get(line));
            
            while(pos < sb.length())
            {
                if(comment)
                {
                    if(pos + 1 < sb.length() && sb.charAt(pos) == '*' && sb.charAt(pos + 1) == '/')
                    {
                        comment = false;
                        sb.delete(old, pos + 2);
                    }
                    pos++;
                    continue;
                }
                else if(text)
                {
                    if(sb.charAt(pos) == '"')
                    {
                        replacement = "#" + stringCounter++;
                        strings.put(replacement, sb.substring(old, pos + 1));
                        text = false;
                        sb.replace(old, pos + 1, replacement);
                        pos = old - 1 + replacement.length();
                    }
                    pos++;
                    continue;
                }
                switch(sb.charAt(pos))
                {
                    case '/':
                        if(pos + 1 < sb.length())
                        {
                            switch(sb.charAt(pos + 1))
                            {
                                case '/':
                                    sb.delete(pos, sb.length());
                                    break;
                                case '*':
                                    comment = true;
                                    old = pos;
                                    pos++;
                                    break;
                            }
                        }
                        break;
                    case '}':
                        sb.delete(0, pos + 1);
                        pos = -1;
                        layer--;
                        if(jumps.isEmpty())
                        {
                            throw new PreScriptException("} without a corresponding function and / or {", line);
                        }
                        JumpWrapper data = jumps.pop();
                        switch(data.function)
                        {
                            case "function":
                            {
                                data.data.setRelativeJump(code.size());
                                currentFunction = null;
                                break;
                            }
                            case "try":
                            {
                                tryState = data.data;
                                break;
                            }
                            case "catch":
                            {
                                data.data.setRelativeJump(code.size());
                                break;
                            }
                            case "else":
                            case "elseif":
                            case "if":
                            {
                                data.data.setRelativeJump(code.size() + 1);
                                addCodeInstruction("endif", new InputProvider[] {});
                                break;
                            }
                            case "for":
                            {
                                loopJumps.pop();
                                createBreakContinue(code.size());
                                JumpData jump = data.data;
                                jump.setRelativeJump(code.size());
                                addCodeInstruction("next", new InputProvider[] {new JumpData(-jump.getInt(null) - 1)});
                                break;
                            }
                            case "while":
                            {
                                loopJumps.pop();
                                createBreakContinue(code.size());

                                JumpData jump = data.data;
                                jump.setRelativeJump(code.size() + 1);
                                addCodeInstruction("wend", new InputProvider[] {new JumpData(-jump.getInt(null) - 1)});
                                break;
                            }
                        }
                        break;
                    case '{':
                        int currentJumps = jumps.size();
                        check = sb.toString();
                        if(check.startsWith("function "))
                        {
                            if(currentFunction != null)
                            {
                                throw new PreScriptException("function not allowed in another function", line);
                            }
                            int index = check.indexOf("(");
                            if(index == -1)
                            {
                                throw new PreScriptException("missing function syntax", line);
                            }
                            currentFunction = check.substring(9, index).toLowerCase();
                            functions.put(currentFunction, code.size());
                            localLabels.put(currentFunction, new HashMap<>());
                            int endIndex = check.indexOf(")", index);
                            if(index == -1)
                            {
                                throw new PreScriptException("missing function syntax", line);
                            }
                            String[] inputs;
                            if(index + 1 == endIndex)
                            {
                                inputs = new String[0];
                            }
                            else
                            {
                                inputs = check.substring(index + 1, endIndex).split("[ ]*,[ ]*");
                            }
                            InputProvider[] in = new InputProvider[inputs.length + 1];
                            for(int i = 1; i < in.length; i++)
                            {
                                in[i] = new ConstantString(inputs[i - 1]);
                            }
                            JumpData jump = new JumpData(code.size());
                            in[0] = jump;
                            jumps.add(new JumpWrapper(jump, "function"));
                            addCodeInstruction("function", in);
                           
                            pos = endIndex + 1;
                            boolean b = true;
                            while(b)
                            {
                                switch(sb.charAt(pos))
                                {
                                    case '{':
                                        b = false;
                                        break;
                                    case '\n':
                                    case ' ':
                                        break;
                                    default:
                                        throw new PreScriptException("invalid character between function and {", line);
                                }
                                pos++;
                            }
                            
                            layer++;
                            sb.delete(0, pos);
                        }
                        else
                        {
                            check = sb.substring(0, pos);
                            compileLine(check);
                            sb.delete(0, pos + 1);
                            layer++;
                            if(currentJumps == jumps.size())
                            {
                                throw new PreScriptException("{ without a corresponding function", line);
                            }
                        }
                        pos = -1;
                        break;
                    case ';':
                        compileLine(sb.substring(0, pos).trim());
                        sb.delete(0, pos + 1);
                        pos = -1;
                        break;
                    case '"':
                        text = true;
                        old = pos;
                        break;
                }
                pos++;
            }
            if(!text && !comment)
            {
                labelIndex = sb.indexOf("@");
                if(labelIndex != -1)
                {
                    String label = sb.toString().trim();
                    if(label.charAt(0) != '@')
                    {
                        throw new PreScriptException("you seriously fucked up the syntax here", line);
                    }
                    addLabel(label.substring(1), code.size() - 1);
                    sb = new StringBuilder();
                }
            }
        }
        
        //System.out.println("__________________________________");
        
        Instruction[] input = code.toArray(new Instruction[code.size()]);
        
        /*for(Instruction in : input)
        {
            System.out.println(in);
        }
        System.out.println("__________________________________");*/
        /*labels.entrySet().stream().forEach((e) -> 
        {
            System.out.println("LABEL " + e.getKey() + " " + e.getValue());
        });*/
        //System.out.println("__________________________________");
        return input;
    }
    
    private void compileLine(String currentCode)
    {
        //System.out.println(">>>"  + currentCode);
        String[] parts = SnuviUtils.split(strings, currentCode, line);
        //System.out.println(">>> " + String.join("_", parts));
        if(tryState != null)
        {
            switch(parts.length)
            {
                case 0: return;
                case 1: 
                    if(!parts[0].equals("catch"))
                    {
                        throw new PreScriptException("no catch after try", line);
                    }
                    if(tryState == null)
                    {
                        throw new PreScriptException("catch without try", line);
                    }
                    tryState.setRelativeJump(code.size());
                    JumpData jump = new JumpData(code.size());
                    addCodeInstruction("catch", new InputProvider[] {jump});
                    jumps.push(new JumpWrapper(jump, "catch"));
                    tryState = null;
                    return;
                default:
                    throw new PreScriptException("invalid catch after try", line);
            }
        }
        
        if(parts.length == 0)
        {
            return;
        }
        else if(parts[0].equals("return"))
        {
            addCodeInstruction("return", compileFunction(parts, true));
            return;
        }
        else if(parts[0].startsWith("@"))
        {
            if(parts.length > 1)
            {
                throw new PreScriptException("arguments after label", line);
            }
            addLabel(parts[0].substring(1), code.size() - 1);
            return;
        }
        
        String input;
        if(parts.length == 1)
        {
            int bPos = parts[0].indexOf('(');
            if(bPos != -1)
            {
                input = parts[0].substring(0, bPos);
                parts = SnuviUtils.split(strings, parts[0].substring(bPos + 1, parts[0].length() - 1), line);
            }
            else
            {
                switch(parts[0])
                {
                    case "try":
                    {
                        JumpData jump = new JumpData(code.size());
                        addCodeInstruction("try", new InputProvider[] {jump});
                        jumps.push(new JumpWrapper(jump, "try"));
                        return;
                    }
                    case "else":
                    {
                        JumpData jump = new JumpData(code.size());
                        addCodeInstruction("else", new InputProvider[] {jump});
                        jumps.push(new JumpWrapper(jump, "else"));
                        return;
                    }
                    case "while":
                        throw new PreScriptException("missing syntax at while", line);
                    case "if":
                        throw new PreScriptException("missing syntax at if", line);
                    case "elseif":
                        throw new PreScriptException("missing syntax at elseif", line);
                    case "for":
                        throw new PreScriptException("missing syntax at for", line);             
                    case "break":
                    {
                        if(loopJumps.isEmpty())
                        {
                            throw new PreScriptException("break without a loop", line);
                        }
                        JumpData jump = new JumpData(code.size() - 1);
                        breakContinueJumps.add(jump);
                        addCodeInstruction("break", new InputProvider[] {jump});
                        return;
                    }
                    case "continue":
                    {
                        if(loopJumps.isEmpty())
                        {
                            throw new PreScriptException("continue without a loop", line);
                        }
                        JumpData jump = new JumpData(code.size());
                        breakContinueJumps.add(jump);
                        addCodeInstruction("continue", new InputProvider[] {jump});
                        return;
                    }
                }
                return;
            }
        }
        else
        {
            switch(parts[0])
            {           
                case "++":
                    addCodeInstruction("p+", compileFunction(new String[] {parts[1]}, false));
                    return;
                case "--":
                    addCodeInstruction("p-", compileFunction(new String[] {parts[1]}, false));
                    return;
            }
            switch(parts[1])
            {           
                case "++":
                case "--":
                    input = parts[1];
                    parts = new String[] {parts[0]};
                    break;
                case "=":
                case "+=":
                case "-=":
                case "*=":
                case "/=":
                case "%=":
                case "<<=":
                case ">>=":
                case "&=":
                case "^=":
                case "|=":
                {
                    input = parts[1];
                    parts[1] = ",";
                    break;
                }
                default:
                    throw new PreScriptException("unknown operation " + parts[1], line);
            }
        }
        switch(input)
        {
            case "break":
                throw new PreScriptException("break does not accept arguments", line);
            case "continue":
                throw new PreScriptException("continue does not accept arguments", line);      
        }
        //System.out.println(input + "  " + String.join("__", parts));
        
        switch(input)
        {
            case "elseif":
                createIf("elseif", parts);
                break;
            case "if":
                createIf("if", parts);
                break;
            case "for":
                createFor(parts);
                break;
            case "while":
                createWhile(parts);
                break;
            default:
                addCodeInstruction(input, compileFunction(parts, false));
        }
    }
    
    private void addSyntax(LinkedList<InputProvider> list, Syntax sy)
    {
        int pars = sy.getParameters();
        if(pars > list.size())
        {
            throw new PreScriptException("missing syntax argument", line);
        }
        if(sy == Syntax.UNARY_SUB)
        {
            list.add(new SignInverter(list.pollLast()));
            return;
        }
        InputProvider[] input = new InputProvider[pars];
        for(int j = input.length - 1; j >= 0; j--)
        {
            input[j] = list.pollLast();
        }
        list.add(new Function(FunctionLoader.getFunction(sy.getFunction()), input));
    }
    
    private void validateStackCounter(int stackCounter)
    {
        if(stackCounter < 0)
        {
            throw new PreScriptException("missing syntax argument", line);
        }
    }
    
    private InputProvider[] compileFunction(String[] parts, boolean first)
    {
        LinkedList<InputProvider> list = new LinkedList<>();
        int stackCounter = 0;
        
        Stack<Syntax> syntax = new Stack<>();
        int bottom = first ? 1 : 0;
        Syntax sy;
        for(int i = bottom; i < parts.length; i++)
        {
            if(parts[i].equals(","))
            {
                // finding a comma means pushing all syntax functions
                while(!syntax.isEmpty())
                {
                    addSyntax(list, syntax.pop());
                }
                stackCounter = 0;
                continue;
            }
            sy = Syntax.getSyntax(parts[i]);
            if(sy != Syntax.UNKNOWN)
            {
                if(stackCounter <= 0)
                {
                    switch(sy)
                    {
                        case INVERT:
                            break;
                        case BIT_INVERT:
                            break;
                        case SUB:
                            sy = Syntax.UNARY_SUB;
                            break;
                        case POST_INC:
                            sy = Syntax.INC;
                            break;
                        case POST_DEC:
                            sy = Syntax.DEC;
                            break;
                        default:
                            throw new PreScriptException("missing syntax argument", line);
                    }
                }
                else
                {
                    switch(sy)
                    {
                        case INVERT:
                        case BIT_INVERT:
                            throw new PreScriptException("missing syntax argument", line);
                    }
                }
                // pushing weaker functions
                int weight = sy.getWeight();
                while(!syntax.isEmpty() && syntax.peek().getWeight() <= weight)
                {
                    addSyntax(list, syntax.pop());
                }
                validateStackCounter(stackCounter);
                syntax.add(sy);
                stackCounter -= sy.getParameters() - 1;
                continue;
            }
            stackCounter++;
            list.add(convertString(parts[i]));
        }
        // pushing left over syntax functions because no comma happened
        while(!syntax.isEmpty())
        {
            addSyntax(list, syntax.pop());
        }
        validateStackCounter(stackCounter);
        return list.toArray(new InputProvider[list.size()]);
    }
    
    private InputProvider convertString(String input)
    {
        if(input.startsWith("@"))
        {
            return new ConstantString(input.substring(1));
        }
        else if(input.startsWith("\"") && input.endsWith("\""))
        {
            return new ConstantString(input.substring(1, input.length() - 1));
        }
        else if(input.equals("true"))
        {
            return ConstantBoolean.TRUE;
        }
        else if(input.equals("false"))
        {
            return ConstantBoolean.FALSE;
        }
        else if(input.equals("null"))
        {
            return ConstantNull.NULL;
        }
        else if(SnuviUtils.isNumber(input))
        {
            return new ConstantDouble(Double.parseDouble(input));
        }
        else if(SnuviUtils.isFunction(input))
        {
            int bPos = input.indexOf('(');
            String[] parts = SnuviUtils.split(strings, input.substring(bPos + 1, input.length() - 1), line);
            if(parts.length > 0)
            {
                return new Function(FunctionLoader.getFunction(input.substring(0, bPos)), compileFunction(parts, false));
            }
            else
            {
                return new Function(FunctionLoader.getFunction(input.substring(0, bPos)), new InputProvider[0]);
            }
        }
        else if(SnuviUtils.isArray(input))
        {
            int bPos = input.indexOf('[');
            String[] parts = SnuviUtils.split(strings, input.substring(bPos + 1, input.length() - 1), line);
            if(parts.length > 0)
            {
                return createArray(input.substring(0, bPos), compileFunction(parts, false));
            }
            else
            {
                return createArray(input.substring(0, bPos), new InputProvider[0]);
            }
        }
        else
        {
            return getOrCreateVariable(input);
        }
    }
    
    public static Object convert(String input)
    {
        if(input == null)
        {
            return null;
        }
        input = input.trim();
        if(input.equals("true"))
        {
            return true;
        }
        else if(input.equals("false"))
        {
            return false;
        }
        else if(input.equals("null"))
        {
            return null;
        }
        else if(input.startsWith("\"") && input.endsWith("\""))
        {
            if(input.length() == 1)
            {
                return "\"";
            }
            return input.substring(1, input.length() - 1);
        }
        try
        {
            return Double.parseDouble(input);
        }
        catch(NumberFormatException ex)
        {
            return input;
        }
    }
    
    private Variable getOrCreateVariable(String var)
    {
        if(currentFunction != null && var.charAt(0) != '$')
        {
            Variable oldVar = localVars.get(var);
            if(oldVar == null)
            {
                oldVar = new LocalVariable(var);
                localVars.put(var, oldVar);
            }
            return oldVar;
        }
        else
        {
            if(var.charAt(0) == '$')
            {
                var = var.substring(1);
            }                     
            Variable oldVar = vars.get(var);
            if(oldVar == null)
            {
                oldVar = new Variable(var);
                vars.put(var, oldVar);
            }
            return oldVar;
        }
    }
    
    private DynamicArray createArray(String var, InputProvider[] in)
    {
        if(currentFunction != null)
        {
            Variable oldVar = localVars.get(var);
            if(oldVar == null)
            {
                oldVar = new LocalArrayVariable(var);
                localVars.put(var, oldVar);
            }
            return new DynamicArray(oldVar, in);    
        }
        else
        {
            Variable oldVar = vars.get(var);
            if(oldVar == null)
            {
                oldVar = new ArrayVariable(var);
                vars.put(var, oldVar);
            }
            return new DynamicArray(oldVar, in);
        }
    }
    
    private void createIf(String name, String[] parts)
    {
        InputProvider[] input = compileFunction(parts, false);
        InputProvider[] realInput = new InputProvider[input.length + 1];

        System.arraycopy(input, 0, realInput, 0, input.length);
        JumpData jump = new JumpData(code.size());
        realInput[input.length] = jump;
        jumps.push(new JumpWrapper(jump, name));
        
        addCodeInstruction(name, realInput);
    }
    
    private void createFor(String[] parts)
    {
        // expected syntax
        // for(var, start, end, step)
        // for(var, start, end)
        InputProvider[] input = compileFunction(parts, false);
        if(input.length != 3 && input.length != 4)
        {
            throw new PreScriptException("missing 'for' syntax at", line);
        }
        InputProvider[] realInput = new InputProvider[5];

        System.arraycopy(input, 0, realInput, 0, input.length);
        
        if(input.length == 3)
        {
            realInput[3] = new ConstantDouble(1.0);
        }
        
        JumpData jump = new JumpData(code.size());
        realInput[4] = jump;
        JumpWrapper wrapper = new JumpWrapper(jump, "for");
        jumps.push(wrapper);
        loopJumps.push(wrapper);
        
        addCodeInstruction("for", realInput);
    }
    
    private void createWhile(String[] parts)
    {
        // expected syntax
        // while(condition)
        InputProvider[] input = compileFunction(parts, false);
        if(input.length != 1)
        {
            throw new PreScriptException("invalid conditions at 'while'", line);
        }
        InputProvider[] realInput = new InputProvider[2];
        realInput[0] = input[0];
        
        JumpData jump = new JumpData(code.size());
        realInput[1] = jump;
        
        JumpWrapper wrapper = new JumpWrapper(jump, "while");
        jumps.push(wrapper);
        loopJumps.push(wrapper);

        addCodeInstruction("while", realInput);
    }
    
    private void createBreakContinue(int current)
    {
        breakContinueJumps.forEach(jump -> jump.setRelativeJump(current));
        breakContinueJumps.clear();
    }
}