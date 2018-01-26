package me.hammerle.snuviscript.code;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import me.hammerle.snuviscript.exceptions.PreScriptException;

public class Utils 
{
    private static final Random RANDOM = new Random();
    
    public static int randomInt(int min, int max)
    {
        return RANDOM.nextInt((max - min) + 1) + min;
    }
    
    // - in the number is handled somewhere else
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^[0-9]*[.]{0,1}[0-9]*");
    
    public static boolean isNumber(String s)
    {
        return NUMBER_PATTERN.matcher(s).matches();
    }
    
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("^[a-zA-Z.]*\\(.*\\)");
    
    public static boolean isFunction(String s)
    {
        return FUNCTION_PATTERN.matcher(s).matches();
    }
    
    private static final Pattern ARRAY_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*\\[[^\\]]*\\]");
    
    public static boolean isArray(String s)
    {
        return ARRAY_PATTERN.matcher(s).matches();
    }
    
    // -------------------------------------------------------------------------
    // line splitter
    // -------------------------------------------------------------------------
      
    private static void addNonEmptyString(HashMap<String, String> strings, LinkedList<String> list, String s)
    {
        s = s.trim();
        if(!s.isEmpty())
        {
            if(s.startsWith("#"))
            {
                String text = strings.get(s);
                if(text != null)
                {
                    list.add(text);
                    return;
                }
            }
            list.add(s);
        }
    }
    
    private static int findNextClosingBracket(int pos, StringBuilder sb, int line)
    {
        int brackets = 0;
        int length = sb.length();
        while(pos < length)
        {
            switch(sb.charAt(pos))
            {
                case ')':
                    brackets--;
                    if(brackets == 0)
                    {
                        return pos;
                    }
                    else if(brackets < 0)
                    {
                        throw new PreScriptException(") without (", line);
                    }
                    break;
                case '(':
                    brackets++;
                    break;
            }
            pos++;
        }
        throw new PreScriptException("( without )", line);
    }
    
    private static int findNextClosingSBracket(int pos, StringBuilder sb, int line)
    {
        int brackets = 0;
        int length = sb.length();
        while(pos < length)
        {
            switch(sb.charAt(pos))
            {
                case ']':
                    brackets--;
                    if(brackets == 0)
                    {
                        return pos;
                    }
                    else if(brackets < 0)
                    {
                        throw new PreScriptException("] without [", line);
                    }
                    break;
                case '[':
                    brackets++;
                    break;
            }
            pos++;
        }
        throw new PreScriptException("[ without ]", line);
    }
    
    public static String[] split(HashMap<String, String> strings, String s, int line)
    {
        LinkedList<String> list = new LinkedList<>();
        
        int old = 0;
        int pos = 0;
        
        StringBuilder sb = new StringBuilder(s);
        int length = sb.length();
        char c;
        while(pos < length)
        {
            c = sb.charAt(pos);
            if(!Character.isLetterOrDigit(c))
            {
                switch(c)
                {
                    case '_':
                    case '.':
                    case '#':
                    case '@':
                        break;
                    case ')':
                        throw new PreScriptException(") without (", line);
                    case '(':   
                        pos = findNextClosingBracket(pos, sb, line) + 1;
                        addNonEmptyString(strings, list, sb.substring(old, pos));
                        old = pos;
                        continue;
                    case ']':
                        throw new PreScriptException("] without [", line);
                    case '[':   
                        pos = findNextClosingSBracket(pos, sb, line) + 1;
                        addNonEmptyString(strings, list, sb.substring(old, pos));
                        old = pos;
                        continue;
                    case '\t':
                    case ' ':
                        addNonEmptyString(strings, list, sb.substring(old, pos));
                        old = pos + 1;
                        pos = old;
                        continue;
                    case ',':   
                        addNonEmptyString(strings, list, sb.substring(old, pos));
                        addNonEmptyString(strings, list, ",");
                        old = pos + 1;
                        pos = old;
                        continue;
                    default:
                        addNonEmptyString(strings, list, sb.substring(old, pos));
                        //System.out.println(old + " " + pos);
                        old = pos;
                        pos++;
                        while(pos <= length && Syntax.getSyntax(sb.substring(old, pos)) != Syntax.UNKNOWN)
                        {
                            pos++;
                        }
                        pos--;
                        if(old == pos)
                        {
                            throw new PreScriptException("unknown syntax '" + c + "'", line);
                        }
                        addNonEmptyString(strings, list, sb.substring(old, pos));
                        old = pos;
                        continue;
                }
            }
            pos++;
        }
        if(old < length)
        {
            addNonEmptyString(strings, list, sb.substring(old));
        }
        
        return list.toArray(new String[list.size()]);
    }
    
    public static String getArrayString(Object array)
    {
        StringBuilder sb = new StringBuilder("[");
        int length = Array.getLength(array) - 1;
        for(int i = 0; i < length; i++)
        {
            sb.append(Array.get(array, i));
            sb.append(", ");
        }
        if(length > 0)
        {
            sb.append(Array.get(array, length));
        }
        sb.append("]");
        return sb.toString();
    }
    
    // -------------------------------------------------------------------------
    // connectors
    // -------------------------------------------------------------------------
    
    public static String connect(Script sc, InputProvider[] c, int skip)
    {
        return Arrays.stream(c, skip, c.length).map(o -> o.getString(sc)).collect(Collectors.joining());
    }
    
    public static String connect(Collection<Object> c, int skip)
    {
        return c.stream().skip(skip).map(o -> o.toString()).collect(Collectors.joining());
    }
    
    public static String connect(Script sc, InputProvider[] c, String s, int skip)
    {
        return Arrays.stream(c, skip, c.length).map(o -> o.getString(sc)).collect(Collectors.joining(s));
    }
    
    public static String connect(Collection<Object> c, String s, int skip)
    {
        return c.stream().skip(skip).map(o -> String.valueOf(o)).collect(Collectors.joining(s));
    }
    
    // -------------------------------------------------------------------------
    // file stuff
    // -------------------------------------------------------------------------
    
    public static List<String> readCode(String filename, String ending)
    {
        File script = new File("./" + filename + ending);  
        if(script.exists())
        {
            try
            {
                return Files.readAllLines(script.toPath());
            } 
            catch (MalformedInputException ex) 
            {
                throw new PreScriptException("'" + filename + "' contains an illegal character, change file encoding", 0);
            }
            catch (IOException ex) 
            {
                throw new PreScriptException("file '" + filename + "' cannot be read", 0);
            }
        }
        throw new PreScriptException("file '" + filename + "' does not exist", 0);
    }
    
    public static List<String> readCode(String filename)
    {
        return readCode(filename, ".snuvi");
    }
}
