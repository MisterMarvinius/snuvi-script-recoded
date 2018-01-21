package me.hammerle.snuviscript;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import me.hammerle.snuviscript.code.Script;

public class SnuviScript
{
    public static void main(String[] args) throws IOException 
    {
        List<String> lines = Files.readAllLines(new File("./test.sbasic").toPath());
        Script sc = new Script(lines);
        System.out.println("\n" + sc.run());
    }  
}
