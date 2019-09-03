package me.hammerle.snuviscript;

import me.hammerle.snuviscript.code.ScriptManager;

public class SnuviScript
{
    public static void main(String[] args)
    {
        me.hammerle.snuviscript.test.Test.test();
        
        //ScriptManager sm = new ScriptManager(new ConsoleLogger(), new ConsoleScheduler());
        //sm.startScript(true, args[0], args);
    }  
}