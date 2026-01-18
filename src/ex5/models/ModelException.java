package ex5.models;

import ex5.main.SyntaxException;

public class ModelException extends SyntaxException {

    public ModelException(String message, int lineNumber) {
        super(message, lineNumber);
    }

}
