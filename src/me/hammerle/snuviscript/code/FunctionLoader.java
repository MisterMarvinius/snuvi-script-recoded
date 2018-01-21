package me.hammerle.snuviscript.code;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.stream.Collectors;
import me.hammerle.snuviscript.array.DynamicArray;
import me.hammerle.snuviscript.variable.ArrayVariable;
import me.hammerle.snuviscript.variable.Variable;
import me.hammerle.snuviscript.math.Fraction;

public class FunctionLoader 
{
    private static final HashMap<String, BasicFunction> FUNCTIONS = new HashMap<>();
    
    public static void registerFunction(String name, BasicFunction function)
    {
        FUNCTIONS.put(name, function);
    }
    
    public static void registerFunction(BasicFunction function)
    {
        registerFunction(function.getName(), function);
    }
    
    public static void registerAlias(String original, String alias)
    {
        FUNCTIONS.put(alias, FUNCTIONS.get(original));
    }
    
    public static BasicFunction getFunction(String function)
    {
        return FUNCTIONS.getOrDefault(function, new BasicFunction(function, (sc, in) -> 
        {
            Script sub = sc.subScripts.get(function);
            if(sub == null)
            {
                throw new NullPointerException("function " + function + " does not exist");
            }
            // push storage for local vars
            HashMap<String, Variable> vars = new HashMap<>();
            if(in.length != sub.subScriptInput.length)
            {
                throw new NullPointerException("invalid number of input for function " + function);
            }
            // generate local vars
            String s;
            Variable v;
            for(int i = 0; i < in.length; i++)
            {
                s = sub.subScriptInput[i];
                if(in[i].isArray(sc))
                {
                    v = new ArrayVariable(s);
                    v.set(sc, in[i].getArray(sc));
                }
                else
                {
                    v = new Variable(s);
                    v.set(sc, in[i].get(sc));
                }
                vars.put(s, v);
            }
            
            sub.localVars.push(vars);
            // saving line for return
            int line = sub.currentLine;
            // set starting line for current run
            sub.currentLine = 0;
            // run subscript and save return value
            Object r = sub.run();
            // return back to previous line
            sub.currentLine = line;
            // pop storage for local vars
            sub.localVars.pop();
            return r;
        }));
    }
    
    private static final Random[] RND;
    
