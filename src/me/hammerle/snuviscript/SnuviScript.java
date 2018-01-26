package me.hammerle.snuviscript;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import me.hammerle.snuviscript.code.ISnuviLogger;
import me.hammerle.snuviscript.code.ISnuviScheduler;
import me.hammerle.snuviscript.code.Script;
import me.hammerle.snuviscript.code.SnuviParser;

public class SnuviScript
{
    public static void main(String[] args) throws IOException 
    {
        ISnuviLogger logger = new ISnuviLogger() 
        {
            @Override
            public void print(String message, Exception ex, String function, String scriptname, Script sc, int line) 
            {
                System.out.println("________________________________________");
                System.out.println("Exception");
                if(message != null)
                {
                    System.out.println(" - " + message);
                }
                if(ex != null)
                {
                    System.out.println(" - " + ex.getClass().getSimpleName());
                }
                if(function != null)
                {
                    System.out.println(" - Funktion: " + function);
                }
                if(scriptname != null)
                {
                    System.out.println(" - Script: " + scriptname);
                }
                if(line != -1)
                {
                    System.out.println(" - Line: " + line);
                }
            }
        };
        ISnuviScheduler scheduler = new ISnuviScheduler() 
        {
            @Override
            public int scheduleTask(Runnable r) 
            {
                System.out.println("SCHEDULER");
                return 0;
            }

            @Override
            public int scheduleTask(Runnable r, long delay) 
            {
                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();  
                executor.schedule(r, delay, TimeUnit.MILLISECONDS);
                executor.shutdown();
                return 1;
            }
        };
        SnuviParser parser = new SnuviParser(logger, scheduler);
        parser.startScript("./test", ".sbasic", true);
        
        parser.callEvent("testevent", null, null);
    }  
}
