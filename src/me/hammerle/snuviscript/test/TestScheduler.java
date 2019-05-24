package me.hammerle.snuviscript.test;

import java.util.ArrayList;
import me.hammerle.snuviscript.code.ISnuviScheduler;

public class TestScheduler implements ISnuviScheduler
{
    private final ArrayList<Runnable> list = new ArrayList<>();
    
    @Override
    public int scheduleTask(Runnable r, long delay)
    {
        list.add(r);
        return 0;
    }
    
    public void execute()
    {
        list.forEach(r -> r.run());
    }
}
