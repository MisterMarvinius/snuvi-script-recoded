package me.hammerle.snuviscript.code;

public interface ISnuviScheduler 
{
    public int scheduleTask(Runnable r);  
    public int scheduleTask(Runnable r, long delay);
}
