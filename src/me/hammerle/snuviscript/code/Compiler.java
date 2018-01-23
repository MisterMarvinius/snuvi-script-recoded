package me.hammerle.snuviscript.code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import me.hammerle.snuviscript.constants.ConstantFraction;
import me.hammerle.snuviscript.constants.ConstantNull;
import me.hammerle.snuviscript.constants.ConstantString;
import me.hammerle.snuviscript.array.DynamicArray;
import me.hammerle.snuviscript.constants.ConstantBoolean;
import me.hammerle.snuviscript.variable.ArrayVariable;
import me.hammerle.snuviscript.variable.LocalArrayVariable;
import me.hammerle.snuviscript.variable.LocalVariable;
import me.hammerle.snuviscript.variable.Variable;
import me.hammerle.snuviscript.exceptions.PreScriptException;
import me.hammerle.snuviscript.math.Fraction;

public class Compiler 
{
    public static Instruction[] compile(Script sc, List<String> sCode, HashMap<String, Integer> labels, boolean locale, int lineOffset)
    {
        Compiler compiler = new Compiler(sc, sCode, labels, locale);
        compiler.lineOffset = lineOffset;
        Instruction[] instructions = compiler.compile();
        sc.vars = compiler.vars;
        return instructions;
    }
    
    private final List<String> sCode;
    private final HashMap<String, Variable> vars;
    private final HashMap<String, Variable> localVars;
    private final HashMap<String, Integer> labels;
    
    private final LinkedList<Instruction> code;
    private int line;
    private int lineOffset;
    private int layer;
    
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
    
    private final Stack<JumpWrapper> jumps;
    private final Stack<JumpWrapper> loopJumps;
    private final LinkedList<JumpData> breakContinueJumps;
    private final Script parentScript;
    
    private final HashMap<String, String> strings;
    private int stringCounter;
    
    private final boolean locale;
    
    private Compiler(Script sc, List<String> sCode, HashMap<String, Integer> labels, boolean locale)
    {
        this.parentScript = sc;
        this.sCode = sCode;
        this.vars = new HashMap<>();
        this.localVars = new HashMap<>();
        this.labels = labels;
        
        this.code = new LinkedList<>();
        this.line = 0;
        this.layer = 0;
        this.jumps = new Stack<>();
        this.loopJumps = new Stack<>();
        this.breakContinueJumps = new LinkedList<>();
        this.strings = new HashMap<>();
        this.stringCounter = 0;
        this.locale = locale;
    }
    
    private void addCodeInstruction(String function, InputProvider[] input)
    {
        code.add(new Instruction(line + lineOffset, (byte) layer, FunctionLoader.getFunction(function), input));
    }
    
    private Instruction[] compile()
    {
        int size = sCode.size();
        System.out.println("__________________________________");
        
        StringBuilder sb = new StringBuilder();
        String replacement;
        String check;
        int pos;
        int old = 0;
        boolean text = false;
        boolean comment = false;
        
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
                            case "if":
                            {
                                data.data.setRelativeJump(code.size());
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
                            if(parentScript.subScript)
                            {
                                throw new PreScriptException("function not allowed in another function", line);
                            }
                            int index = check.indexOf("(");
                            if(index == -1)
                            {
                                throw new PreScriptException("missing function syntax", line);
                            }
                            String function = check.substring(9, index);
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
                            ArrayList<String> subList = new ArrayList<>();
                            int bCounter = 1;
                            String subLine = check;
                            pos++;
                            
                            int oldLine = line;
                            out: while(true)
                            {
                                old = pos;
                                while(pos < subLine.length())
                                {
                                    switch(subLine.charAt(pos))
                                    {
                                        case '"':
                                            text = !text;
                                            break;
                                        case '{':
                                            if(!text)
                                            {
                                                bCounter++;
                                            }
                                            break;
                                        case '}':
                                            if(!text)
                                            {
                                                bCounter--;
                                                if(bCounter == 0)
                                                {
                                                    subList.add(subLine.substring(old, pos).trim());
                                                    sb = new StringBuilder();
                                                    sCode.set(line, subLine.substring(pos + 1));
                                                    line--;
                                                    break out;
                                                }
                                            }
                                            break;
                                    }
                                    pos++;
                                }
                                subList.add(subLine.substring(old, pos).trim());
                                line++;
                                if(line >= sCode.size())
                                {
                                    throw new PreScriptException("{ without }", line);
                                }
                                pos = 0;
                                subLine = sCode.get(line);
                            }
                            
                            Script sub = new Script(subList, inputs, parentScript, oldLine);
                            parentScript.subScripts.put(function, sub);
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
        }
        
        System.out.println("__________________________________");
        
        Instruction[] input = code.toArray(new Instruction[code.size()]);
        
        for(Instruction in : input)
        {
            System.out.println(in);
        }
        System.out.println("__________________________________");
        labels.entrySet().stream().forEach((e) -> 
        {
            System.out.println("LABEL " + e.getKey() + " " + e.getValue());
        });
        System.out.println("__________________________________");
        return input;
    }
    
