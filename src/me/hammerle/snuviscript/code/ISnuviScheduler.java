package me.hammerle.snuviscript.code;

public interface ISnuviScheduler {
    public default int scheduleTask(Runnable r) {
        return scheduleTask(r, 0);
    }

    public int scheduleTask(Runnable r, long delay);
}
