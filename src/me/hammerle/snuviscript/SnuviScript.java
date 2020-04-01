package me.hammerle.snuviscript;

import me.hammerle.snuviscript.code.ScriptManager;

public class SnuviScript {
    public static void main(String[] args) {
        me.hammerle.snuviscript.test.Test.test();
        //startForConsole(args);
    }

    private static void startForConsole(String[] args) {
        if(args.length == 0) {
            System.out.println("java -jar SnuviScriptRecoded.jar <file_1> [file_2] ...");
            return;
        }
        ConsoleScheduler cs = new ConsoleScheduler();
        ScriptManager sm = new ScriptManager(new ConsoleLogger(), cs);
        sm.startScript(true, args[0], args);
        cs.tick();
    }
}
