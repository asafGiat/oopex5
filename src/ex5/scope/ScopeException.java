package ex5.scope;

import ex5.main.SyntaxException;

/**
 * Exception for scope-level validation errors.
 */
public class ScopeException extends SyntaxException {
    public ScopeException(String message, int lineNumber) {
        super(message, lineNumber);
    }
}

