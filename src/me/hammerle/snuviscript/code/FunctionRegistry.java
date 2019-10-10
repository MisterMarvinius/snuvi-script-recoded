package me.hammerle.snuviscript.code;

import me.hammerle.snuviscript.inputprovider.InputProvider;
import java.io.File;
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
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import me.hammerle.snuviscript.config.SnuviConfig;

public class FunctionRegistry 
{
    private static final HashMap<String, Object> GLOBAL_VARS = new HashMap<>();
    private static final HashMap<String, NamedFunction> FUNCTIONS = new HashMap<>();
    
    protected static void registerFunction(String name, String fname, ExceptionBiFunction<Script, InputProvider[], Object> f)
    {
        FUNCTIONS.put(name, new NamedFunction(fname, f));
    }
    
    protected static void registerFunction(String name, ExceptionBiFunction<Script, InputProvider[], Object> f)
    {
        registerFunction(name, name, f);
    }
    
    protected static void registerAlias(String original, String alias)
    {
        FUNCTIONS.put(alias, FUNCTIONS.get(original));
    }
    
    public static NamedFunction getFunction(String f)
    {
        final String function = f.toLowerCase();
        return FUNCTIONS.getOrDefault(function, new NamedFunction(function, (sc, in) -> 
        {
            sc.handleFunction(function, in);
            return Void.TYPE;
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
            sc.setStackTrace(in[0].getBoolean(sc));
            return Void.TYPE;
        });
        registerFunction("", (sc, in) -> in[0].get(sc));
        registerFunction("test", (sc, in) -> 
        {
            //sc.getScriptManager().startScript(true, ".sbasic", "./test2");  
            return Void.TYPE;
        });
        
        // ---------------------------------------------------------------------  
        // event
        // --------------------------------------------------------------------- 
        registerFunction("event.load", (sc, in) ->
        {
            String event = in[0].getString(sc);
            sc.loadEvent(event);
            sc.getScriptManager().loadEventSafe(event, sc);
            return Void.TYPE;
        });
        registerFunction("event.unload", (sc, in) ->
        {
            String event = in[0].getString(sc);
            sc.unloadEvent(in[0].getString(sc));
            sc.getScriptManager().unloadEventSafe(event, sc);
            return Void.TYPE;
        });
        registerFunction("event.isloaded", (sc, in) -> sc.isEventLoaded(in[0].getString(sc)));

        // ---------------------------------------------------------------------  
        // script
        // ---------------------------------------------------------------------  
        registerFunction("script.get", (sc, in) -> 
        {
            String name = in[0].getString(sc);
            for(Script script : sc.getScriptManager().getScripts())
            {
                if(script.getName().equals(name))
                {
                    return script;
                }
            }
            return null;
        });
        registerFunction("script.getall", (sc, in) -> 
        {
            String name = in[0].getString(sc);
            return sc.getScriptManager().getScripts().stream()
                    .filter(script -> script.getName().equals(name))
                    .collect(Collectors.toList());
        });
        registerFunction("script.term", (sc, in) -> 
        {
            Script other = (Script) in[0].get(sc);
            other.term();
            sc.getScriptManager().removeScriptSafe(other);
            return Void.TYPE;
        });
        
        // ---------------------------------------------------------------------    
        // bit
        // --------------------------------------------------------------------- 
        
        registerFunction(">>", (sc, in) -> (double) (in[0].getInt(sc) >> in[1].getInt(sc)));
        registerFunction("<<", (sc, in) -> (double) (in[0].getInt(sc) << in[1].getInt(sc)));
        registerFunction("&", (sc, in) -> (double) (in[0].getInt(sc) & in[1].getInt(sc)));
        registerFunction("|", (sc, in) -> (double) (in[0].getInt(sc) | in[1].getInt(sc)));
        registerFunction("^", (sc, in) -> (double) (in[0].getInt(sc) ^ in[1].getInt(sc)));
        registerFunction("~", (sc, in) -> (double) (~in[0].getInt(sc)));
        registerFunction("bit.set", (sc, in) -> (double) (in[0].getInt(sc) | (1 << (in[1].getInt(sc)))));
        registerFunction("bit.unset", (sc, in) -> (double) (in[0].getInt(sc) & (~(1 << (in[1].getInt(sc))))));
        registerFunction("bit.get", (sc, in) -> (in[0].getInt(sc) & (1 << (in[1].getInt(sc)))) != 0);
        
        // ---------------------------------------------------------------------    
        // math
        // ---------------------------------------------------------------------    
        registerFunction("%", (sc, in) -> (double) (in[0].getInt(sc) % in[1].getInt(sc)));
        registerAlias("%", "math.mod");
        registerFunction("math.abs", (sc, in) -> Math.abs(in[0].getDouble(sc)));
        registerFunction("math.pow", (sc, in) -> Math.pow(in[0].getDouble(sc), in[1].getDouble(sc)));
        registerFunction("math.root", (sc, in) -> Math.pow(in[0].getDouble(sc), 1.0 / in[1].getDouble(sc)));
        registerFunction("math.sqrt", (sc, in) -> Math.sqrt(in[0].getDouble(sc)));
        registerFunction("math.hypot", (sc, in) -> Math.hypot(in[0].getDouble(sc), in[1].getDouble(sc)));
        registerFunction("math.sin", (sc, in) -> Math.sin(in[0].getDouble(sc)));
        registerFunction("math.cos", (sc, in) -> Math.cos(in[0].getDouble(sc)));
        registerFunction("math.tan", (sc, in) -> Math.tan(in[0].getDouble(sc)));
        registerFunction("math.asin", (sc, in) -> Math.asin(in[0].getDouble(sc)));
        registerFunction("math.acos", (sc, in) -> Math.acos(in[0].getDouble(sc)));
        registerFunction("math.atan", (sc, in) -> Math.atan(in[0].getDouble(sc)));
        registerFunction("math.e", (sc, in) -> Math.E);
        registerFunction("math.pi", (sc, in) -> Math.PI);
        registerFunction("math.ln", (sc, in) -> Math.log(in[0].getDouble(sc)));
        registerFunction("math.log", (sc, in) -> Math.log10(in[0].getDouble(sc)));
        registerFunction("math.random", (sc, in) -> (double) SnuviUtils.randomInt(in[0].getInt(sc), in[1].getInt(sc)));
        registerFunction("math.round", (sc, in) -> (double) Math.round(in[0].getDouble(sc)));
        registerFunction("math.rounddown", (sc, in) -> Math.floor(in[0].getDouble(sc)));
        registerFunction("math.roundup", (sc, in) -> Math.ceil(in[0].getDouble(sc)));
        registerFunction("math.roundcomma", (sc, in) -> 
        {
            double d = in[0].getDouble(sc);
            int factor = (int) Math.pow(10, in[1].getInt(sc));
            return (double) (((double) Math.round(d * factor)) / factor);
        });
        
        // ---------------------------------------------------------------------  
        // lists
        // ---------------------------------------------------------------------    
        registerFunction("list.new", (sc, in) ->     
        {
            if(in.length == 0)
            {
                return new ArrayList<>();
            }
            in[0].set(sc, new ArrayList<>());
            return Void.TYPE;
        });
        registerFunction("list.exists", (sc, in) -> in[0].get(sc) instanceof List);
        registerFunction("list.add", (sc, in) -> ((List) in[0].get(sc)).add(in[1].get(sc)));
        registerFunction("list.addall", (sc, in) -> 
        {
            List list = ((List) in[0].get(sc));
            for(int i = 1; i < in.length; i++)
            {
                list.add(in[i].get(sc));
            }
            return Void.TYPE;
        });
        registerFunction("list.remove", (sc, in) -> ((List) in[0].get(sc)).remove(in[1].get(sc)));
        registerFunction("list.removeindex", (sc, in) -> ((List) in[0].get(sc)).remove(in[1].getInt(sc)));
        registerFunction("list.contains", (sc, in) -> ((List) in[0].get(sc)).contains(in[1].get(sc)));
        registerFunction("list.getsize", (sc, in) -> (double) ((List) in[0].get(sc)).size());
        registerFunction("list.getindex", (sc, in) -> ((List) in[0].get(sc)).get(in[1].getInt(sc)));
        registerAlias("list.getindex", "list.get");
        registerFunction("list.setindex", (sc, in) -> ((List) in[0].get(sc)).set(in[1].getInt(sc), in[2].get(sc)));
        registerFunction("list.clear", (sc, in) ->     
        {
            ((List) in[0].get(sc)).clear();
            return Void.TYPE;
        });
        registerFunction("list.getindexof", (sc, in) -> (double) ((List) in[0].get(sc)).indexOf(in[1].get(sc)));
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
        registerFunction("list.iterator", (sc, in) -> ((List) in[0].get(sc)).iterator());

        // ---------------------------------------------------------------------  
        // arrays
        // ---------------------------------------------------------------------   
        registerFunction("array.new", (sc, in) -> 
        {
            if(in.length == 0)
            {
                throw new ArrayIndexOutOfBoundsException("missing array dimension");
            }
            int[] dim = new int[in.length];
            for(int i = 0; i < in.length; i++)
            {
                dim[i] = in[i].getInt(sc);
            }
            return Array.newInstance(Object.class, dim);
        });
        registerFunction("array.getsize", (sc, in) -> (double) Array.getLength(in[0].get(sc)));
        registerAlias("array.getsize", "array.length");
        
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
            if(in.length == 0)
            {
                return new HashMap<>();
            }
            in[0].set(sc, new HashMap<>());
            return Void.TYPE;
        });
        registerFunction("map.exists", (sc, in) -> in[0].get(sc) instanceof Map);
        registerFunction("map.add", (sc, in) -> ((Map) in[0].get(sc)).put(in[1].get(sc), in[2].get(sc)));
        registerFunction("map.remove", (sc, in) -> ((Map) in[0].get(sc)).remove(in[1].get(sc)));
        registerFunction("map.contains", (sc, in) -> ((Map) in[0].get(sc)).containsKey(in[1].get(sc)));
        registerFunction("map.getsize", (sc, in) -> (double) ((Map) in[0].get(sc)).size());
        registerFunction("map.get", (sc, in) -> ((Map) in[0].get(sc)).get(in[1].get(sc)));
        registerFunction("map.getordefault", (sc, in) -> ((Map) in[0].get(sc)).getOrDefault(in[1].get(sc), in[2].get(sc)));
        registerFunction("map.clear", (sc, in) ->     
        {
            ((Map) in[0].get(sc)).clear();
            return Void.TYPE;
        });
        registerFunction("map.iterator", (sc, in) -> ((Map) in[0].get(sc)).entrySet().iterator());
        registerFunction("map.getkey", (sc, in) -> ((Map.Entry) in[0].get(sc)).getKey());
        registerFunction("map.getvalue", (sc, in) -> ((Map.Entry) in[0].get(sc)).getValue());
        registerFunction("map.setvalue", (sc, in) -> ((Map.Entry) in[0].get(sc)).setValue(in[1].get(sc)));
        
