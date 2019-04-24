package me.hammerle.snuviscript.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import me.hammerle.snuviscript.code.SnuviParser;
import me.hammerle.snuviscript.token.Tokenizer;
import me.hammerle.snuviscript.compiler.Compiler;
import me.hammerle.snuviscript.exceptions.PreScriptException;

public class Test
{
    private static final TestScheduler SCHEDULER = new TestScheduler();
    private static final TestLogger LOGGER = new TestLogger();
    private static final SnuviParser PARSER = new SnuviParser(LOGGER, SCHEDULER);
    private static int done = 0;
    private static int tests = 0;    
    
    public static void test()
    {
        //genTests(5, 15, "functions");
        findTestFiles(new File("./test"));
        System.out.println(String.format("%d / %d tests succeeded", done, tests));
    }
    
    public static void testNew()
    {
        findTestFilesNew(new File("./test"));
    }
    
    private static void genTests(int from, int to, String name)
    {
        for(int i = from; i < to; i++)
        {
            try
            {
                File f = new File(String.format("./test/%s/%s%d", name, name, i));
                f.createNewFile();
                
                f = new File(String.format("./test/%s/%s%d.out", name, name, i));
                f.createNewFile();
            }
            catch(Exception ex)
            {
            }
        }
    }
    
    private static void findTestFiles(File f)
    {
        if(f.isFile())
        {
            if(!f.getName().endsWith(".out"))
            {
                tests++;
                
                LOGGER.reset();
                PARSER.startScript(true, "", f.getPath());
                
                if(LOGGER.check(new File(f.getPath() + ".out")))
                {
                    done++;
                }
            }
        }
        else if(f.isDirectory())
        {
            for(File fi : f.listFiles())
            {
                findTestFiles(fi);
            }
        }
    }
    
    private static void findTestFilesNew(File f)
    {
        if(f.isFile())
        {
            if(!f.getName().endsWith(".out"))
            {
                System.out.println("_________________________________________________");
                System.out.println(String.format("Tokenize \"%s\"", f.getPath()));
                try
                {
                    try(FileInputStream in = new FileInputStream(f))
                    {
                        Tokenizer tokenizer = new Tokenizer();
                        Compiler c = new Compiler();
                        c.checkSyntax(tokenizer.tokenize(in));
                    }
                }
                catch(IOException ex)
                {
                    ex.printStackTrace();
                }
                catch(PreScriptException ex)
                {
                    //ex.printStackTrace();
                    System.out.println(ex.getMessage());
                    System.out.println(ex.getStartLine());
                    System.out.println(ex.getEndLine());
                }
            }
        }
        else if(f.isDirectory())
        {
            for(File fi : f.listFiles())
            {
                findTestFilesNew(fi);
            }
        }
    }
}
