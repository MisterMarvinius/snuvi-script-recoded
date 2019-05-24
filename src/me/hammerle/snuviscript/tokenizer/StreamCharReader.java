package me.hammerle.snuviscript.tokenizer;

import java.io.IOException;
import java.io.InputStream;

public class StreamCharReader
{
    private final InputStream in;
    private int buffer = -1;
    
    public StreamCharReader(InputStream in)
    {
        this.in = in;
    }
    
    public int peekChar()
    {
        if(buffer == -1)
        {
            buffer = readChar();
            return buffer;
        }
        return buffer;
    }
    
    public int readChar()
    {
        if(buffer != -1)
        {
            int r = buffer;
            buffer = -1;
            return r;
        }
        try
        {
            if(in.available() <= 0)
            {
                return -1;
            }
            int data = in.read();
            if((data & 0x80) != 0) // special char
            {
                if((data & 0x40) != 0) // this should always be true
                {
                    if((data & 0x20) != 0) // 3 byte unicode
                    {
                        int a = in.read();
                        int b = in.read();
                        data = ((data & 0xF) << 12) | ((a & 0x3F) << 6) | (b & 0x3F);
                    }
                    else // 2 byte unicode
                    {
                        data = ((data & 0x1F) << 6) | (in.read() & 0x3F);
                    }
                }
                else
                {
                    // should not happen as unicode starts with 11
                }
            }
            return data;
        }
        catch(IOException ex)
        {
            return -1;
        }
    }
}