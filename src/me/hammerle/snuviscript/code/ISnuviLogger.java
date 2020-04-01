package me.hammerle.snuviscript.code;

import me.hammerle.snuviscript.exceptions.StackTrace;

public interface ISnuviLogger {
    public void print(String message, Exception ex, String function, String scriptname, Script sc, StackTrace lines);

    public default void print(Exception ex) {
        print(null, ex, null, null, null, null);
    }

    public default void print(String message) {
        print(message, null, null, null, null, null);
    }
}
