package ex5.validator;

import ex5.main.SyntaxException;

public class VariableException extends SyntaxException {
    public VariableException(String message, int lineNumber) {
        super(message, lineNumber);
    }
}