        // ---------------------------------------------------------------------  
        // sets
        // --------------------------------------------------------------------- 
        registerFunction("set.new", (sc, in) ->     
        {
            if(in.length == 0)
            {
                return new HashSet<>();
            }
            in[0].set(sc, new HashSet<>());
            return Void.TYPE;
        });
        registerFunction("set.exists", (sc, in) -> in[0].get(sc) instanceof Set);
        registerFunction("set.add", (sc, in) -> ((Set) in[0].get(sc)).add(in[1].get(sc)));
        registerFunction("set.addall", (sc, in) -> 
        {
            Set set = ((Set) in[0].get(sc));
            for(int i = 1; i < in.length; i++)
            {
                set.add(in[i].get(sc));
            }
            return Void.TYPE;
        });
        registerFunction("set.remove", (sc, in) -> ((Set) in[0].get(sc)).remove(in[1].get(sc)));
        registerFunction("set.contains", (sc, in) -> ((Set) in[0].get(sc)).contains(in[1].get(sc)));
        registerFunction("set.getsize", (sc, in) -> (double) ((Set) in[0].get(sc)).size());
        registerFunction("set.clear", (sc, in) ->     
        {
            ((Set) in[0].get(sc)).clear();
            return Void.TYPE;
        });
        registerFunction("set.iterator", (sc, in) -> ((Set) in[0].get(sc)).iterator());

