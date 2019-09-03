package me.hammerle.snuviscript;

import me.hammerle.snuviscript.code.ISnuviScheduler;

public class ConsoleScheduler implements ISnuviScheduler
{
    @Override
    public int scheduleTask(Runnable r, long delay)
    {
        System.out.println("Scheduling is currently not supported in this environment.");
        return -1;
    }
    
}
