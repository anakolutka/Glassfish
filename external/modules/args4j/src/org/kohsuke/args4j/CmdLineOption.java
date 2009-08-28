package org.kohsuke.args4j;

/**
 * A command line option.
 * 
 * <p>
 * This object is responsible for parsing a particular option
 * and storing the parsed result.
 * 
 * @author
 *     Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public interface CmdLineOption {
    
    /**
     * Checks if this option parser recognizes the specified
     * option name.
     */
    boolean accepts(String optionName);

    /**
     * Called if the option that this parser recognizes is found.
     * 
     * @param parser
     *      The parser that's using this option object.
     *      
     *      For example, if the option "-quiet" is simply an alias to
     *      "-verbose 5", then the implementation can just call the
     *      {@link CmdLineParser#parse(String[])} method recursively.      
     * 
     * @param params
     *      The rest of the arguments. This method can use this
     *      object to access the arguments of the option if necessary.
     *  
     * @return
     *      The number of arguments consumed. For example, return 0
     *      if this option doesn't take any parameter.
     */
    int parseArguments( CmdLineParser parser, Parameters params ) throws CmdLineException;
    
//    /**
//     * Adds the usage message for this option. 
//     * <p>
//     * This method is used to build usage message for the parser.
//     * 
//     * @param   buf
//     *      Messages should be appended to this buffer.
//     *      If you add something, make sure you add '\n' at the end. 
//     */
//    void appendUsage( StringBuffer buf );
    
    
    /**
     * SPI for {@link CmdLineOption}.
     * 
     * Object of this interface is passed to
     * {@link CmdLineOption}s to make it easy/safe to parse
     * additional parameters for options.
     */
    public interface Parameters {
        /**
         * Gets the recognized option name.
         * 
         * @return
         *      This option name has been passed to the
         *      {@link CmdLineOption#accepts(String)} method and
         *      the method has returned <code>true</code>.
         */
        String getOptionName();
        
        /**
         * Gets the additional parameter to this option.
         * 
         * @param idx
         *      specifying 0 will retrieve the token next to the option.
         *      For example, if the command line looks like "-o abc -d x",
         *      then <code>getParameter(0)</code> for "-o" returns "abc"
         *      and <code>getParameter(1)</code> will return "-d".
         * 
         * @return
         *      Always return non-null valid String. If an attempt is
         *      made to access a non-existent index, this method throws
         *      appropriate {@link CmdLineException}.
         */
        String getParameter( int idx ) throws CmdLineException;
        
        /**
         * The convenience method of
         * <code>Integer.parseInt(getParameter(idx))</code>
         * with proper error handling.
         * 
         * @exception CmdLineException
         *      If the parameter is not an integer, it throws an
         *      approrpiate {@link CmdLineException}.
         */
        int getIntParameter( int idx ) throws CmdLineException;
    }
    
}
