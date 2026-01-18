package ex5.validator;

import ex5.main.SyntaxException;

/**
 * Exception thrown for variable-related syntax/semantic errors during validation.
 * Thrown when a variable-related semantic or syntax error occurs.
 */
public class VariableException extends SyntaxException {
    /**
     * Construct a VariableException with a message and the original source line number.
     *
     * @param message    human-readable description of the problem
     * @param lineNumber original source line number where the error occurred
     */
    public VariableException(String message, int lineNumber) {
        super(message, lineNumber);
    }
}
