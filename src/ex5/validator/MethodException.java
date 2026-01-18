package ex5.validator;

import ex5.main.SyntaxException;

/**
 * Exception thrown for method-related syntax/semantic errors during validation.
 * Thrown when a method-related semantic or syntax error occurs.
 */
public class MethodException extends SyntaxException {
    /**
     * Construct a MethodException with message and line number.
     *
     * @param message    description of the error
     * @param lineNumber original source line number
     */
    public MethodException(String message, int lineNumber) {
        super(message, lineNumber);
    }
}
