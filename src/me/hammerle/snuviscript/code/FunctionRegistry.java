package me.hammerle.snuviscript.code;

import me.hammerle.snuviscript.inputprovider.InputProvider;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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
import me.hammerle.snuviscript.inputprovider.Variable;

public class FunctionRegistry {
    public static final Set<PosixFilePermission> FILE_ACCESS =
            PosixFilePermissions.fromString("rwxrwxrwx");
    private static final HashMap<String, Object> GLOBAL_VARS = new HashMap<>();
    private static final HashMap<String, NamedFunction> FUNCTIONS = new HashMap<>();

    public static class ArrayIterator implements Iterator<Object> {
        private final Object[] array;
        private int index = 0;

        public ArrayIterator(Object[] array) {
            this.array = array;
        }

        @Override
        public boolean hasNext() {
            return index < array.length;
        }

        @Override
        public Object next() {
            return array[index++];
        }
    }

    protected static void registerFunction(String name,
            ExceptionBiFunction<Script, InputProvider[], Object> f) {
        FUNCTIONS.put(name, new NamedFunction(name, f));
    }

    protected static void registerConsumer(String name,
            ExceptionBiConsumer<Script, InputProvider[]> f) {
        FUNCTIONS.put(name, new NamedFunction(name, (sc, in) -> {
            f.apply(sc, in);
            return Void.TYPE;
        }));
    }

    protected static void registerAlias(String original, String alias) {
        FUNCTIONS.put(alias, FUNCTIONS.get(original));
    }

    public static NamedFunction getFunction(String f) {
        final String function = f.toLowerCase();
        return FUNCTIONS.getOrDefault(function, new NamedFunction(function, (sc, in) -> {
            sc.handleFunction(function, in);
            return Void.TYPE;
        }));
    }

    static {
        register();
    }

    private static class ScheduledGoto implements Runnable {
        private String name;
        private Script sc;
        private String label;
        private int line;

        public ScheduledGoto(String name, Script sc, String label, int line) {
            this.name = name;
            this.sc = sc;
            this.label = label;
            this.line = line;
        }

