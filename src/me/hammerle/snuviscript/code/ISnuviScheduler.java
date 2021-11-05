package me.hammerle.snuviscript.code;

public interface ISnuviScheduler {
    public default void scheduleTask(String name, Runnable r) {
        scheduleTask(name, r, 0);
    }

    public void scheduleTask(String name, Runnable r, long delay);
}
