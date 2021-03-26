package me.hammerle.snuviscript;

import me.hammerle.snuviscript.code.ScriptManager;

public class SnuviScript {
    public static void main(String[] args) {
        if(args.length == 0) {
            System.out.println("java -jar SnuviScriptRecoded.jar <file_1> [file_2] ...");
            return;
        } else if(args[0].equals("test")) {
            me.hammerle.snuviscript.test.Test.test();
            return;
        }
        ConsoleScheduler cs = new ConsoleScheduler();
        ScriptManager sm = new ScriptManager(new ConsoleLogger(), cs);
        sm.startScript(args[0], args);
        cs.tick();
    }
}
