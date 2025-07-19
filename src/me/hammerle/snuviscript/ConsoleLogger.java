package me.hammerle.snuviscript;

import me.hammerle.snuviscript.code.ISnuviLogger;
import me.hammerle.snuviscript.code.Script;
import me.hammerle.snuviscript.exceptions.StackTrace;

public class ConsoleLogger implements ISnuviLogger {
    @Override
    public void print(String message, Exception ex, String function, String scriptname, Script sc,
            StackTrace lines) {
        StringBuilder sb = new StringBuilder();

        if(ex == null) {
            sb.append("debug: '");
            sb.append(message);
            sb.append("'");
        } else {
            sb.append(ex.getClass().getSimpleName());
            sb.append(": '");
            sb.append(ex.getMessage());
            if(message != null && !message.isEmpty()) {
                sb.append(" - ");
                sb.append(message);
            }
            sb.append("'");
        }

        if(scriptname != null && !scriptname.isEmpty()) {
            sb.append(" in script '");
            sb.append(scriptname.replace(".snuvi", ""));
            sb.append("'");
        }

        if(sc != null) {
            sb.append(" id '");
            sb.append(sc.getId());
            sb.append("'");
        }

        if(function != null && !function.isEmpty()) {
            sb.append(" in function '");
            sb.append(function);
            sb.append("'");
        }

        if(lines != null) {
            sb.append(" at '");
            sb.append(lines.toString());
            sb.append("'");
        }

        System.out.println(sb.toString());
    }

}
