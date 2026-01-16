package ex5.validator;

import ex5.main.SyntaxException;

public class MethodException extends SyntaxException {
    public MethodException(String message, int lineNumber) {
        super(message, lineNumber);
    }
}