    private void compileLine(String currentCode)
    {
        if(currentCode.startsWith("'"))
        {
            return;
        }
        //System.out.println(">>>"  + currentCode);
        String[] parts = Utils.split(strings, currentCode, line);
        //System.out.println(">>> " + String.join("_", parts));
        
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
            labels.put(parts[0].substring(1), code.size() - 1);
            return;
        }
        
        String input;
        if(parts.length == 1)
        {
            int bPos = parts[0].indexOf('(');
            if(bPos != -1)
            {
                input = parts[0].substring(0, bPos);
                parts = Utils.split(strings, parts[0].substring(bPos + 1, parts[0].length() - 1), line);
            }
            else
            {
                switch(parts[0])
                {
                    case "while":
                        throw new PreScriptException("missing syntax at while", line);
                    case "if":
                        throw new PreScriptException("missing syntax at if", line);
                    case "for":
                        throw new PreScriptException("missing syntax at for", line);             
                    case "break":
                    {
                        if(loopJumps.isEmpty())
                        {
                            throw new IllegalStateException("break without a loop");
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
                            throw new IllegalStateException("continue without a loop");
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
                    //System.out.println(String.join("__", parts));
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
            case "if":
                createIf(parts);
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
            if(sy != Syntax.UNKNOWN && sy != Syntax.MAYBE)
            {
                if(stackCounter <= 0)
                {
                    switch(sy)
                    {
                        case SUB:
                            sy = Syntax.UNARY_SUB;
                            break;
                        case INC:
                            sy = Syntax.POST_INC;
                            break;
                        case DEC:
                            sy = Syntax.POST_DEC;
                            break;
                        default:
                            throw new PreScriptException("missing syntax argument", line);
                    }
                    System.out.println(syntax);
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
        else if(Utils.isNumber(input))
        {
            return new ConstantFraction(Fraction.fromDouble(Double.parseDouble(input)));
        }
        else if(Utils.isFunction(input))
        {
            int bPos = input.indexOf('(');
            String[] parts = Utils.split(strings, input.substring(bPos + 1, input.length() - 1), line);
            if(parts.length > 0)
            {
                return new Function(FunctionLoader.getFunction(input.substring(0, bPos)), compileFunction(parts, false));
            }
            else
            {
                return new Function(FunctionLoader.getFunction(input.substring(0, bPos)), new InputProvider[0]);
            }
        }
        else if(Utils.isArray(input))
        {
            int bPos = input.indexOf('[');
            String[] parts = Utils.split(strings, input.substring(bPos + 1, input.length() - 1), line);
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
        else if(Utils.isNumber(input))
        {
            return Fraction.fromDouble(Double.parseDouble(input));
        }
        return input;
    }
    
    private Variable getOrCreateVariable(String var)
    {
        if(locale)
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
        if(locale)
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
    
    private void createIf(String[] parts)
    {
        InputProvider[] input = compileFunction(parts, false);
        InputProvider[] realInput = new InputProvider[input.length + 1];

        System.arraycopy(input, 0, realInput, 0, input.length);
        JumpData jump = new JumpData(code.size());
        realInput[input.length] = jump;
        jumps.push(new JumpWrapper(jump, "if"));
        
        addCodeInstruction("if", realInput);
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
            realInput[3] = new ConstantFraction(new Fraction(1));
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
// 811