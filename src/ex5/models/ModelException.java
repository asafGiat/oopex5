package ex5.models;

import ex5.main.SJavaException;

public class ModelException extends SJavaException {

    public ModelException(String message, int lineNumber) {
        super(message, lineNumber);
    }

}
