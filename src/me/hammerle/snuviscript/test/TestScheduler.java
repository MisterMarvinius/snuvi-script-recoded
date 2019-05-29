package me.hammerle.snuviscript.test;

import java.util.LinkedList;
import me.hammerle.snuviscript.code.ISnuviScheduler;

public class TestScheduler implements ISnuviScheduler
{
    private final LinkedList<Runnable> list = new LinkedList<>();
    
    @Override
    public int scheduleTask(Runnable r, long delay)
    {
        list.add(r);
        return 0;
    }
    
    public void execute()
    {
        while(!list.isEmpty())
        {
            list.removeFirst().run();
        }
    }
}
