package me.hammerle.snuviscript.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.TreeMap;
import java.util.stream.Collectors;
import me.hammerle.snuviscript.code.FunctionRegistry;
import me.hammerle.snuviscript.code.Script;
import me.hammerle.snuviscript.code.ScriptManager;
import me.hammerle.snuviscript.code.SnuviUtils;
import me.hammerle.snuviscript.exceptions.StackTrace;

public class SnuviConfig {
    private final TreeMap<String, Object> conf = new TreeMap<>();
    private final File file;
    private boolean dirty = false;

    public SnuviConfig(String path, String name) {
        StringBuilder sb = new StringBuilder("./");
        sb.append(path);
        sb.append("/");
        sb.append(name);
        sb.append(".snuvic");
        file = new File(sb.toString());
    }

    private void print(Script sc, String message, Exception ex) {
        if(sc == null) {
            System.out.println(message);
            ex.printStackTrace();
            return;
        }
        ScriptManager sm = sc.getScriptManager();
        final StackTrace trace = sc.getStackTrace();
        sm.getScheduler().scheduleTask("config_print", () -> {
            sm.getLogger().print(message, ex, null, sc.getName(), sc, trace);
        });
    }

    private void print(Script sc, String message) {
        print(sc, message, null);
    }

    public final synchronized void load(Script sc) {
        if(!exists()) {
            print(sc, "cannot load non existent file '" + file.getPath() + "'");
            return;
        }
        try {
            String warning = "wrong syntax in '" + file.getPath() + "'";
            Files.readAllLines(file.toPath()).stream().forEach(s -> {
                int b = s.indexOf("=");
                if(b == -1) {
                    print(sc, warning);
                    print(sc, s);
                } else {
                    conf.put(s.substring(0, b).trim(), SnuviUtils.convert(s.substring(b + 1)));
                }
            });
        } catch(MalformedInputException ex) {
            print(sc,
                    "'" + file.getPath() + "' contains an illegal character, change file encoding",
                    ex);
        } catch(OutOfMemoryError ex) {
            print(sc, "'" + file.getPath() + "' is too big");
        } catch(SecurityException ex) {
            print(sc, "'" + file.getPath() + "' is not accessable", ex);
        } catch(IOException ex) {
            print(sc, "'" + file.getPath() + "' cannot be read", ex);
        }
    }

    public final boolean exists() {
        return file.exists();
    }

    public final synchronized boolean delete() {
        return file.delete();
    }

    public final synchronized boolean save(Script sc) {
        if(conf.isEmpty() || !dirty) {
            return false;
        }
        dirty = false;
        try {
            Path p = Paths.get(file.toURI());
            Files.write(p, conf.entrySet().stream().map(e -> {
                if(e.getValue().getClass() == String.class) {
                    return String.format("%s=\"%s\"", e.getKey(),
                            e.getValue().toString().replaceAll("\n", "\\n"));
                }
                return String.format("%s=%s", e.getKey(), e.getValue());
            }).collect(Collectors.toList()), StandardCharsets.UTF_8);
            try {
                Files.setPosixFilePermissions(p, FunctionRegistry.FILE_ACCESS);
            } catch(Exception ex) {
            }
            return true;
        } catch(UnsupportedOperationException ex) {
            print(sc, "an unsupported operation was used", ex);
            return false;
        } catch(SecurityException ex) {
            print(sc, "'" + file.getPath() + "' is not accessable", ex);
            return false;
        } catch(IOException ex) {
            print(sc, "cannot write to '" + file.getPath() + "'", ex);
            return false;
        }
    }

    public final synchronized <T> T get(Script sc, String key, Class<T> c, T error) {
        try {
            Object o = conf.get(key);
            if(o == null) {
                return error;
            }
            return c.cast(o);
        } catch(ClassCastException ex) {
            print(sc, "invalid get", ex);
            return error;
        }
    }

    public final String getString(Script sc, String key, String error) {
        return get(sc, key, String.class, error);
    }

    public final String getString(Script sc, String key) {
        return getString(sc, key, null);
    }

    public final float getFloat(Script sc, String key, float error) {
        return get(sc, key, Double.class, (double) error).floatValue();
    }

    public final double getDouble(Script sc, String key, double error) {
        return get(sc, key, Double.class, error);
    }

    public final long getLong(Script sc, String key, long error) {
        return get(sc, key, Double.class, (double) error).longValue();
    }

    public final int getInt(Script sc, String key, int error) {
        return get(sc, key, Double.class, (double) error).intValue();
    }

    public final boolean getBoolean(Script sc, String key, boolean error) {
        return get(sc, key, Boolean.class, error);
    }

    public final synchronized void set(String key, Object o) {
        dirty = true;
        conf.put(key, o);
    }
}
