package me.hammerle.snuviscript;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import me.hammerle.snuviscript.code.ISnuviLogger;
import me.hammerle.snuviscript.code.ISnuviScheduler;
import me.hammerle.snuviscript.code.Script;
import me.hammerle.snuviscript.code.SnuviParser;
import me.hammerle.snuviscript.code.SnuviUtils;
import me.hammerle.snuviscript.exceptions.PreScriptException;
import me.hammerle.snuviscript.token.Tokenizer;

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
        parser.startScript(true, ".sbasic", "./test");  
        
        try
        {
            List<String> list = SnuviUtils.readCode(".sbasic", "./test");           
            Tokenizer t = new Tokenizer(String.join("\n", list));
            t.tokenize();
        }
        catch(PreScriptException ex)
        {
            System.out.println(ex);
            System.out.println(ex.getStartLine() + "   " + ex.getEndLine());
            ex.printStackTrace();
        }
        
        //System.out.println("spawn - " + conf.getLocation("spawn"));
        //parser.callEvent("testevent", null, null);      
    }  
    
    /*public static Location getSpawn()
    {
        // Player changeDimension, WorldServer, MinecraftServer
        return KajetansMod.conf.getLocation("spawn");
    }

    public static void setSpawn(World w, Vec3d v, float yaw, float pitch)
    {          
        SimpleConfig conf = KajetansMod.conf;
        conf.setLocation("spawn", new Location(w, v, yaw, pitch));
        conf.save();
        w.setSpawnPoint(new BlockPos(v.x, v.y, v.z));
    }*/
}
