package me.hammerle.snuviscript.code;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import me.hammerle.snuviscript.array.DynamicArray;
import me.hammerle.snuviscript.config.SnuviConfig;
import me.hammerle.snuviscript.exceptions.AssertionException;
import me.hammerle.snuviscript.exceptions.FileIOException;
import me.hammerle.snuviscript.variable.ArrayVariable;
import me.hammerle.snuviscript.variable.Variable;
import me.hammerle.snuviscript.math.Fraction;

public class FunctionLoader 
{
    private static final HashMap<String, BasicFunction> FUNCTIONS = new HashMap<>();
    
    protected static void registerFunction(String name, String fname, BiFunction<Script, InputProvider[], Object> f)
    {
        FUNCTIONS.put(name, new BasicFunction(fname, f));
    }
    
    protected static void registerFunction(String name, BiFunction<Script, InputProvider[], Object> f)
    {
        registerFunction(name, name, f);
    }
    
    protected static void registerAlias(String original, String alias)
    {
        FUNCTIONS.put(alias, FUNCTIONS.get(original));
    }
    
    protected static BasicFunction getFunction(String f)
    {
        final String function = f.toLowerCase();
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
                throw new NullPointerException("invalid number of parameters at function '" + function + "'");
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
    
    static
    {
        // ---------------------------------------------------------------------    
        // system stuff
        // --------------------------------------------------------------------- 
        registerFunction("nothing", (sc, in) -> Void.TYPE);
        registerFunction("error", (sc, in) -> 
        {
            sc.printStackTrace = !sc.printStackTrace;
            return Void.TYPE;
        });
        registerFunction("", (sc, in) -> in[0].get(sc));
        
        // ---------------------------------------------------------------------  
        // event
        // --------------------------------------------------------------------- 
        registerFunction("event.load", (sc, in) ->
        {
            sc.events.add(in[0].getString(sc));
            return Void.TYPE;
        });
        registerFunction("event.unload", (sc, in) ->
        {
            sc.events.remove(in[0].getString(sc));
            return Void.TYPE;
        });
        registerFunction("event.isloaded", (sc, in) -> sc.isEventLoaded(in[0].getString(sc)));

        // ---------------------------------------------------------------------    
        // bit
        // --------------------------------------------------------------------- 
        
        registerFunction(">>", (sc, in) -> in[0].getFraction(sc).rightShift(in[1].getInt(sc)));
        registerFunction("<<", (sc, in) -> in[0].getFraction(sc).leftShift(in[1].getInt(sc)));
        registerFunction("&", (sc, in) -> in[0].getFraction(sc).and(in[1].getFraction(sc)));
        registerFunction("|", (sc, in) -> in[0].getFraction(sc).or(in[1].getFraction(sc)));
        registerFunction("^", (sc, in) -> in[0].getFraction(sc).xor(in[1].getFraction(sc)));
        registerFunction("~", (sc, in) -> in[0].getFraction(sc).invertBits());
        registerFunction("bit.set", (sc, in) -> in[0].getFraction(sc).setBit(in[1].getInt(sc)));
        registerFunction("bit.unset", (sc, in) -> in[0].getFraction(sc).unsetBit(in[1].getInt(sc)));
        registerFunction("bit.get", (sc, in) -> in[0].getFraction(sc).getBit(in[1].getInt(sc)));
        
        // ---------------------------------------------------------------------    
        // math
        // ---------------------------------------------------------------------    
        registerFunction("%", (sc, in) -> new Fraction(in[0].getInt(sc) % in[1].getInt(sc)));
        registerAlias("%", "math.mod");
        registerFunction("math.abs", (sc, in) -> in[0].getFraction(sc).abs());
        registerFunction("math.pow", (sc, in) -> in[0].getFraction(sc).power(in[1].getFraction(sc)));
        registerFunction("math.root", (sc, in) -> in[0].getFraction(sc).power(in[1].getFraction(sc).invert()));
        registerFunction("math.sin", (sc, in) -> in[0].getFraction(sc).sin());
        registerFunction("math.cos", (sc, in) -> in[0].getFraction(sc).cos());
        registerFunction("math.tan", (sc, in) -> in[0].getFraction(sc).tan());
        registerFunction("math.sin", (sc, in) -> in[0].getFraction(sc).sin());
        registerFunction("math.acos", (sc, in) -> in[0].getFraction(sc).acos());
        registerFunction("math.atan", (sc, in) -> in[0].getFraction(sc).atan());
        registerFunction("math.asin", (sc, in) -> in[0].getFraction(sc).asin());
        registerFunction("math.e", (sc, in) -> Fraction.E);
        registerFunction("math.pi", (sc, in) -> Fraction.PI);
        registerFunction("math.ln", (sc, in) -> in[0].getFraction(sc).log());
        registerFunction("math.log", (sc, in) -> in[0].getFraction(sc).log10());
        registerFunction("math.random", (sc, in) -> new Fraction(SnuviUtils.randomInt(in[0].getInt(sc), in[1].getInt(sc))));
        registerFunction("math.round", (sc, in) -> in[0].getFraction(sc).round());
        registerFunction("math.rounddown", (sc, in) -> in[0].getFraction(sc).floor());
        registerFunction("math.roundup", (sc, in) -> in[0].getFraction(sc).ceil());
        registerFunction("math.roundcomma", (sc, in) -> in[0].getFraction(sc).round(in[1].getInt(sc)));
        
        // ---------------------------------------------------------------------  
        // lists
        // ---------------------------------------------------------------------    
        registerFunction("list.new", (sc, in) ->     
        {
            in[0].set(sc, new ArrayList<>());
            return Void.TYPE;
        });
        registerFunction("list.exists", (sc, in) -> in[0].get(sc) instanceof List);
        registerFunction("list.add", (sc, in) -> ((List) in[0].get(sc)).add(in[1].get(sc)));
        registerFunction("list.remove", (sc, in) -> ((List) in[0].get(sc)).remove(in[1].get(sc)));
        registerFunction("list.removeindex", (sc, in) -> ((List) in[0].get(sc)).remove(in[1].getInt(sc)));
        registerFunction("list.contains", (sc, in) -> ((List) in[0].get(sc)).contains(in[1].get(sc)));
        registerFunction("list.getsize", (sc, in) -> new Fraction(((List) in[0].get(sc)).size()));
        registerFunction("list.getindex", (sc, in) -> ((List) in[0].get(sc)).get(in[1].getInt(sc)));
        registerAlias("list.getindex", "list.get");
        registerFunction("list.setindex", (sc, in) -> ((List) in[0].get(sc)).set(in[1].getInt(sc), in[2].get(sc)));
        registerFunction("list.clear", (sc, in) ->     
        {
            ((List) in[0].get(sc)).clear();
            return Void.TYPE;
        });
        registerFunction("list.getindexof", (sc, in) -> new Fraction(((List) in[0].get(sc)).indexOf(in[1].get(sc))));
        registerFunction("list.sort", (sc, in) ->     
        {
            Collections.sort(((List<Object>) in[0].get(sc)), (Object o1, Object o2) -> ((Comparable) o1).compareTo(o2));
            return Void.TYPE;
        });
        registerFunction("list.reverse", (sc, in) ->     
        {
            Collections.reverse((List<Object>) in[0].get(sc)); 
            return Void.TYPE;
        });
        registerFunction("list.shuffle", (sc, in) ->     
        {
            Collections.shuffle((List<Object>) in[0].get(sc)); 
            return Void.TYPE;
        });

        // ---------------------------------------------------------------------  
        // arrays
        // ---------------------------------------------------------------------   
        registerFunction("array.new", (sc, in) -> 
        {
            for(InputProvider input : in)
            {
                ((DynamicArray) input).init(sc);
            }
            return Void.TYPE;
        });
        registerFunction("array.getsize", (sc, in) -> new Fraction(Array.getLength(in[0].getArray(sc))));
        
        /*
        registerFunction("array.swap", (sc, in) ->                                           
                {
                    Object[] o = (Object[]) args[0];
                    int first = ScriptUtils.getInt(args[1]);
                    int sec = ScriptUtils.getInt(args[2]);
                    Object helper = o[first];
                    o[first] = o[sec];
                    o[sec] = helper;
                });
        registerFunction("array.sort", (sc, in) ->  
                {
                    if(args.length <= 1)
                    {
                        Arrays.sort((Object[]) args[0]);
                    }
                    else
                    {
                        Arrays.sort((Object[]) args[0], ScriptUtils.getInt(args[1]), ScriptUtils.getInt(args[2]));
                    }
                });
        registerFunction("array.copy", (sc, in) ->  
                {
                    int first = ScriptUtils.getInt(args[2]);
                    System.arraycopy((Object[]) args[0], first, (Object[]) args[1], 
                            ScriptUtils.getInt(args[4]), ScriptUtils.getInt(args[3]) - first + 1);
                });
        registerFunction("array.rsort", (sc, in) ->    
                {
                    if(args.length <= 1)
                    {
                        Arrays.sort((Object[]) args[0], (Object o, Object o1) -> -((Comparable) o).compareTo(o));
                    }
                    else
                    {
                        Arrays.sort((Object[]) args[0], ScriptUtils.getInt(args[1]), 
                                ScriptUtils.getInt(args[2]), (Object o, Object o1) -> -((Comparable) o).compareTo(o)); 
                    }
                });
        registerFunction("array.fill", (sc, in) ->     
                {
                    if(args.length <= 2)
                    {
                        Arrays.fill((Object[]) args[0], args[1]);
                    }
                    else
                    {
                        Arrays.fill((Object[]) args[0], ScriptUtils.getInt(args[2]), ScriptUtils.getInt(args[3]), args[1]); 
                    }
                });*/  

        // --------------------------------------------------------------------- 
        // maps
        // --------------------------------------------------------------------- 
        registerFunction("map.new", (sc, in) ->     
        {
            in[0].set(sc, new HashMap<>());
            return Void.TYPE;
        });
        registerFunction("map.exists", (sc, in) -> in[0].get(sc) instanceof Map);
        registerFunction("map.add", (sc, in) -> ((Map) in[0].get(sc)).put(in[1].get(sc), in[2].get(sc)));
        registerFunction("map.remove", (sc, in) -> ((Map) in[0].get(sc)).remove(in[1].get(sc)));
        registerFunction("map.contains", (sc, in) -> ((Map) in[0].get(sc)).containsKey(in[1].get(sc)));
        registerFunction("map.getsize", (sc, in) -> new Fraction(((Map) in[0].get(sc)).size()));
        registerFunction("map.get", (sc, in) -> ((Map) in[0].get(sc)).get(in[1].get(sc)));
        registerFunction("map.getordefault", (sc, in) -> ((Map) in[0].get(sc)).getOrDefault(in[1].get(sc), in[2].get(sc)));
        registerFunction("map.clear", (sc, in) ->     
        {
            ((Map) in[0].get(sc)).clear();
            return Void.TYPE;
        });
        registerFunction("map.keys", (sc, in) ->     
        {
            in[0].set(sc, ((Map) in[1].get(sc)).keySet().stream().collect(Collectors.toList()));
            return Void.TYPE;
        });
        registerFunction("map.values", (sc, in) ->     
        {
            in[0].set(sc, ((Map) in[1].get(sc)).values().stream().collect(Collectors.toList()));
            return Void.TYPE;
        });
        
        // ---------------------------------------------------------------------  
        // sets
        // --------------------------------------------------------------------- 
        registerFunction("set.new", (sc, in) ->     
        {
            in[0].set(sc, new HashSet<>());
            return Void.TYPE;
        });
        registerFunction("set.exists", (sc, in) -> in[0].get(sc) instanceof Set);
        registerFunction("set.add", (sc, in) -> ((Set) in[0].get(sc)).add(in[1].get(sc)));
        registerFunction("set.remove", (sc, in) -> ((Set) in[0].get(sc)).remove(in[1].get(sc)));
        registerFunction("set.contains", (sc, in) -> ((Set) in[0].get(sc)).contains(in[1].get(sc)));
        registerFunction("set.getsize", (sc, in) -> new Fraction(((Set) in[0].get(sc)).size()));
        registerFunction("set.tolist", (sc, in) ->     
        {
            in[0].set(sc, ((Set) in[1].get(sc)).stream().collect(Collectors.toList()));
            return Void.TYPE;
        });

        // --------------------------------------------------------------------- 
        // time
        // ---------------------------------------------------------------------
        registerFunction("time.new", (sc, in) ->       
        {
            GregorianCalendar cal = GregorianCalendar.from(ZonedDateTime.now());
            cal.setTimeInMillis(in[1].getFraction(sc).longValue());
            in[0].set(sc, cal);
            return Void.TYPE;
        });
        registerFunction("time.getmillis", (sc, in) -> new Fraction(System.currentTimeMillis()));
        registerFunction("time.from", (sc, in) -> new Fraction(((GregorianCalendar) in[0].get(sc)).getTimeInMillis()));
        registerFunction("time.nextday", (sc, in) ->         
        {
            GregorianCalendar cal = (GregorianCalendar) in[0].get(sc);
            cal.add(Calendar.DAY_OF_YEAR, 1);
            cal.set(Calendar.HOUR, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return Void.TYPE;
        });   
        registerFunction("time.getyear", (sc, in) -> new Fraction(((GregorianCalendar) in[0].get(sc)).get(Calendar.YEAR)));
        registerFunction("time.getmonth", (sc, in) -> new Fraction(((GregorianCalendar) in[0].get(sc)).get(Calendar.MONTH) + 1));
        registerFunction("time.getday", (sc, in) -> new Fraction(((GregorianCalendar) in[0].get(sc)).get(Calendar.DAY_OF_MONTH)));
        registerFunction("time.gethour", (sc, in) -> new Fraction(((GregorianCalendar) in[0].get(sc)).get(Calendar.HOUR_OF_DAY)));
        registerFunction("time.getminute", (sc, in) -> new Fraction(((GregorianCalendar) in[0].get(sc)).get(Calendar.MINUTE)));
        registerFunction("time.getsecond", (sc, in) -> new Fraction(((GregorianCalendar) in[0].get(sc)).get(Calendar.SECOND)));
              
        // ---------------------------------------------------------------------    
        // text
        // ---------------------------------------------------------------------   
        registerFunction("text.matches", (sc, in) -> in[0].getString(sc).matches(in[1].getString(sc)));      
        registerFunction("text.number", (sc, in) -> 
        {
            Fraction f = in[0].getFraction(sc);
            if(f.doubleValue() == f.longValue())
            {
                return String.valueOf(f.longValue()); 
            }
            return String.valueOf(f.doubleValue()); 
        });
        registerFunction("text.class", (sc, in) -> in[0].get(sc).getClass().getSimpleName());      
        registerFunction("text.tolowercase", (sc, in) -> SnuviUtils.connect(sc, in, 0).toLowerCase());
        registerAlias("text.tolowercase", "tolowercase");
        registerFunction("text.touppercase", (sc, in) -> SnuviUtils.connect(sc, in, 0).toUpperCase());
        registerAlias("text.touppercase", "touppercase");
        registerFunction("text.split", (sc, in) ->      
        {
            in[0].set(sc, Arrays.stream(SnuviUtils.connect(sc, in, 2).split(in[1].getString(sc))).map(s -> Compiler.convert(s)).collect(Collectors.toList()));
            return Void.TYPE;
        });  
        registerAlias("text.split", "split");
        registerFunction("text.concatlist", (sc, in) -> ((List<Object>) in[0].get(sc)).stream().limit(in[3].getInt(sc) + 1).skip(in[2].getInt(sc)).map(o -> String.valueOf(o)).collect(Collectors.joining(in[1].getString(sc))));       
        registerAlias("text.concatlist", "concatlist");
        registerFunction("text.concat", (sc, in) -> SnuviUtils.connect(sc, in, 0)); 
        registerAlias("text.concat", "concat");
        registerFunction("text", (sc, in) -> String.valueOf(in[0].get(sc)));       
        registerFunction("text.substring", (sc, in) -> in[0].getString(sc).substring(in[1].getInt(sc), in[2].getInt(sc))); 
        registerFunction("text.length", (sc, in) ->  in[0].getString(sc).length()); 
        registerFunction("text.startswith", (sc, in) -> in[0].getString(sc).startsWith(in[1].getString(sc), in[2].getInt(sc))); 
        registerFunction("text.endswith", (sc, in) -> in[0].getString(sc).endsWith(in[1].getString(sc))); 
        registerFunction("text.contains", (sc, in) ->  in[0].getString(sc).contains(in[1].getString(sc))); 
        registerFunction("text.indexof", (sc, in) -> in[0].getString(sc).indexOf(in[1].getString(sc), in[2].getInt(sc))); 
        registerFunction("text.lastindexof", (sc, in) -> in[0].getString(sc).lastIndexOf(in[1].getString(sc), in[2].getInt(sc)));
        registerFunction("text.replace", (sc, in) -> in[0].getString(sc).replace(in[1].getString(sc), in[2].getString(sc)));
        registerFunction("text.trim", (sc, in) -> in[0].getString(sc).trim());
        registerFunction("text.charat", (sc, in) -> String.valueOf(in[0].getString(sc).charAt(in[1].getInt(sc))));
        registerFunction("text.charcode", (sc, in) -> new Fraction(in[0].getString(sc).charAt(in[1].getInt(sc))));
        registerFunction("text.fromcode", (sc, in) -> String.valueOf((char) in[0].getInt(sc)));
        
        // -------------------------------------------------------------------------------    
        // files
        // ------------------------------------------------------------------------------- 
        
        registerFunction("file.new", (sc, in) ->    
        {
            in[0].set(sc, new File(in[1].getString(sc)));
            return Void.TYPE;
        });
        registerFunction("file.exists", (sc, in) -> ((File) in[0].get(sc)).exists());
        registerFunction("file.delete", (sc, in) ->  ((File) in[0].get(sc)).delete());
        registerFunction("file.getname", (sc, in) -> ((File) in[0].get(sc)).getName());
        registerFunction("file.getlist", (sc, in) ->       
        {
            in[0].set(sc, Arrays.asList(((File) in[0].get(sc)).listFiles()));
            return Void.TYPE;
        });
        registerFunction("file.read", (sc, in) ->         
        {
            try
            {
                in[0].set(sc, Files.readAllLines(((File) in[1].get(sc)).toPath()));
            }
            catch(IOException ex)
            {
                throw new FileIOException(ex.getMessage());
            }
            return Void.TYPE;
        });
        registerFunction("file.write", (sc, in) ->         
        {
            try
            {
                File f = (File) in[0].get(sc);
                if(f.getParentFile() != null)
                {
                    f.getParentFile().mkdirs();
                }
                if(!f.exists())
                {
                    try
                    {
                        f.createNewFile();
                    }
                    catch(IOException ex)
                    {
                        throw new FileIOException(ex.getMessage());
                    }
                }
                Files.write(Paths.get(f.toURI()), ((List<Object>) in[1].get(sc))
                        .stream().map(o -> String.valueOf(o)).collect(Collectors.toList()), StandardCharsets.UTF_8);
            }
            catch(UnsupportedOperationException | SecurityException | IOException ex)
            {
                throw new FileIOException(ex.getMessage());
            }
            return Void.TYPE;
        });
        
        // ---------------------------------------------------------------------  
        // config
        // ---------------------------------------------------------------------
        
        registerFunction("config.new", (sc, in) ->        
        {
            in[0].set(sc, new SnuviConfig(sc, in[1].getString(sc), in[2].getString(sc)));
            return Void.TYPE;
        });      
        registerFunction("config.exists", (sc, in) -> ((SnuviConfig) in[0].get(sc)).exists());
        registerFunction("config.save", (sc, in) -> ((SnuviConfig) in[0].get(sc)).save());
        registerFunction("config.load", (sc, in) -> 
        {
            ((SnuviConfig) in[0].get(sc)).load();
            return Void.TYPE;
        });
        registerFunction("config.delete", (sc, in) -> ((SnuviConfig) in[0].get(sc)).delete());
        registerFunction("config.set", (sc, in) ->      
        {
            ((SnuviConfig) in[0].get(sc)).set(in[1].getString(sc), in[2].get(sc));
            return Void.TYPE;
        });
        registerFunction("config.getbool", (sc, in) -> ((SnuviConfig) in[0].get(sc)).getBoolean(in[1].getString(sc), in[2].getBoolean(sc)));
        registerFunction("config.getfraction", (sc, in) -> ((SnuviConfig) in[0].get(sc)).getFraction(in[1].getString(sc), in[2].getFraction(sc)));
        registerFunction("config.getstring", (sc, in) -> ((SnuviConfig) in[0].get(sc)).getString(in[1].getString(sc), in[2].getString(sc)));
        
        // ---------------------------------------------------------------------    
        // commands without library
        // ---------------------------------------------------------------------   
        // elementary calculating
        registerFunction("+", (sc, in) -> 
        {
            return in[0].getFraction(sc).add(in[1].getFraction(sc));
        });
        registerAlias("+", "add");
        registerFunction("-", (sc, in) -> 
        {
            return in[0].getFraction(sc).sub(in[1].getFraction(sc));
        });
        registerAlias("-", "sub");
        registerFunction("*", (sc, in) -> 
        {
            return in[0].getFraction(sc).mul(in[1].getFraction(sc));
        });
        registerAlias("*", "mul");
        registerFunction("/", (sc, in) -> 
        {
            return in[0].getFraction(sc).div(in[1].getFraction(sc));
        });
        registerAlias("/", "div");

        // var setter
        registerFunction("=", (sc, in) -> 
        {
            in[0].set(sc, in[1].get(sc));
            return Void.TYPE;
        });
        registerFunction("+=", (sc, in) -> 
        {
            in[0].set(sc, in[0].getFraction(sc).add(in[1].getFraction(sc)));
            return Void.TYPE;
        });
        registerFunction("++", (sc, in) -> 
        {
            Fraction f = in[0].getFraction(sc);
            in[0].set(sc, f.add(new Fraction(1)));
            return f;
        });
        registerAlias("++", "inc");
        registerFunction("p+", (sc, in) -> 
        {
            Fraction f = in[0].getFraction(sc).add(new Fraction(1));
            in[0].set(sc, f);
            return f;
        });
        registerFunction("-=", (sc, in) -> 
        {
            in[0].set(sc, in[0].getFraction(sc).sub(in[1].getFraction(sc)));
            return Void.TYPE;
        });
        registerFunction("--", (sc, in) -> 
        {
            Fraction f = in[0].getFraction(sc);
            in[0].set(sc, f.sub(new Fraction(1)));
            return f;
        });
        registerAlias("--", "dec");
        registerFunction("p-", (sc, in) -> 
        {
            Fraction f = in[0].getFraction(sc).sub(new Fraction(1));
            in[0].set(sc, f);
            return f;
        });
        registerFunction("*=", (sc, in) -> 
        {
            in[0].set(sc, in[0].getFraction(sc).mul(in[1].getFraction(sc)));
            return Void.TYPE;
        });
        registerFunction("/=", (sc, in) -> 
        {
            in[0].set(sc, in[0].getFraction(sc).div(in[1].getFraction(sc)));
            return Void.TYPE;
        });
        registerFunction("%=", (sc, in) -> 
        {
            in[0].set(sc, new Fraction(in[0].getInt(sc) % in[1].getInt(sc)));
            return Void.TYPE;
        });
        registerFunction("<<=", (sc, in) -> 
        {
            in[0].set(sc, in[0].getFraction(sc).leftShift(in[1].getInt(sc)));
            return Void.TYPE;
        });
        registerFunction(">>=", (sc, in) -> 
        {
            in[0].set(sc, in[0].getFraction(sc).rightShift(in[1].getInt(sc)));
            return Void.TYPE;
        });
        registerFunction("&=", (sc, in) -> 
        {
            in[0].set(sc, in[0].getFraction(sc).and(in[1].getFraction(sc)));
            return Void.TYPE;
        });
        registerFunction("^=", (sc, in) -> 
        {
            in[0].set(sc, in[0].getFraction(sc).xor(in[1].getFraction(sc)));
            return Void.TYPE;
        });
        registerFunction("|=", (sc, in) -> 
        {
            in[0].set(sc, in[0].getFraction(sc).or(in[1].getFraction(sc)));
            return Void.TYPE;
        });
        
        // var stuff
        registerFunction("getvar", (sc, in) -> sc.getVar(in[0].getString(sc)).get(sc));   
        registerFunction("setvar", (sc, in) -> 
        {
            sc.getVar(in[0].getString(sc)).set(sc, in[1].get(sc));
            return Void.TYPE;
        });
        registerFunction("removevar", (sc, in) -> 
        {
            sc.getVar(in[0].getString(sc)).set(sc, null);
            return Void.TYPE;
        });  
        
        
        registerFunction("wait", (sc, in) -> 
        {
            sc.isWaiting = true;
            return Void.TYPE;
        });
        
        // try - catch
        registerFunction("try", (sc, in) -> 
        {
            sc.catchLine = sc.currentLine + in[0].getInt(sc);
            return Void.TYPE;
        });              
        registerFunction("catch", (sc, in) -> 
        {
            if(sc.catchLine != -1)
            {
                sc.currentLine += in[0].getInt(sc);
            }
            return Void.TYPE;
        });  
        
        // branching
        registerFunction("goto", (sc, in) -> 
        {
            sc.currentLine = sc.labels.get(in[0].getString(sc));
            return Void.TYPE;
        });       
        registerFunction("sgoto", (sc, in) -> 
        {
            if(sc.subScript)
            {
                throw new IllegalStateException("sgoto is not allowed in functions");
            }
            int time = in[0].getInt(sc);
            if(time < 0)
            {
                throw new IllegalArgumentException("time units can't be negative");
            }
            int label = sc.labels.get(in[1].getString(sc));
            sc.scheduler.scheduleTask(() -> 
            {
                if(!sc.isValid || sc.isHolded)
                {
                    return;
                }
                sc.currentLine = label + 1;
                sc.run();
            }, time);
            return Void.TYPE;
        });
        registerFunction("gosub", (sc, in) -> 
        {
            sc.returnStack.push(sc.currentLine);
            sc.currentLine = sc.labels.get(in[0].getString(sc));
            return Void.TYPE;
        });
        registerFunction("return", (sc, in) -> 
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
        });
        registerFunction("if", (sc, in) -> 
        {
            sc.ifState = in[0].getBoolean(sc);
            if(!sc.ifState)
            {
                sc.currentLine += in[1].getInt(sc);
            }
            return Void.TYPE;
        });
        registerFunction("elseif", (sc, in) -> 
        {
            if(sc.ifState)
            {
                sc.currentLine += in[1].getInt(sc);
            }
            else
            {
                sc.ifState = in[0].getBoolean(sc);
                if(!sc.ifState)
                {
                    sc.currentLine += in[1].getInt(sc);
                }
            }
            return Void.TYPE;
        });
        registerFunction("else", (sc, in) -> 
        {
            if(sc.ifState)
            {
                sc.currentLine += in[0].getInt(sc);
            }
            sc.ifState = true;
            return Void.TYPE;
        });  
        registerFunction("while", (sc, in) -> 
        {
            if(!in[0].getBoolean(sc))
            {
                sc.currentLine += in[1].getInt(sc);
            }
            return Void.TYPE;
        });
        registerFunction("wend", (sc, in) -> 
        {
            sc.currentLine += in[0].getInt(sc);
            return Void.TYPE;
        });
        registerFunction("for", (sc, in) -> 
        {
            // for(var, start, end, step)
            Fraction start = in[1].getFraction(sc);
            in[0].set(sc, start);           
            if(start.compareTo(in[2].getFraction(sc)) > 0)
            {
                sc.currentLine += in[4].getInt(sc);
            }
            return Void.TYPE;
        });
        registerFunction("next", (sc, in) -> 
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
        });
        registerFunction("continue", (sc, in) -> 
        {
            sc.currentLine += in[0].getInt(sc);
            return Void.TYPE;
        });
        registerFunction("break", (sc, in) -> 
        {
            sc.currentLine += in[0].getInt(sc);
            return Void.TYPE;
        });
        
        // comparing
        registerFunction("==", (sc, in) -> Objects.equals(in[0].get(sc), in[1].get(sc)));
        registerAlias("==", "equal");
        registerAlias("==", "equals");
        registerFunction("!=", (sc, in) -> !Objects.equals(in[0].get(sc), in[1].get(sc)));
        registerAlias("!=", "notequal");
        registerFunction("<", (sc, in) -> in[0].getFraction(sc).compareTo(in[1].getFraction(sc)) < 0);
        registerAlias("<", "less");
        registerFunction(">", (sc, in) -> in[0].getFraction(sc).compareTo(in[1].getFraction(sc)) > 0);
        registerAlias(">", "greater");
        registerFunction("<=", (sc, in) -> in[0].getFraction(sc).compareTo(in[1].getFraction(sc)) <= 0);
        registerAlias("<=", "lessequal");
        registerFunction(">=", (sc, in) -> in[0].getFraction(sc).compareTo(in[1].getFraction(sc)) >= 0);
        registerAlias(">=", "greaterequal");
        registerFunction("!", (sc, in) -> !in[0].getBoolean(sc));
        registerAlias("!", "invert");
        
        // logical stuff
        registerFunction("&&", (sc, in) -> Arrays.stream(in).map(i -> i.getBoolean(sc)).allMatch(s -> s));
        registerAlias("&&", "and");
        registerFunction("||", (sc, in) -> Arrays.stream(in).map(i -> i.getBoolean(sc)).anyMatch(s -> s));
        registerAlias( "||", "or");
        
        // non grouped stuff
        registerFunction("swap", (sc, in) -> 
        {
            Object o = in[0].get(sc);
            in[0].set(sc, in[1].get(sc));
            in[1].set(sc, o);
            return Void.TYPE;
        });
        registerFunction("print", (sc, in) -> 
        {
            System.out.println(SnuviUtils.connect(sc, in, 0));
            return Void.TYPE;
        });
        registerFunction("waitfor", (sc, in) ->    
        {
            if(sc.subScript)
            {
                throw new IllegalStateException("waitfor is not allowed in functions");
            }
            long l = in[0].getInt(sc);
            if(l < 0)
            {
                throw new IllegalArgumentException("time units can't be negative");
            }
            sc.isHolded = true;
            sc.scheduler.scheduleTask(() -> 
            {           
                // activate this again on NullPointerException
                // if(sc == null || !sc.isValid)
                if(sc.isValid)
                {
                    sc.isHolded = false;
                    sc.run();
                }
            }, l); 
            sc.isWaiting = true;
            return Void.TYPE;
        });
        registerFunction("term", (sc, in) -> 
        {
            sc.parser.termSafe(sc);
            return Void.TYPE;
        });
                
        registerFunction("islong", (sc, in) ->                                           
        {
            Object o = in[0].get(sc);
            if(o instanceof Fraction)
            {
                return ((Fraction) o).isLong();
            }
            return false;
        });
        registerFunction("assert", (sc, in) ->                                           
        {
            if(!in[0].getBoolean(sc))
            {
                throw new AssertionException("assertion failed");
            }
            return Void.TYPE;
        });
    }
}
