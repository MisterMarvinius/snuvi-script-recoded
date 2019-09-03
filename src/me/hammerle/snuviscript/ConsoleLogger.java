package me.hammerle.snuviscript;

import me.hammerle.snuviscript.code.ISnuviLogger;
import me.hammerle.snuviscript.code.Script;

public class ConsoleLogger implements ISnuviLogger
{
    @Override
    public void print(String message, Exception ex, String function, String scriptname, Script sc, int line)
    {
        StringBuilder sb = new StringBuilder();

        if(ex == null)
        {
            sb.append("debug: '");
            sb.append(message);
            sb.append("'");
        }
        else
        {
            sb.append(ex.getClass().getSimpleName());
            sb.append(": '");
            sb.append(ex.getMessage());
            if(message != null && !message.isEmpty())
            {
                sb.append(" - ");
                sb.append(message);
            }
            sb.append("'");
        }
        
        if(scriptname != null && !scriptname.isEmpty())
        {
            sb.append(" in script '");
            sb.append(scriptname);
            sb.append("'");
        }
        
        if(sc != null)
        {
            sb.append(" id '");
            sb.append(sc.getId());
            sb.append("'");
        }
        
        if(function != null && !function.isEmpty())
        {
            sb.append(" in function '");
            sb.append(function);
            sb.append("'");
        }
        
        if(line != -1)
        {
            sb.append(" in line '");
            sb.append(line);
            sb.append("'");
        }
        
        System.out.println(sb.toString());
    }
    
}
