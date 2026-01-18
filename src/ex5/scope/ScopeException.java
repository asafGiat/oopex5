package ex5.scope;

import ex5.main.SyntaxException;

/**
 * Exception for scope-level validation errors.
 * Thrown when an invalid scope or statement is encountered.
 */
public class ScopeException extends SyntaxException {
    /**
     * Construct a ScopeException with message and line number.
     *
     * @param message    description of the scope error
     * @param lineNumber original source line number
     */
    public ScopeException(String message, int lineNumber) {
        super(message, lineNumber);
    }
}
