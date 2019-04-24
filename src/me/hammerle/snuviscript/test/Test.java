package me.hammerle.snuviscript.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.function.BiConsumer;
import me.hammerle.snuviscript.code.SnuviParser;
import me.hammerle.snuviscript.token.Tokenizer;
import me.hammerle.snuviscript.token.Token;

public class Test
{
    private static final TestScheduler SCHEDULER = new TestScheduler();
    private static final TestLogger LOGGER = new TestLogger();
    private static final SnuviParser PARSER = new SnuviParser(LOGGER, SCHEDULER);
    private static int done = 0;
    private static int tests = 0;  
    
    public static void test()
    {
        testTokenizer();
        testOutput();
    }
    
    private static void testOutput()
    {
        done = 0;
        tests = 0;  
        forEachFile(new File("./test"), ".out", (inFile, checkFile) -> 
        {
            tests++;
                
            LOGGER.reset();
            PARSER.startScript(true, "", inFile.getPath());

            if(LOGGER.check(checkFile))
            {
                done++;
            }
        });
        System.out.println(String.format("%d / %d output tests succeeded", done, tests));
    }
    
    private static void testTokenizer()
    {
        done = 0;
        tests = 0; 
        forEachFile(new File("./test"), ".tout", (inFile, checkFile) -> 
        {
            try
            {
                try(FileInputStream in = new FileInputStream(inFile))
                {
                    tests++; 
                    Tokenizer tokenizer = new Tokenizer();
                    LOGGER.reset();
                    for(Token t : tokenizer.tokenize(in))
                    {
                        LOGGER.print(t.toString(), null, null, null, null, -1);
                    }
                    if(LOGGER.check(checkFile))
                    {
                        done++;
                    }
                }
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }
        });
        System.out.println(String.format("%d / %d tokenizer tests succeeded", done, tests));
    }
    
    private static void forEachFile(File f, String ending, BiConsumer<File, File> bc)
    {
        if(f.isFile())
        {
            if(!f.getName().contains("."))
            {
                File checkFile = new File(f.getPath() + ending);
                if(!checkFile.exists())
                {
                    try
                    {
                        checkFile.createNewFile();
                    }
                    catch(IOException ex)
                    {
                    }
                }
                bc.accept(f, checkFile);
            }
        }
        else if(f.isDirectory())
        {
            for(File fi : f.listFiles())
            {
                forEachFile(fi, ending, bc);
            }
        }
    }
}