        @Override
        public void run() {
            if(sc.shouldTerm()) {
                return;
            } else if(sc.isHolded()) {
                sc.getScriptManager().getScheduler().scheduleTask(name,
                        new ScheduledGoto(name, sc, label, line), 2);
                return;
            }
            try {
                sc.gotoLabel(label, true, 1);
                sc.run();
            } catch(Exception ex) {
                sc.logException(ex, "sgoto", line);
            }
            if(sc.shouldTerm()) {
                sc.getScriptManager().removeScript(sc);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void register() {
        registerFunction("nothing", (sc, in) -> Void.TYPE);
        registerConsumer("modtimer", (sc, in) -> sc.addTimer(in[0].getLong(sc)));
        registerConsumer("error", (sc, in) -> sc.setStackTrace(in[0].getBoolean(sc)));
        registerConsumer("event.load", (sc, in) -> {
            String event = in[0].getString(sc);
            sc.loadEvent(event);
            sc.getScriptManager().loadEvent(event, sc);
        });
        registerConsumer("event.unload", (sc, in) -> {
            String event = in[0].getString(sc);
            sc.unloadEvent(in[0].getString(sc));
            sc.getScriptManager().unloadEvent(event, sc);
        });
        registerFunction("event.isloaded", (sc, in) -> sc.isEventLoaded(in[0].getString(sc)));
        registerFunction("script.get", (sc, in) -> {
            if(in.length == 0) {
                return sc;
            }
            String name = in[0].getString(sc);
            for(Script script : sc.getScriptManager().getScripts()) {
                if(script.getName().equals(name)) {
                    return script;
                }
            }
            return null;
        });
        registerFunction("script.getfromid",
                (sc, in) -> sc.getScriptManager().getScript(in[0].getInt(sc)));
        registerFunction("script.getid", (sc, in) -> (double) ((Script) in[0].get(sc)).getId());
        registerFunction("script.getvar", (sc, in) -> {
            Script other = (Script) in[0].get(sc);
            Variable v = other.getVar(in[1].getString(sc));
            if(v == null) {
                return null;
            }
            return v.get(other);
        });
        registerConsumer("script.setvar", (sc, in) -> {
            Script other = (Script) in[0].get(sc);
            other.getVar(in[1].getString(sc)).set(other, in[2].get(sc));
        });
        registerFunction("script.getall", (sc, in) -> {
            String name = in[0].getString(sc);
            return sc.getScriptManager().getScripts().stream()
                    .filter(script -> script.getName().equals(name)).collect(Collectors.toList());
        });
        registerConsumer("script.term", (sc, in) -> {
            Script other = (Script) in[0].get(sc);
            other.term();
            sc.getScriptManager().removeScript(other);
        });
        registerFunction("bit.rightshift",
                (sc, in) -> (double) (in[0].getInt(sc) >> in[1].getInt(sc)));
        registerFunction("bit.leftshift",
                (sc, in) -> (double) (in[0].getInt(sc) << in[1].getInt(sc)));
        registerFunction("bit.and", (sc, in) -> (double) (in[0].getInt(sc) & in[1].getInt(sc)));
        registerFunction("bit.or", (sc, in) -> (double) (in[0].getInt(sc) | in[1].getInt(sc)));
        registerFunction("bit.xor", (sc, in) -> (double) (in[0].getInt(sc) ^ in[1].getInt(sc)));
        registerFunction("bit.invert", (sc, in) -> (double) (~in[0].getInt(sc)));
        registerFunction("bit.set",
                (sc, in) -> (double) (in[0].getInt(sc) | (1 << (in[1].getInt(sc)))));
        registerFunction("bit.unset",
                (sc, in) -> (double) (in[0].getInt(sc) & (~(1 << (in[1].getInt(sc))))));
        registerFunction("bit.get",
                (sc, in) -> (in[0].getInt(sc) & (1 << (in[1].getInt(sc)))) != 0);
        registerFunction("%", (sc, in) -> (double) (in[0].getInt(sc) % in[1].getInt(sc)));
        registerAlias("%", "math.mod");
        registerFunction("math.abs", (sc, in) -> Math.abs(in[0].getDouble(sc)));
        registerFunction("math.pow",
                (sc, in) -> Math.pow(in[0].getDouble(sc), in[1].getDouble(sc)));
        registerFunction("math.root",
                (sc, in) -> Math.pow(in[0].getDouble(sc), 1.0 / in[1].getDouble(sc)));
        registerFunction("math.sqrt", (sc, in) -> Math.sqrt(in[0].getDouble(sc)));
        registerFunction("math.hypot",
                (sc, in) -> Math.hypot(in[0].getDouble(sc), in[1].getDouble(sc)));
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
        registerFunction("math.random",
                (sc, in) -> (double) SnuviUtils.randomInt(in[0].getInt(sc), in[1].getInt(sc)));
        registerFunction("math.round", (sc, in) -> (double) Math.round(in[0].getDouble(sc)));
        registerFunction("math.rounddown", (sc, in) -> Math.floor(in[0].getDouble(sc)));
        registerFunction("math.roundup", (sc, in) -> Math.ceil(in[0].getDouble(sc)));
        registerFunction("math.roundcomma", (sc, in) -> {
            double d = in[0].getDouble(sc);
            int factor = (int) Math.pow(10, in[1].getInt(sc));
            return (double) (((double) Math.round(d * factor)) / factor);
        });
        registerFunction("math.min",
                (sc, in) -> Math.min(in[0].getDouble(sc), in[1].getDouble(sc)));
        registerFunction("math.max",
                (sc, in) -> Math.max(in[0].getDouble(sc), in[1].getDouble(sc)));
        registerFunction("list.new", (sc, in) -> new ArrayList<>());
        registerFunction("list.exists", (sc, in) -> in[0].get(sc) instanceof List);
        registerFunction("list.add", (sc, in) -> ((List) in[0].get(sc)).add(in[1].get(sc)));
        registerConsumer("list.addall", (sc, in) -> {
            List list = ((List) in[0].get(sc));
            for(int i = 1; i < in.length; i++) {
                list.add(in[i].get(sc));
            }
        });
        registerFunction("list.remove", (sc, in) -> ((List) in[0].get(sc)).remove(in[1].get(sc)));
        registerFunction("list.removeindex",
                (sc, in) -> ((List) in[0].get(sc)).remove(in[1].getInt(sc)));
        registerFunction("list.contains",
                (sc, in) -> ((List) in[0].get(sc)).contains(in[1].get(sc)));
        registerFunction("list.getsize", (sc, in) -> (double) ((List) in[0].get(sc)).size());
        registerFunction("list.getindex", (sc, in) -> ((List) in[0].get(sc)).get(in[1].getInt(sc)));
        registerAlias("list.getindex", "list.get");
        registerFunction("list.setindex",
                (sc, in) -> ((List) in[0].get(sc)).set(in[1].getInt(sc), in[2].get(sc)));
        registerConsumer("list.clear", (sc, in) -> ((List) in[0].get(sc)).clear());
        registerFunction("list.getindexof",
                (sc, in) -> (double) ((List) in[0].get(sc)).indexOf(in[1].get(sc)));
        registerConsumer("list.sort", (sc, in) -> {
            Collections.sort(((List<Object>) in[0].get(sc)),
                    (o1, o2) -> ((Comparable) o1).compareTo(o2));
        });
        registerConsumer("list.reverse",
                (sc, in) -> Collections.reverse((List<Object>) in[0].get(sc)));
        registerConsumer("list.shuffle",
                (sc, in) -> Collections.shuffle((List<Object>) in[0].get(sc)));
        registerFunction("list.iterator", (sc, in) -> ((List) in[0].get(sc)).iterator());
        registerFunction("array.new", (sc, in) -> {
            if(in.length == 0) {
                throw new ArrayIndexOutOfBoundsException("missing array dimension");
            }
            int[] dim = new int[in.length];
            for(int i = 0; i < in.length; i++) {
                dim[i] = in[i].getInt(sc);
            }
            return Array.newInstance(Object.class, dim);
        });
        registerFunction("array.getsize", (sc, in) -> (double) Array.getLength(in[0].get(sc)));
        registerAlias("array.getsize", "array.length");
        registerFunction("map.new", (sc, in) -> new HashMap<>());
        registerFunction("map.exists", (sc, in) -> in[0].get(sc) instanceof Map);
        registerFunction("map.add",
                (sc, in) -> ((Map) in[0].get(sc)).put(in[1].get(sc), in[2].get(sc)));
        registerFunction("map.remove", (sc, in) -> ((Map) in[0].get(sc)).remove(in[1].get(sc)));
        registerFunction("map.contains",
                (sc, in) -> ((Map) in[0].get(sc)).containsKey(in[1].get(sc)));
        registerFunction("map.getsize", (sc, in) -> (double) ((Map) in[0].get(sc)).size());
        registerFunction("map.get", (sc, in) -> ((Map) in[0].get(sc)).get(in[1].get(sc)));
        registerFunction("map.getordefault",
                (sc, in) -> ((Map) in[0].get(sc)).getOrDefault(in[1].get(sc), in[2].get(sc)));
        registerConsumer("map.clear", (sc, in) -> ((Map) in[0].get(sc)).clear());
        registerFunction("map.iterator", (sc, in) -> ((Map) in[0].get(sc)).entrySet().iterator());
        registerFunction("map.getkey", (sc, in) -> ((Map.Entry) in[0].get(sc)).getKey());
        registerFunction("map.getvalue", (sc, in) -> ((Map.Entry) in[0].get(sc)).getValue());
        registerFunction("map.setvalue",
                (sc, in) -> ((Map.Entry) in[0].get(sc)).setValue(in[1].get(sc)));
        registerFunction("set.new", (sc, in) -> new HashSet<>());
        registerFunction("set.exists", (sc, in) -> in[0].get(sc) instanceof Set);
        registerFunction("set.add", (sc, in) -> ((Set) in[0].get(sc)).add(in[1].get(sc)));
        registerConsumer("set.addall", (sc, in) -> {
            Set set = ((Set) in[0].get(sc));
            for(int i = 1; i < in.length; i++) {
                set.add(in[i].get(sc));
            }
        });
        registerFunction("set.remove", (sc, in) -> ((Set) in[0].get(sc)).remove(in[1].get(sc)));
        registerFunction("set.contains", (sc, in) -> ((Set) in[0].get(sc)).contains(in[1].get(sc)));
        registerFunction("set.getsize", (sc, in) -> (double) ((Set) in[0].get(sc)).size());
        registerConsumer("set.clear", (sc, in) -> ((Set) in[0].get(sc)).clear());
        registerFunction("set.iterator", (sc, in) -> ((Set) in[0].get(sc)).iterator());
        registerFunction("time.new", (sc, in) -> {
            GregorianCalendar cal = GregorianCalendar.from(ZonedDateTime.now());
            cal.setTimeInMillis(in[0].getLong(sc));
            return cal;
        });
        registerFunction("time.getmillis", (sc, in) -> (double) System.currentTimeMillis());
        registerFunction("time.getnanos", (sc, in) -> (double) System.nanoTime());
        registerFunction("time.from",
                (sc, in) -> (double) ((GregorianCalendar) in[0].get(sc)).getTimeInMillis());
        registerConsumer("time.nextday", (sc, in) -> {
            GregorianCalendar cal = (GregorianCalendar) in[0].get(sc);
            cal.add(Calendar.DAY_OF_YEAR, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.MILLISECOND, 0);
        });
        registerFunction("time.getyear",
                (sc, in) -> (double) ((GregorianCalendar) in[0].get(sc)).get(Calendar.YEAR));
        registerFunction("time.getmonth",
                (sc, in) -> (double) (((GregorianCalendar) in[0].get(sc)).get(Calendar.MONTH) + 1));
        registerFunction("time.getday", (sc,
                in) -> (double) (((GregorianCalendar) in[0].get(sc)).get(Calendar.DAY_OF_MONTH)));
        registerFunction("time.gethour",
                (sc, in) -> (double) ((GregorianCalendar) in[0].get(sc)).get(Calendar.HOUR_OF_DAY));
        registerFunction("time.getminute",
                (sc, in) -> (double) ((GregorianCalendar) in[0].get(sc)).get(Calendar.MINUTE));
        registerFunction("time.getsecond",
                (sc, in) -> (double) ((GregorianCalendar) in[0].get(sc)).get(Calendar.SECOND));
        registerFunction("time.parse",
                (sc, in) -> {
                    String format = in[0].getString(sc);
                    String source = in[1].getString(sc);
                    try {
                        Date date = new SimpleDateFormat(format).parse(source);
                        GregorianCalendar cal = new GregorianCalendar();
                        cal.setTime(date);
                        return cal;
                    } catch(Exception ex) {
                        return null;
                    }
                });
        registerFunction("string.matches",
                (sc, in) -> in[0].getString(sc).matches(in[1].getString(sc)));
        registerFunction("string.number", (sc, in) -> SnuviUtils.toString(in[0].getDouble(sc)));
        registerFunction("string.class", (sc, in) -> in[0].get(sc).getClass().getSimpleName());
        registerFunction("string.tolowercase",
                (sc, in) -> SnuviUtils.connect(sc, in, 0).toLowerCase());
        registerFunction("string.touppercase",
                (sc, in) -> SnuviUtils.connect(sc, in, 0).toUpperCase());
        registerFunction("string.split",
                (sc, in) -> in[1].getString(sc).split(in[0].getString(sc)));
        registerFunction("string.concat", (sc, in) -> SnuviUtils.connect(sc, in, 0));
        registerFunction("string", (sc, in) -> String.valueOf(in[0].get(sc)));
        registerFunction("string.substring",
                (sc, in) -> in[0].getString(sc).substring(in[1].getInt(sc), in[2].getInt(sc)));
        registerFunction("string.length", (sc, in) -> (double) in[0].getString(sc).length());
        registerFunction("string.startswith",
                (sc, in) -> in[0].getString(sc).startsWith(in[1].getString(sc), in[2].getInt(sc)));
        registerFunction("string.endswith",
                (sc, in) -> in[0].getString(sc).endsWith(in[1].getString(sc)));
        registerFunction("string.contains",
                (sc, in) -> in[0].getString(sc).contains(in[1].getString(sc)));
        registerFunction("string.indexof", (sc,
                in) -> (double) in[0].getString(sc).indexOf(in[1].getString(sc), in[2].getInt(sc)));
        registerFunction("string.lastindexof", (sc, in) -> (double) in[0].getString(sc)
                .lastIndexOf(in[1].getString(sc), in[2].getInt(sc)));
        registerFunction("string.replace", (sc, in) -> in[0].getString(sc)
                .replaceAll(in[1].getString(sc), in[2].getString(sc)));
        registerFunction("string.trim", (sc, in) -> in[0].getString(sc).trim());
        registerFunction("string.charcode",
                (sc, in) -> (double) in[0].getString(sc).charAt(in[1].getInt(sc)));
        registerFunction("string.fromcode", (sc, in) -> String.valueOf((char) in[0].getInt(sc)));
        registerFunction("file.new", (sc, in) -> new File(in[0].getString(sc)));
        registerFunction("file.exists", (sc, in) -> ((File) in[0].get(sc)).exists());
        registerFunction("file.isfile", (sc, in) -> ((File) in[0].get(sc)).isFile());
        registerFunction("file.isdirectory", (sc, in) -> ((File) in[0].get(sc)).isDirectory());
        registerFunction("file.delete", (sc, in) -> ((File) in[0].get(sc)).delete());
        registerFunction("file.getname", (sc, in) -> ((File) in[0].get(sc)).getName());
        registerFunction("file.getlist",
                (sc, in) -> Arrays.asList(((File) in[0].get(sc)).listFiles()));
        registerFunction("file.read",
                (sc, in) -> Files.readAllLines(((File) in[0].get(sc)).toPath()));
        registerConsumer("file.write", (sc, in) -> {
            Path p = Paths.get(((File) in[0].get(sc)).toURI());
            Files.write(p, ((List<Object>) in[1].get(sc)).stream().map(o -> String.valueOf(o))
                    .collect(Collectors.toList()), StandardCharsets.UTF_8);
            try {
                Files.setPosixFilePermissions(p, FunctionRegistry.FILE_ACCESS);
            } catch(Exception ex) {
            }
        });
        registerConsumer("file.createfolder", (sc, in) -> {
            Path p = Paths.get(((File) in[0].get(sc)).toURI());
            Files.createDirectory(p, PosixFilePermissions.asFileAttribute(FILE_ACCESS));
            try {
                Files.setPosixFilePermissions(p, FunctionRegistry.FILE_ACCESS);
            } catch(Exception ex) {
            }
        });
        registerConsumer("file.rename", (sc, in) -> {
            ((File) in[0].get(sc)).renameTo((File) in[1].get(sc));
        });
        registerFunction("config.new",
                (sc, in) -> new SnuviConfig(in[0].getString(sc), in[1].getString(sc)));
        registerFunction("config.exists", (sc, in) -> ((SnuviConfig) in[0].get(sc)).exists());
        registerFunction("config.save", (sc, in) -> ((SnuviConfig) in[0].get(sc)).save(sc));
        registerConsumer("config.load", (sc, in) -> ((SnuviConfig) in[0].get(sc)).load(sc));
        registerFunction("config.delete", (sc, in) -> ((SnuviConfig) in[0].get(sc)).delete());
        registerConsumer("config.set",
                (sc, in) -> ((SnuviConfig) in[0].get(sc)).set(in[1].getString(sc), in[2].get(sc)));
        registerFunction("config.getbool", (sc, in) -> ((SnuviConfig) in[0].get(sc)).getBoolean(sc,
                in[1].getString(sc), in[2].getBoolean(sc)));
        registerFunction("config.getdouble", (sc, in) -> ((SnuviConfig) in[0].get(sc)).getDouble(sc,
                in[1].getString(sc), in[2].getDouble(sc)));
        registerFunction("config.getstring", (sc, in) -> ((SnuviConfig) in[0].get(sc)).getString(sc,
                in[1].getString(sc), in[2].getString(sc)));
        registerFunction("read.number", (sc, in) -> {
            try {
                return Double.parseDouble(in[0].getString(sc));
            } catch(NumberFormatException ex) {
                return null;
            }
        });
        registerFunction("+", (sc, in) -> in[0].getDouble(sc) + in[1].getDouble(sc));
        registerAlias("+", "add");
        registerFunction("-", (sc, in) -> in.length == 1 ? -in[0].getDouble(sc)
                : in[0].getDouble(sc) - in[1].getDouble(sc));
        registerAlias("-", "sub");
        registerFunction("*", (sc, in) -> in[0].getDouble(sc) * in[1].getDouble(sc));
        registerAlias("*", "mul");
        registerFunction("/", (sc, in) -> in[0].getDouble(sc) / in[1].getDouble(sc));
        registerAlias("/", "div");
        registerFunction("=", (sc, in) -> {
            Object o = in[1].get(sc);
            in[0].set(sc, o);
            return o;
        });
        registerFunction("+=", (sc, in) -> {
            Object o = in[0].getDouble(sc) + in[1].getDouble(sc);
            in[0].set(sc, o);
            return o;
        });
        registerFunction("p++", (sc, in) -> {
            double d = in[0].getDouble(sc);
            in[0].set(sc, d + 1.0);
            return d;
        });
        registerAlias("p++", "inc");
        registerFunction("++", (sc, in) -> {
            double d = in[0].getDouble(sc) + 1.0;
            in[0].set(sc, d);
            return d;
        });
        registerFunction("-=", (sc, in) -> {
            Object o = in[0].getDouble(sc) - in[1].getDouble(sc);
            in[0].set(sc, o);
            return o;
        });
        registerFunction("p--", (sc, in) -> {
            double d = in[0].getDouble(sc);
            in[0].set(sc, d - 1.0);
            return d;
        });
        registerAlias("p--", "dec");
        registerFunction("--", (sc, in) -> {
            double d = in[0].getDouble(sc) - 1.0;
            in[0].set(sc, d);
            return d;
        });
        registerFunction("*=", (sc, in) -> {
            Object o = in[0].getDouble(sc) * in[1].getDouble(sc);
            in[0].set(sc, o);
            return o;
        });
        registerFunction("/=", (sc, in) -> {
            Object o = in[0].getDouble(sc) / in[1].getDouble(sc);
            in[0].set(sc, o);
            return o;
        });
        registerFunction("%=", (sc, in) -> {
            Object o = (double) (in[0].getInt(sc) % in[1].getInt(sc));
            in[0].set(sc, o);
            return o;
        });
        registerFunction("<<=", (sc, in) -> {
            Object o = (double) (in[0].getInt(sc) << in[1].getInt(sc));
            in[0].set(sc, o);
            return o;
        });
        registerFunction(">>=", (sc, in) -> {
            Object o = (double) (in[0].getInt(sc) >> in[1].getInt(sc));
            in[0].set(sc, o);
            return o;
        });
        registerFunction("&=", (sc, in) -> {
            Object o = (double) (in[0].getInt(sc) & in[1].getInt(sc));
            in[0].set(sc, o);
            return o;
        });
        registerFunction("^=", (sc, in) -> {
            Object o = (double) (in[0].getInt(sc) ^ in[1].getInt(sc));
            in[0].set(sc, o);
            return o;
        });
        registerFunction("|=", (sc, in) -> {
            Object o = (double) (in[0].getInt(sc) | in[1].getInt(sc));
            in[0].set(sc, o);
            return o;
        });
        registerFunction("getvar", (sc, in) -> sc.getVar(in[0].getString(sc)).get(sc));
        registerConsumer("setvar",
                (sc, in) -> sc.getVar(in[0].getString(sc)).set(sc, in[1].get(sc)));
        registerConsumer("removevar", (sc, in) -> sc.getVar(in[0].getString(sc)).set(sc, null));
        registerConsumer("wait", (sc, in) -> sc.setWaiting());
        registerConsumer("goto", (sc, in) -> sc.gotoLabel(in[0].getString(sc), true));
        registerConsumer("ignoregoto", (sc, in) -> sc.gotoLabel(in[0].getString(sc), false));
        registerAlias("ignoregoto", "igoto");
        registerConsumer("sgoto", (sc, in) -> {
            int time = in[0].getInt(sc);
            if(time < 0) {
                throw new IllegalArgumentException("time units can't be negative");
            }
            String label = in[1].getString(sc);
            int line = sc.getLine();
            sc.getScriptManager().getScheduler().scheduleTask("sgoto",
                    new ScheduledGoto("sgoto", sc, label, line), time);
        });
        registerConsumer("gosub", (sc, in) -> sc.goSub(in[0].getString(sc)));
        registerFunction("==", (sc, in) -> Objects.equals(in[0].get(sc), in[1].get(sc)));
        registerAlias("==", "equal");
        registerAlias("==", "equals");
        registerFunction("!=", (sc, in) -> !Objects.equals(in[0].get(sc), in[1].get(sc)));
        registerAlias("!=", "notequal");
        registerFunction("<",
                (sc, in) -> ((Comparable) in[0].get(sc)).compareTo(in[1].get(sc)) < 0);
        registerAlias("<", "less");
        registerFunction(">",
                (sc, in) -> ((Comparable) in[0].get(sc)).compareTo(in[1].get(sc)) > 0);
        registerAlias(">", "greater");
        registerFunction("<=",
                (sc, in) -> ((Comparable) in[0].get(sc)).compareTo(in[1].get(sc)) <= 0);
        registerAlias("<=", "lessequal");
        registerFunction(">=",
                (sc, in) -> ((Comparable) in[0].get(sc)).compareTo(in[1].get(sc)) >= 0);
        registerAlias(">=", "greaterequal");
        registerFunction("!", (sc, in) -> !in[0].getBoolean(sc));
        registerAlias("!", "invert");
        registerFunction("&&", (sc, in) -> {
            for(InputProvider i : in) {
                if(!i.getBoolean(sc)) {
                    return false;
                }
            }
            return true;
        });
        registerAlias("&&", "and");
        registerFunction("||", (sc, in) -> {
            for(InputProvider i : in) {
                if(i.getBoolean(sc)) {
                    return true;
                }
            }
            return false;
        });
        registerAlias("||", "or");
        registerFunction("getscriptvar", (sc, in) -> GLOBAL_VARS.get(in[0].getString(sc)));
        registerFunction("setscriptvar",
                (sc, in) -> GLOBAL_VARS.put(in[0].getString(sc), in[1].get(sc)));
        registerFunction("delscriptvar", (sc, in) -> GLOBAL_VARS.remove(in[0].getString(sc)));
        registerConsumer("clearscriptvars", (sc, in) -> GLOBAL_VARS.clear());
        registerFunction("hasnext", (sc, in) -> ((Iterator) in[0].get(sc)).hasNext());
        registerFunction("next", (sc, in) -> ((Iterator) in[0].get(sc)).next());
        registerConsumer("remove", (sc, in) -> ((Iterator) in[0].get(sc)).remove());
        registerFunction("iterator", (sc, in) -> {
            Object o = in[0].get(sc);
            if(o instanceof Iterable) {
                return ((Iterable) o).iterator();
            }
            return new ArrayIterator((Object[]) in[0].get(sc));
        });
        registerConsumer("swap", (sc, in) -> {
            Object o = in[0].get(sc);
            in[0].set(sc, in[1].get(sc));
            in[1].set(sc, o);
        });
        registerConsumer("print", (sc, in) -> {
            sc.getScriptManager().getLogger().print(SnuviUtils.connect(sc, in, 0), null, "print",
                    sc.getName(), sc, sc.getStackTrace());
        });
        registerConsumer("waitfor", (sc, in) -> {
            long l = in[0].getInt(sc);
            if(l < 0) {
                throw new IllegalArgumentException("time units can't be negative");
            }
            sc.setHolded(true);
            sc.setWaiting();
            sc.getScriptManager().getScheduler().scheduleTask("waitfor", () -> {
                if(sc.shouldTerm()) {
                    return;
                }
                sc.setHolded(false);
                sc.run();
                if(sc.shouldTerm()) {
                    sc.getScriptManager().removeScript(sc);
                }
            }, l);
        });
        registerConsumer("term", (sc, in) -> {
            sc.term();
            sc.getScriptManager().removeScript(sc);
        });
        registerFunction("isbool", (sc, in) -> (in[0].get(sc) instanceof Boolean));
        registerFunction("isdouble", (sc, in) -> (in[0].get(sc) instanceof Double));
        registerFunction("islong", (sc, in) -> {
            Object o = in[0].get(sc);
            if(o instanceof Double) {
                double d = (Double) o;
                return d == (long) d;
            }
            return false;
        });
        registerConsumer("assert", (sc, in) -> {
            if(!in[0].getBoolean(sc)) {
                throw new IllegalArgumentException("assertion failed");
            }
        });
        registerFunction("class", (sc, in) -> in[0].get(sc).getClass());
        registerFunction("usedmemory", (sc, in) -> {
            Runtime runtime = Runtime.getRuntime();
            double usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1048576;
            return usedMemory;
        });
        registerFunction("allocatedmemory",
                (sc, in) -> Runtime.getRuntime().totalMemory() / 1048576.0);
    }
}
