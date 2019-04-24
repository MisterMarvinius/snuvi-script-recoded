package me.hammerle.snuviscript.test;

import me.hammerle.snuviscript.code.ISnuviScheduler;

public class TestScheduler implements ISnuviScheduler
{
    @Override
    public int scheduleTask(Runnable r)
    {
        System.out.println("Schedule");
        return 0;
    }

    @Override
    public int scheduleTask(Runnable r, long delay)
    {
        return 0;
    }
    
}