        // --------------------------------------------------------------------- 
        // time
        // ---------------------------------------------------------------------
        registerFunction("time.new", (sc, in) ->       
        {
            if(in.length <= 1)
            {
                GregorianCalendar cal = GregorianCalendar.from(ZonedDateTime.now());
                cal.setTimeInMillis(in[0].getLong(sc));
                return cal;
            }
            else
            {
                GregorianCalendar cal = GregorianCalendar.from(ZonedDateTime.now());
                cal.setTimeInMillis(in[1].getLong(sc));
                in[0].set(sc, cal);
                return Void.TYPE;
            }
        });
        registerFunction("time.getmillis", (sc, in) -> (double) System.currentTimeMillis());
        registerFunction("time.getnanos", (sc, in) -> (double) System.nanoTime());
        registerFunction("time.from", (sc, in) -> (double) ((GregorianCalendar) in[0].get(sc)).getTimeInMillis());
        registerFunction("time.nextday", (sc, in) ->         
        {
            GregorianCalendar cal = (GregorianCalendar) in[0].get(sc);
            cal.add(Calendar.DAY_OF_YEAR, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return Void.TYPE;
        });   
        registerFunction("time.getyear", (sc, in) -> (double) ((GregorianCalendar) in[0].get(sc)).get(Calendar.YEAR));
        registerFunction("time.getmonth", (sc, in) -> (double) (((GregorianCalendar) in[0].get(sc)).get(Calendar.MONTH) + 1));
        registerFunction("time.getday", (sc, in) -> (double) (((GregorianCalendar) in[0].get(sc)).get(Calendar.DAY_OF_MONTH)));
        registerFunction("time.gethour", (sc, in) -> (double) ((GregorianCalendar) in[0].get(sc)).get(Calendar.HOUR_OF_DAY));
        registerFunction("time.getminute", (sc, in) -> (double) ((GregorianCalendar) in[0].get(sc)).get(Calendar.MINUTE));
        registerFunction("time.getsecond", (sc, in) -> (double) ((GregorianCalendar) in[0].get(sc)).get(Calendar.SECOND));
              
        // ---------------------------------------------------------------------    
        // text
        // ---------------------------------------------------------------------   
        registerFunction("text.matches", (sc, in) -> in[0].getString(sc).matches(in[1].getString(sc)));      
        registerFunction("text.number", (sc, in) -> SnuviUtils.toString(in[0].getDouble(sc)));
        registerFunction("text.class", (sc, in) -> in[0].get(sc).getClass().getSimpleName());      
        registerFunction("text.tolowercase", (sc, in) -> SnuviUtils.connect(sc, in, 0).toLowerCase());
        registerAlias("text.tolowercase", "tolowercase");
        registerFunction("text.touppercase", (sc, in) -> SnuviUtils.connect(sc, in, 0).toUpperCase());
        registerAlias("text.touppercase", "touppercase");
        registerFunction("text.split", (sc, in) ->      
        {
            if(in.length <= 2)
            {
                String[] parts = in[1].getString(sc).split(in[0].getString(sc));
                ArrayList<Object> list = new ArrayList<>();
                for(String part : parts)
                {
                    list.add(SnuviUtils.convert(part));
                }
                return list;
            }
            else
            {
                String[] parts = in[2].getString(sc).split(in[1].getString(sc));
                ArrayList<Object> list = new ArrayList<>();
                for(String part : parts)
                {
                    list.add(SnuviUtils.convert(part));
                }
                in[0].set(sc, list);
                return Void.TYPE;
            }
        });  
        registerAlias("text.split", "split");
        registerFunction("text.concatlist", (sc, in) ->     
        {
            StringBuilder sb = new StringBuilder();
            List<Object> list = (List<Object>) in[0].get(sc);
            String splitter = in[1].getString(sc);
            Iterator<Object> iter = list.iterator();
            int from = in[2].getInt(sc);
            int to = Math.min(in[3].getInt(sc), list.size() - 1);
            to -= from;
            while(iter.hasNext() && from > 0)
            {
                iter.next();
                from--;
            }
            while(iter.hasNext() && to > 0)
            {
                sb.append(iter.next());
                sb.append(splitter);
                to--;
            }
            if(iter.hasNext() && to == 0)
            {
                sb.append(iter.next());
            }
            return sb.toString();
        });       
        registerAlias("text.concatlist", "concatlist");
        registerFunction("text.concat", (sc, in) -> SnuviUtils.connect(sc, in, 0)); 
        registerAlias("text.concat", "concat");
        registerFunction("text.concatspace", (sc, in) -> SnuviUtils.connect(sc, in, " ", 0));   
        registerFunction("text", (sc, in) -> String.valueOf(in[0].get(sc)));       
        registerFunction("text.substring", (sc, in) -> in[0].getString(sc).substring(in[1].getInt(sc), in[2].getInt(sc))); 
        registerFunction("text.length", (sc, in) ->  (double) in[0].getString(sc).length()); 
        registerFunction("text.startswith", (sc, in) -> in[0].getString(sc).startsWith(in[1].getString(sc), in[2].getInt(sc))); 
        registerFunction("text.endswith", (sc, in) -> in[0].getString(sc).endsWith(in[1].getString(sc))); 
        registerFunction("text.contains", (sc, in) ->  in[0].getString(sc).contains(in[1].getString(sc))); 
        registerFunction("text.indexof", (sc, in) -> in[0].getString(sc).indexOf(in[1].getString(sc), in[2].getInt(sc))); 
        registerFunction("text.lastindexof", (sc, in) -> in[0].getString(sc).lastIndexOf(in[1].getString(sc), in[2].getInt(sc)));
        registerFunction("text.replace", (sc, in) -> in[0].getString(sc).replace(in[1].getString(sc), in[2].getString(sc)));
        registerFunction("text.trim", (sc, in) -> in[0].getString(sc).trim());
        registerFunction("text.charat", (sc, in) -> String.valueOf(in[0].getString(sc).charAt(in[1].getInt(sc))));
        registerFunction("text.charcode", (sc, in) -> (double) in[0].getString(sc).charAt(in[1].getInt(sc)));
        registerFunction("text.fromcode", (sc, in) -> String.valueOf((char) in[0].getInt(sc)));
        registerFunction("text.onlyletters", (sc, in) -> 
        {             
            for(char c : in[0].getString(sc).toCharArray())
            {
                if(!Character.isLetter(c))
                {
                    return false;
                }
            }
            return true;
        });
        
        // -------------------------------------------------------------------------------    
        // files
        // ------------------------------------------------------------------------------- 
        
        registerFunction("file.new", (sc, in) -> new File(in[0].getString(sc)));
        registerFunction("file.exists", (sc, in) -> ((File) in[0].get(sc)).exists());
        registerFunction("file.isfile", (sc, in) -> ((File) in[0].get(sc)).isFile());
        registerFunction("file.isdirectory", (sc, in) -> ((File) in[0].get(sc)).isDirectory());
        registerFunction("file.delete", (sc, in) ->  ((File) in[0].get(sc)).delete());
        registerFunction("file.getname", (sc, in) -> ((File) in[0].get(sc)).getName());
        registerFunction("file.getlist", (sc, in) -> Arrays.asList(((File) in[0].get(sc)).listFiles()));
        registerFunction("file.read", (sc, in) -> Files.readAllLines(((File) in[0].get(sc)).toPath()));
        registerFunction("file.write", (sc, in) ->         
        {
            File f = (File) in[0].get(sc);
            if(f.getParentFile() != null)
            {
                f.getParentFile().mkdirs();
            }
            if(!f.exists())
            {
                f.createNewFile();
            }
            Files.write(Paths.get(f.toURI()), ((List<Object>) in[1].get(sc))
                    .stream().map(o -> String.valueOf(o)).collect(Collectors.toList()), StandardCharsets.UTF_8);
            return Void.TYPE;
        });
        
        // ---------------------------------------------------------------------  
        // config
        // ---------------------------------------------------------------------
        
        registerFunction("config.new", (sc, in) -> new SnuviConfig(sc, in[0].getString(sc), in[1].getString(sc)));      
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
        registerFunction("config.getdouble", (sc, in) -> ((SnuviConfig) in[0].get(sc)).getDouble(in[1].getString(sc), in[2].getDouble(sc)));
        registerFunction("config.getstring", (sc, in) -> ((SnuviConfig) in[0].get(sc)).getString(in[1].getString(sc), in[2].getString(sc)));
        
        // ---------------------------------------------------------------------  
        // read library   
        // ---------------------------------------------------------------------
        registerFunction("read.number", (sc, in) -> Double.parseDouble(in[0].getString(sc)));
        
        // ---------------------------------------------------------------------    
        // commands without library
        // ---------------------------------------------------------------------   
        // elementary calculating
        registerFunction("+", (sc, in) -> in[0].getDouble(sc) + in[1].getDouble(sc));
        registerAlias("+", "add");
        registerFunction("-", (sc, in) -> in.length == 1 ? -in[0].getDouble(sc) : in[0].getDouble(sc) - in[1].getDouble(sc));
        registerAlias("-", "sub");
        registerFunction("*", (sc, in) -> in[0].getDouble(sc) * in[1].getDouble(sc));
        registerAlias("*", "mul");
        registerFunction("/", (sc, in) -> in[0].getDouble(sc) / in[1].getDouble(sc));
        registerAlias("/", "div");

        // var setter
        registerFunction("=", (sc, in) -> 
        {
            Object o = in[1].get(sc);
            in[0].set(sc, o);
            return o;
        });
        registerFunction("+=", (sc, in) -> 
        {
            Object o = in[0].getDouble(sc) + in[1].getDouble(sc);
            in[0].set(sc, o);
            return o;
        });
        registerFunction("p++", (sc, in) -> 
        {
            double d = in[0].getDouble(sc);
            in[0].set(sc, d + 1.0);
            return d;
        });
        registerAlias("p++", "inc");
        registerFunction("++", (sc, in) -> 
        {
            double d = in[0].getDouble(sc) + 1.0;
            in[0].set(sc, d);
            return d;
        });
        registerFunction("-=", (sc, in) -> 
        {
            Object o = in[0].getDouble(sc) - in[1].getDouble(sc);
            in[0].set(sc, o);
            return o;
        });
        registerFunction("p--", (sc, in) -> 
        {
            double d = in[0].getDouble(sc);
            in[0].set(sc, d - 1.0);
            return d;
        });
        registerAlias("p--", "dec");
        registerFunction("--", (sc, in) -> 
        {
            double d = in[0].getDouble(sc) - 1.0;
            in[0].set(sc, d);
            return d;
        });
        registerFunction("*=", (sc, in) -> 
        {
            Object o = in[0].getDouble(sc) * in[1].getDouble(sc);
            in[0].set(sc, o);
            return o;
        });
        registerFunction("/=", (sc, in) -> 
        {
            Object o = in[0].getDouble(sc) / in[1].getDouble(sc);
            in[0].set(sc, o);
            return o;
        });
        registerFunction("%=", (sc, in) -> 
        {
            Object o = (double) (in[0].getInt(sc) % in[1].getInt(sc));
            in[0].set(sc, o);
            return o;
        });
        registerFunction("<<=", (sc, in) -> 
        {
            Object o = (double) (in[0].getInt(sc) << in[1].getInt(sc));
            in[0].set(sc, o);
            return o;
        });
        registerFunction(">>=", (sc, in) -> 
        {
            Object o = (double) (in[0].getInt(sc) >> in[1].getInt(sc));
            in[0].set(sc, o);
            return o;
        });
        registerFunction("&=", (sc, in) -> 
        {
            Object o = (double) (in[0].getInt(sc) & in[1].getInt(sc));
            in[0].set(sc, o);
            return o;
        });
        registerFunction("^=", (sc, in) -> 
        {
            Object o = (double) (in[0].getInt(sc) ^ in[1].getInt(sc));
            in[0].set(sc, o);
            return o;
        });
        registerFunction("|=", (sc, in) -> 
        {
            Object o = (double) (in[0].getInt(sc) | in[1].getInt(sc));
            in[0].set(sc, o);
            return o;
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
            sc.setWaiting();
            return Void.TYPE;
        });
        
        // branching
        registerFunction("goto", (sc, in) -> 
        {
            sc.gotoLabel(in[0].getString(sc), true);
            return Void.TYPE;
        });   
        registerFunction("ignoregoto", (sc, in) -> 
        {
            sc.gotoLabel(in[0].getString(sc), false);
            return Void.TYPE;
        }); 
        registerAlias("ignoregoto", "igoto");
        registerFunction("sgoto", (sc, in) -> 
        {
            int time = in[0].getInt(sc);
            if(time < 0)
            {
                throw new IllegalArgumentException("time units can't be negative");
            }
            String label = in[1].getString(sc);
            sc.getScriptManager().getScheduler().scheduleTask(() -> 
            {
                if(sc.shouldTerm() || sc.isHolded())
                {
                    return;
                }
                sc.gotoLabel(label, true, 1);
                sc.run();
                if(sc.shouldTerm())
                {
                    sc.getScriptManager().removeScriptSafe(sc);
                }
            }, time);
            return Void.TYPE;
        });
        registerFunction("gosub", (sc, in) -> 
        {
            sc.goSub(in[0].getString(sc));
            return Void.TYPE;
        });
        
        // comparing
        registerFunction("==", (sc, in) -> Objects.equals(in[0].get(sc), in[1].get(sc)));
        registerAlias("==", "equal");
        registerAlias("==", "equals");
        registerFunction("!=", (sc, in) -> !Objects.equals(in[0].get(sc), in[1].get(sc)));
        registerAlias("!=", "notequal");
        registerFunction("<", (sc, in) -> ((Comparable) in[0].get(sc)).compareTo(in[1].get(sc)) < 0);
        registerAlias("<", "less");
        registerFunction(">", (sc, in) -> ((Comparable) in[0].get(sc)).compareTo(in[1].get(sc)) > 0);
        registerAlias(">", "greater");
        registerFunction("<=", (sc, in) -> ((Comparable) in[0].get(sc)).compareTo(in[1].get(sc)) <= 0);
        registerAlias("<=", "lessequal");
        registerFunction(">=", (sc, in) -> ((Comparable) in[0].get(sc)).compareTo(in[1].get(sc)) >= 0);
        registerAlias(">=", "greaterequal");
        registerFunction("!", (sc, in) -> !in[0].getBoolean(sc));
        registerAlias("!", "invert");
        
        // logical stuff
        registerFunction("&&", (sc, in) -> 
        {
            for(InputProvider i : in)
            {
                if(!i.getBoolean(sc))
                {
                    return false;
                }
            }
            return true;
        });
        registerAlias("&&", "and");
        registerFunction("||", (sc, in) -> 
        {
            for(InputProvider i : in)
            {
                if(i.getBoolean(sc))
                {
                    return true;
                }
            }
            return false;
        });
        registerAlias( "||", "or");
        
        // non grouped stuff
        registerFunction("getscriptvar", (sc, in) -> GLOBAL_VARS.get(in[0].getString(sc)));
        registerFunction("setscriptvar", (sc, in) -> GLOBAL_VARS.put(in[0].getString(sc), in[1].get(sc)));      
        registerFunction("delscriptvar", (sc, in) -> GLOBAL_VARS.remove(in[0].getString(sc)));   
        registerFunction("hasnext", (sc, in) -> ((Iterator) in[0].get(sc)).hasNext());
        registerFunction("next", (sc, in) -> ((Iterator) in[0].get(sc)).next());
        registerFunction("swap", (sc, in) -> 
        {
            Object o = in[0].get(sc);
            in[0].set(sc, in[1].get(sc));
            in[1].set(sc, o);
            return Void.TYPE;
        });
        registerFunction("print", (sc, in) -> 
        {
            sc.getScriptManager().getLogger().print(SnuviUtils.connect(sc, in, 0), null, "print", sc.getName(), sc, sc.getActiveSourceLine());
            return Void.TYPE;
        });
        registerFunction("waitfor", (sc, in) ->    
        {
            long l = in[0].getInt(sc);
            if(l < 0)
            {
                throw new IllegalArgumentException("time units can't be negative");
            }
            sc.setHolded(true);
            sc.setWaiting();
            sc.getScriptManager().getScheduler().scheduleTask(() -> 
            {           
                if(sc.shouldTerm())
                {
                    return;
                }
                sc.setHolded(false);
                sc.run();
                if(sc.shouldTerm())
                {
                    sc.getScriptManager().removeScriptSafe(sc);
                }
            }, l); 
            return Void.TYPE;
        });
        registerFunction("term", (sc, in) -> 
        {
            sc.term();
            sc.getScriptManager().removeScriptSafe(sc);
            return Void.TYPE;
        });
               
        registerFunction("isdouble", (sc, in) ->                                           
        {
            return in[0].get(sc) instanceof Double;
        });
        registerFunction("islong", (sc, in) ->                                           
        {
            Object o = in[0].get(sc);
            if(o instanceof Double)
            {
                double d = (Double) o;
                return d == (long) d;
            }
            return false;
        });
        registerFunction("assert", (sc, in) ->                                           
        {
            if(!in[0].getBoolean(sc))
            {
                throw new IllegalArgumentException("assertion failed");
            }
            return Void.TYPE;
        });
        registerFunction("class", (sc, in) -> in[0].get(sc).getClass());   
        registerFunction("usedmemory", (sc, in) -> 
        {
            Runtime runtime = Runtime.getRuntime();
            double usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1048576;
            return usedMemory;
        });   
        registerFunction("allocatedmemory", (sc, in) -> Runtime.getRuntime().totalMemory() / 1048576.0);          
    }
}
