package ex5.validator;

import ex5.main.SyntaxException;

/**
 * Exception thrown when a condition expression is invalid.
 */
public class ConditionException extends SyntaxException {
    /**
     * Create a ConditionException with message and line number.
     *
     * @param message    description of the condition error
     * @param lineNumber source line number
     */
    public ConditionException(String message, int lineNumber) {
        super(message, lineNumber);
    }
}
