package ex5.validator;

import ex5.main.SyntaxException;

public class ConditionException extends SyntaxException {
    public ConditionException(String message, int lineNumber) {
        super(message, lineNumber);
    }
}

