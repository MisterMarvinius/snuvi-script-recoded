package me.hammerle.snuviscript.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.function.BiConsumer;
import me.hammerle.snuviscript.code.Script;
import me.hammerle.snuviscript.code.ScriptManager;
import me.hammerle.snuviscript.tokenizer.Tokenizer;
import me.hammerle.snuviscript.tokenizer.Token;
import me.hammerle.snuviscript.code.Compiler;
import me.hammerle.snuviscript.instructions.Instruction;

public class Test {
    private static final TestScheduler SCHEDULER = new TestScheduler();
    private static final TestLogger LOGGER = new TestLogger();
    private static final ScriptManager PARSER = new ScriptManager(LOGGER, SCHEDULER);
    private static int done = 0;
    private static int tests = 0;

    public static void test() {
        testTokenizer();
        testCompiler();
        testOutput();
        
        LOGGER.reset();
        PARSER.startScript(true, "test", "./test/test.test");
        LOGGER.printAll();
    }

    private static void testOutput() {
        done = 0;
        tests = 0;
        forEachFile(new File("./test"), ".out", (inFile, checkFile) -> {
            tests++;

            LOGGER.reset();

            Script sc = new Script(PARSER, null, null, inFile.getName(), inFile.getPath());
            sc.run();

            if(LOGGER.check(checkFile)) {
                done++;
            }
        });
        System.out.println(String.format("%d / %d output tests succeeded", done, tests));
    }

    private static void testTokenizer() {
        done = 0;
        tests = 0;
        forEachFile(new File("./test"), ".tout", (inFile, checkFile) -> {
            try {
                try(FileInputStream in = new FileInputStream(inFile)) {
                    tests++;
                    Tokenizer tokenizer = new Tokenizer();
                    LOGGER.reset();
                    for(Token t : tokenizer.tokenize(in)) {
                        LOGGER.print(t.toString(), null, null, null, null, null);
                    }
                    if(LOGGER.check(checkFile)) {
                        done++;
                    }
                }
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        });
        System.out.println(String.format("%d / %d tokenizer tests succeeded", done, tests));
    }

    private static void testCompiler() {
        done = 0;
        tests = 0;
        final Compiler c = new Compiler();
        forEachFile(new File("./test"), ".cout", (inFile, checkFile) -> {
            tests++;
            try {
                try(FileInputStream in = new FileInputStream(inFile)) {
                    Tokenizer tokenizer = new Tokenizer();
                    LOGGER.reset();
                    Instruction[] instr = c.compile(tokenizer.tokenize(in),
                            new HashMap<>(), new HashMap<>(), new HashMap<>(),
                            new HashMap<>());
                    for(Instruction i : instr) {
                        LOGGER.print(i.toString(), null, null, null, null, null);
                    }
                    if(LOGGER.check(checkFile)) {
                        done++;
                    }
                }
            } catch(Exception ex) {
                System.out.println("_________________________________________");
                System.out.println(inFile + " failed:");
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            }
        });
        System.out.println(String.format("%d / %d compiler tests succeeded", done, tests));
    }

    private static void forEachFile(File f, String ending, BiConsumer<File, File> bc) {
        if(f.isFile()) {
            if(!f.getName().contains(".")) {
                File checkFile = new File(f.getPath() + ending);
                if(!checkFile.exists()) {
                    try {
                        checkFile.createNewFile();
                    } catch(IOException ex) {
                    }
                }
                bc.accept(f, checkFile);
            }
        } else if(f.isDirectory()) {
            for(File fi : f.listFiles()) {
                forEachFile(fi, ending, bc);
            }
        }
    }
}
