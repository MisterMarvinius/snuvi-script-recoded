package me.hammerle.snuviscript.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.TreeMap;
import java.util.stream.Collectors;
import me.hammerle.snuviscript.code.ISnuviLogger;
import me.hammerle.snuviscript.code.Script;
import me.hammerle.snuviscript.code.SnuviUtils;

public class SnuviConfig {
    protected final ISnuviLogger logger;
    protected final TreeMap<String, Object> conf;
    private final File file;
    private Script sc;

    private SnuviConfig(Script sc, ISnuviLogger logger, String path, String name) {
        this.sc = sc;
        this.logger = logger;
        StringBuilder sb = new StringBuilder("./");
        sb.append(path);
        sb.append("/");
        sb.append(name);
        sb.append(".snuvic");
        file = new File(sb.toString());
        conf = new TreeMap<>();
    }

    public SnuviConfig(ISnuviLogger logger, String path, String name) {
        this(null, logger, path, name);
    }

    public SnuviConfig(Script sc, String path, String name) {
        this(sc, sc.getScriptManager().getLogger(), path, name);
    }

    private void print(String message, Exception ex) {
        logger.print(message, ex, null, sc == null ? null : sc.getName(), sc, sc == null ? null : sc.getStackTrace());
    }

    private void print(String message) {
        print(message, null);
    }

    public final void load() {
        if(!exists()) {
            print("cannot load non existent file '" + file.getPath() + "'");
            return;
        }
        try {
            String warning = "wrong syntax in '" + file.getPath() + "'";
            Files.readAllLines(file.toPath()).stream().forEach(s -> {
                int b = s.indexOf("=");
                if(b == -1) {
                    print(warning);
                    print(s);
                } else {
                    conf.put(s.substring(0, b).trim(), SnuviUtils.convert(s.substring(b + 1)));
                }
            });
        } catch(MalformedInputException ex) {
            print("'" + file.getPath() + "' contains an illegal character, change file encoding", ex);
        } catch(OutOfMemoryError ex) {
            print("'" + file.getPath() + "' is too big");
        } catch(SecurityException ex) {
            print("'" + file.getPath() + "' is not accessable", ex);
        } catch(IOException ex) {
            print("'" + file.getPath() + "' cannot be read", ex);
        }
    }

    public final boolean exists() {
        return file.exists();
    }

    public final boolean delete() {
        return file.delete();
    }

    public final boolean save() {
        try {
            if(file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            if(!file.exists()) {
                try {
                    file.createNewFile();
                } catch(IOException ex) {
                    print("'" + file.getPath() + "' cannot be created", ex);
                    return false;
                }
            }
            Files.write(Paths.get(file.toURI()), conf.entrySet().stream()
                    .map(e -> {
                        if(e.getValue().getClass() == String.class) {
                            return e.getKey() + "=\"" + e.getValue() + "\"";
                        }
                        return e.getKey() + "=" + String.valueOf(e.getValue());
                    })
                    .collect(Collectors.toList()), StandardCharsets.UTF_8);
            return true;
        } catch(UnsupportedOperationException ex) {
            print("an unsupported operation was used", ex);
            return false;
        } catch(SecurityException ex) {
            print("'" + file.getPath() + "' is not accessable", ex);
            return false;
        } catch(IOException ex) {
            print("cannot write to '" + file.getPath() + "'", ex);
            return false;
        }
    }

    public final <T> T get(String key, Class<T> c, T error) {
        try {
            Object o = conf.get(key);
            if(o == null) {
                return error;
            }
            return c.cast(o);
        } catch(ClassCastException ex) {
            print("invalid get", ex);
            return error;
        }
    }

    public final String getString(String key, String error) {
        return get(key, String.class, error);
    }

    public final String getString(String key) {
        return getString(key, null);
    }

    public final float getFloat(String key, float error) {
        return get(key, Double.class, (double) error).floatValue();
    }

    public final double getDouble(String key, double error) {
        return get(key, Double.class, error);
    }

    public final long getLong(String key, long error) {
        return get(key, Double.class, (double) error).longValue();
    }

    public final int getInt(String key, int error) {
        return get(key, Double.class, (double) error).intValue();
    }

    public final boolean getBoolean(String key, boolean error) {
        return get(key, Boolean.class, error);
    }

    public final void set(String key, Object o) {
        conf.put(key, o);
    }
}
