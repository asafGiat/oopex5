package ex5.parser;

import ex5.main.SyntaxException;

/**
 * Exception representing parsing errors during validation.
 * Thrown when a parsing error occurs during preprocessing or parsing.
 */
public class ParserException extends SyntaxException {
    /**
     * Construct a ParserException with a message and the original line number.
     *
     * @param message    description of the parse error
     * @param lineNumber original source line number
     */
    public ParserException(String message, int lineNumber) {
        super(message, lineNumber);
    }
}
