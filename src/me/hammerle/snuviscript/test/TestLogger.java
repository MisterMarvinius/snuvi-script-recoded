package me.hammerle.snuviscript.test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import me.hammerle.snuviscript.code.ISnuviLogger;
import me.hammerle.snuviscript.code.Script;

public class TestLogger implements ISnuviLogger
{
    private final ArrayList<String> list = new ArrayList<>();
    
    @Override
    public void print(String message, Exception ex, String function, String scriptname, Script sc, int line)
    {
        if(ex == null)
        {
            list.add(message);
        }
        else
        {
            System.out.println(ex);
            System.out.println(ex.getMessage());
            System.out.println("error line " + line);
        }
    }
    
    public void reset()
    {
        list.clear();
    }
    
    public boolean check(File f)
    {
        if(!f.exists())
        {
            System.out.println(String.format("\"%s\" does not exist", f.getPath()));
            return false;
        }
        try
        {
            List<String> file = Files.readAllLines(f.toPath());
            if(file.size() != list.size())
            {
                printNoMatch(f, file);
                return false;
            }
            for(int i = 0; i < file.size(); i++)
            {
                if(!file.get(i).equals(list.get(i)))
                {
                    printNoMatch(f, file);
                    return false;
                }
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            return false;
        }
        return true;
    }
    
    private void printNoMatch(File f, List<String> file)
    {
        System.out.println(String.format("error checking %s ", f.getPath()));
        System.out.println("Expected ----------------------------------------");
        file.forEach(s -> System.out.println(s));
        System.out.println("Actual ------------------------------------------");
        list.forEach(s -> System.out.println(s));
        System.out.println("-------------------------------------------------");
    }
}