    static
    {
        RND = new Random[8];
        for(int i = 0; i < 8; i++)
        {
            RND[i] = new Random();
        }
        // ---------------------------------------------------------------------
        // brackets
        // ---------------------------------------------------------------------
        registerFunction("", new BasicFunction("", (sc, in) -> 
        {
            return in[0].get(sc);
        }));
        // ---------------------------------------------------------------------
        // elementary arithmetic
        // ---------------------------------------------------------------------
        registerFunction("+", new BasicFunction("ADD", (sc, in) -> 
        {
            return in[0].getFraction(sc).add(in[1].getFraction(sc));
        }));
        registerFunction("-", new BasicFunction("SUB", (sc, in) -> 
        {
            return in[0].getFraction(sc).sub(in[1].getFraction(sc));
        }));
        registerFunction("*", new BasicFunction("MUL", (sc, in) -> 
        {
            return in[0].getFraction(sc).mul(in[1].getFraction(sc));
        }));
        registerFunction("/", new BasicFunction("DIV", (sc, in) -> 
        {
            return in[0].getFraction(sc).div(in[1].getFraction(sc));
        }));
        // ---------------------------------------------------------------------
        // comparing
        // ---------------------------------------------------------------------
        registerFunction("==", new BasicFunction("EQUAL", (sc, in) -> 
        {
            Object a = in[0].get(sc);
            Object b = in[1].get(sc);
            if(a == null || b == null)
            {
                return a == b ? 1 : 0;
            }
            else if(a instanceof String || b instanceof String)
            {
                return a.equals(b) ? 1 : 0;
            }
            return ((Number) a).doubleValue() == ((Number) b).doubleValue() ? 1 : 0;
        }));
        registerFunction("!=", new BasicFunction("NOTEQUAL", (sc, in) -> 
        {
            Object a = in[0].get(sc);
            Object b = in[1].get(sc);
            if(a == null || b == null)
            {
                return a != b ? 1 : 0;
            }
            else if(a instanceof String || b instanceof String)
            {
                return a.equals(b) ? 1 : 0;
            }
            return ((Number) a).doubleValue() != ((Number) b).doubleValue() ? 1 : 0;
        }));
        registerFunction(">", new BasicFunction("GREATER", (sc, in) -> 
        {
            return in[0].getDouble(sc) > in[1].getDouble(sc) ? 1 : 0;
        }));
        registerFunction(">=", new BasicFunction("GREATEREQUAL", (sc, in) -> 
        {
            return in[0].getDouble(sc) >= in[1].getDouble(sc) ? 1 : 0;
        }));
        registerFunction("<", new BasicFunction("SMALLER", (sc, in) -> 
        {
            return in[0].getDouble(sc) < in[1].getDouble(sc) ? 1 : 0;
        }));
        registerFunction("<=", new BasicFunction("SMALLEREQUAL", (sc, in) -> 
        {
            return in[0].getDouble(sc) <= in[1].getDouble(sc) ? 1 : 0;
        }));
        // ---------------------------------------------------------------------
        // logical operators
        // ---------------------------------------------------------------------
        registerFunction("&&", new BasicFunction("AND", (sc, in) -> 
        {
            return (in[0].getDouble(sc) != 0 && in[1].getDouble(sc) != 0) ? 1 : 0;
        }));
        registerFunction("||", new BasicFunction("OR", (sc, in) -> 
        {
            return (in[0].getDouble(sc) != 0 || in[1].getDouble(sc) != 0) ? 1 : 0;
        }));
        // ---------------------------------------------------------------------
        // bit stuff
        // ---------------------------------------------------------------------
        registerFunction(new BasicFunction("MOD", (sc, in) -> 
        {
            return in[0].getInt(sc) % in[1].getInt(sc);
        }));
        registerFunction("&", new BasicFunction("AND", (sc, in) -> 
        {
            return in[0].getInt(sc) & in[1].getInt(sc);
        }));
        registerFunction("|", new BasicFunction("OR", (sc, in) -> 
        {
            return in[0].getInt(sc) | in[1].getInt(sc);
        }));
        registerFunction("^", new BasicFunction("XOR", (sc, in) -> 
        {
            return in[0].getInt(sc) ^ in[1].getInt(sc);
        }));
        registerFunction("<<", new BasicFunction("SHIFTL", (sc, in) -> 
        {
            return in[0].getInt(sc) << in[1].getInt(sc);
        }));
        registerFunction(">>", new BasicFunction("SHIFTR", (sc, in) -> 
        {
            return in[0].getInt(sc) >> in[1].getInt(sc);
        }));
        // ---------------------------------------------------------------------
        // basic instructions (variables and arrays)
        // ---------------------------------------------------------------------
        registerFunction("=", new BasicFunction("SET", (sc, in) -> 
        {
            in[0].set(sc, in[1].get(sc));
            return Void.TYPE;
        }));
        registerFunction("+=", new BasicFunction("ADD_SET", (sc, in) -> 
        {
            in[0].set(sc, in[0].getFraction(sc).add(in[1].getFraction(sc)));
            return Void.TYPE;
        }));
        registerFunction("-=", new BasicFunction("SUB_SET", (sc, in) -> 
        {
            in[0].set(sc, in[0].getFraction(sc).sub(in[1].getFraction(sc)));
            return Void.TYPE;
        }));
        registerFunction("*=", new BasicFunction("MUL_SET", (sc, in) -> 
        {
            in[0].set(sc, in[0].getFraction(sc).mul(in[1].getFraction(sc)));
            return Void.TYPE;
        }));
        registerFunction("/=", new BasicFunction("DIV_SET", (sc, in) -> 
        {
            in[0].set(sc, in[0].getFraction(sc).div(in[1].getFraction(sc)));
            return Void.TYPE;
        }));
        registerFunction("%=", new BasicFunction("MOD_SET", (sc, in) -> 
        {
            in[0].set(sc, new Fraction(in[0].getInt(sc) % in[1].getInt(sc)));
            return Void.TYPE;
        }));
        registerFunction("<<=", new BasicFunction("LEFT_SHIFT_SET", (sc, in) -> 
        {
            in[0].set(sc, in[0].getFraction(sc).leftShift(in[1].getInt(sc)));
            return Void.TYPE;
        }));
        registerFunction(">>=", new BasicFunction("RIGHT_SHIFT_SET", (sc, in) -> 
        {
            in[0].set(sc, in[0].getFraction(sc).rightShift(in[1].getInt(sc)));
            return Void.TYPE;
        }));
        registerFunction("&=", new BasicFunction("BIT_AND_SET", (sc, in) -> 
        {
            in[0].set(sc, in[0].getFraction(sc).and(in[1].getFraction(sc)));
            return Void.TYPE;
        }));
        registerFunction("^=", new BasicFunction("BIT_XOR_SET", (sc, in) -> 
        {
            in[0].set(sc, in[0].getFraction(sc).xor(in[1].getFraction(sc)));
            return Void.TYPE;
        }));
        registerFunction("|=", new BasicFunction("BIT_OR_SET", (sc, in) -> 
        {
            in[0].set(sc, in[0].getFraction(sc).or(in[1].getFraction(sc)));
            return Void.TYPE;
        }));
        registerFunction(new BasicFunction("DIM", (sc, in) -> 
        {
            for(InputProvider input : in)
            {
                ((DynamicArray) input).init(sc);
            }
            return Void.TYPE;
        }));
        registerAlias("DIM", "VAR");
        registerFunction(new BasicFunction("SWAP", (sc, in) -> 
        {
            Object o = in[0].get(sc);
            in[0].set(sc, in[1].get(sc));
            in[1].set(sc, o);
            return Void.TYPE;
        }));
        registerFunction(new BasicFunction("INC", (sc, in) -> 
        {
            in[0].set(sc, in[0].getInt(sc) + (in.length > 1 ? in[1].getInt(sc) : 1));
            return Void.TYPE;
        }));
        registerFunction(new BasicFunction("DEC", (sc, in) -> 
        {
            in[0].set(sc, in[0].getInt(sc) - (in.length > 1 ? in[1].getInt(sc) : 1));
            return Void.TYPE;
        }));
        // ---------------------------------------------------------------------
        // basic instructions (control and branching)
        // ---------------------------------------------------------------------
        registerFunction(new BasicFunction("goto", (sc, in) -> 
        {
            sc.currentLine = sc.labels.get(in[0].getString(sc));
            return Void.TYPE;
        }));
        registerFunction(new BasicFunction("GOSUB", (sc, in) -> 
        {
            sc.returnStack.push(sc.currentLine);
            sc.currentLine = sc.labels.get(in[0].getString(sc));
            return Void.TYPE;
        }));
        registerFunction(new BasicFunction("return", (sc, in) -> 
        {
            if(sc.returnStack.isEmpty())
            {
                sc.end();
                sc.returnValue = in.length > 0 ? in[0].get(sc) : null;
            }
            else
            {
                sc.currentLine = sc.returnStack.pop();
            }
            return Void.TYPE;
        }));
        registerFunction(new BasicFunction("if", (sc, in) -> 
        {
            int p = in[0].getInt(sc);
            if(p == 0)
            {
                sc.currentLine += in[1].getInt(sc);
            }
            return Void.TYPE;
        }));
        registerFunction(new BasicFunction("endif", (sc, in) -> 
        {
            return Void.TYPE;
        }));
        registerFunction(new BasicFunction("for", (sc, in) -> 
        {
            // for(var, start, end, step)
            Fraction start = in[1].getFraction(sc);
            in[0].set(sc, start);           
            if(start.compareTo(in[2].getFraction(sc)) > 0)
            {
                sc.currentLine += in[4].getInt(sc);
            }
            return Void.TYPE;
        }));
        registerFunction(new BasicFunction("next", (sc, in) -> 
        {
            int line = sc.currentLine + in[0].getInt(sc);
            InputProvider[] f = sc.code[line].getParameters();
            // for(var, start, end, step)
            Fraction current = f[0].getFraction(sc).add(f[3].getFraction(sc));
            f[0].set(sc, current);
            if(current.compareTo(f[2].getFraction(sc)) <= 0)
            {
                sc.currentLine = line;
            }
            return Void.TYPE;
        }));
        registerFunction(new BasicFunction("while", (sc, in) -> 
        {
            if(in[0].getInt(sc) == 0)
            {
                sc.currentLine += in[1].getInt(sc);
            }
            return Void.TYPE;
        }));
        registerFunction(new BasicFunction("wend", (sc, in) -> 
        {
            sc.currentLine += in[0].getInt(sc);
            return Void.TYPE;
        }));
        registerFunction(new BasicFunction("continue", (sc, in) -> 
        {
            sc.currentLine += in[0].getInt(sc);
            return Void.TYPE;
        }));
        registerFunction(new BasicFunction("break", (sc, in) -> 
        {
            sc.currentLine += in[0].getInt(sc);
            return Void.TYPE;
        }));
        // ---------------------------------------------------------------------
        // mathematics
        // ---------------------------------------------------------------------
        registerFunction(new BasicFunction("FLOOR", (sc, in) -> 
        {
            return (int) Math.floor(in[0].getDouble(sc));
        }));
        registerFunction(new BasicFunction("ROUND", (sc, in) -> 
        {
            return (int) Math.round(in[0].getDouble(sc));
        }));
        registerFunction(new BasicFunction("CEIL", (sc, in) -> 
        {
            return (int) Math.ceil(in[0].getDouble(sc));
        }));
        registerFunction(new BasicFunction("ABS", (sc, in) -> 
        {
            return in[0].getFraction(sc).abs();
        }));
        registerFunction(new BasicFunction("SGN", (sc, in) -> 
        {
            double d = in[0].getDouble(sc);
            return d < 0 ? -1 : (d > 0 ? 1 : 0);
        }));
        /*registerFunction(new BasicFunction("MIN", (sc, in) -> 
        {
            if(in.length == 1)
            {
                return ((IMathOperation) in[0]).min(sc);
            }
            double min = Arrays.stream(in).mapToDouble(i -> i.getDouble(sc)).min().getAsDouble();
            if(min == (int) min)
            {
                return (int) min;
            }
            return min;
        }));
        registerFunction(new BasicFunction("MAX", (sc, in) -> 
        {
            if(in.length == 1)
            {
                return ((IMathOperation) in[0]).max(sc);
            }
            double max = Arrays.stream(in).mapToDouble(i -> i.getDouble(sc)).max().getAsDouble();
            if(max == (int) max)
            {
                return (int) max;
            }
            return max;
        }));*/
        registerFunction(new BasicFunction("RND", (sc, in) -> 
        {
            int seedId;
            int max;
            switch (in.length) 
            {
                case 1:
                    seedId = 0;
                    max = in[0].getInt(sc);
                    break;
                case 2:
                    seedId = in[0].getInt(sc);
                    max = in[1].getInt(sc);
                    break;
                default:
                    throw new IllegalArgumentException("invalid number of arguments");
            }
            if(seedId < 0 || seedId > 7)
            {
                throw new IllegalArgumentException("seed id must be from 0 to 7");
            }
            return RND[seedId].nextInt(max);
        }));
        registerFunction(new BasicFunction("RNDF", (sc, in) -> 
        {
            int seedId = 0;
            if(in.length > 0)
            {
                seedId = in[0].getInt(sc);
            }
            if(seedId < 0 || seedId > 7)
            {
                throw new IllegalArgumentException("seed id must be from 0 to 7");
            }
            return RND[seedId].nextDouble();
        }));
        registerFunction(new BasicFunction("RANDOMIZE", (sc, in) -> 
        {
            int seedId = in[0].getInt(sc);
            if(seedId < 0 || seedId > 7)
            {
                throw new IllegalArgumentException("seed id must be from 0 to 7");
            }
            switch (in.length) 
            {
                case 1:
                    RND[seedId] = new Random();
                    break;
                case 2:
                    RND[seedId] = new Random(in[1].getInt(sc));
                    break;
                default:
                    throw new IllegalArgumentException("invalid number of arguments");
            }
            return Void.TYPE;
        }));
        registerFunction(new BasicFunction("SQR", (sc, in) -> 
        {
            return Math.sqrt(in[0].getDouble(sc));
        }));
        registerFunction(new BasicFunction("EXP", (sc, in) -> 
        {
            if(in.length == 0)
            {
                return Math.E;
            }
            return Math.exp(in[0].getDouble(sc));
        }));
        registerFunction(new BasicFunction("LOG", (sc, in) -> 
        {
            if(in.length >= 2)
            {
                return Math.log(in[0].getDouble(sc)) / Math.log(in[1].getDouble(sc));
            }
            return Math.log(in[0].getDouble(sc));
        }));
        registerFunction(new BasicFunction("POW", (sc, in) -> 
        {
            return Math.pow(in[0].getDouble(sc), in[1].getDouble(sc));
        }));
        registerFunction(new BasicFunction("PI", (sc, in) -> 
        {
            return Math.PI;
        }));
        registerFunction(new BasicFunction("RAD", (sc, in) -> 
        {
            return Math.toRadians(in[0].getDouble(sc));
        }));
        registerFunction(new BasicFunction("DEG", (sc, in) -> 
        {
            return Math.toDegrees(in[0].getDouble(sc));
        }));
        registerFunction(new BasicFunction("SIN", (sc, in) -> 
        {
            return Math.sin(in[0].getDouble(sc));
        }));
        registerFunction(new BasicFunction("COS", (sc, in) -> 
        {
            return Math.cos(in[0].getDouble(sc));
        }));
        registerFunction(new BasicFunction("TAN", (sc, in) -> 
        {
            return Math.tan(in[0].getDouble(sc));
        }));
        registerFunction(new BasicFunction("ASIN", (sc, in) -> 
        {
            return Math.asin(in[0].getDouble(sc));
        }));
        registerFunction(new BasicFunction("ACOS", (sc, in) -> 
        {
            return Math.acos(in[0].getDouble(sc));
        }));
        registerFunction(new BasicFunction("ATAN", (sc, in) -> 
        {
            if(in.length >= 2)
            {
                return Math.atan2(in[0].getDouble(sc), in[1].getDouble(sc));
            }
            return Math.atan(in[0].getDouble(sc));
        }));
        registerFunction(new BasicFunction("SINH", (sc, in) -> 
        {
            return Math.sinh(in[0].getDouble(sc));
        }));
        registerFunction(new BasicFunction("COSH", (sc, in) -> 
        {
            return Math.cosh(in[0].getDouble(sc));
        }));
        registerFunction(new BasicFunction("TANH", (sc, in) -> 
        {
            return Math.tanh(in[0].getDouble(sc));
        }));
        registerFunction(new BasicFunction("CLASSIFY", (sc, in) -> 
        {
            double d = in[0].getDouble(sc);
            if(Double.isNaN(d))
            {
                return 2;
            }
            else if(Double.isInfinite(d))
            {
                return 1;
            }
            return 0;
        }));
        
        
        registerFunction(new BasicFunction("print", (sc, in) -> 
        {
            printMessage(Arrays.stream(in).map(s -> s.getString(sc)).collect(Collectors.joining()));
            return Void.TYPE;
        }));
        registerFunction(new BasicFunction("TEST", (sc, in) -> 
        {
            return 1;
        }));
    }
    
    private static void printMessage(String message)
    {
        System.out.println(message);
    }
}
