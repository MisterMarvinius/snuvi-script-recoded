package me.hammerle.snuviscript.code;

public interface ISnuviScheduler {
    public default void scheduleTask(Runnable r) {
        scheduleTask(r, 0);
    }

    public void scheduleTask(Runnable r, long delay);
}
