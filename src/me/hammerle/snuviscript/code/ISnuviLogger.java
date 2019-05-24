package me.hammerle.snuviscript.code;

public interface ISnuviLogger 
{
    /** Prints messages depending on the implementation.
     *
     * @param message a message, can be null
     * @param ex an involved exception, can be null
     * @param function an involved snuvi script function, can be null
     * @param scriptname the name of an involved script, mainly used for 
     * prescript exceptions, can be null, but will never be null if sc != null
     * @param sc an involved script, can be null
     * @param line an involved script line, -1 if no line is involved
     */
    public void print(String message, Exception ex, String function, String scriptname, Script sc, int line);
    
    /** Prints messages depending on the implementation.
     *
     * @param message a message, can be null
     */
    public default void print(String message)
    {
        print(message, null, null, null, null, -1);
    }
}
