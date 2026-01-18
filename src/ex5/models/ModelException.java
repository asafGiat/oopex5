package ex5.models;

import ex5.main.SyntaxException;

/**
 * Exception thrown for model-level errors (e.g. duplicate variables in scope).
 */
public class ModelException extends SyntaxException {

    /**
     * Create a ModelException with an error message and original line number.
     *
     * @param message    error description
     * @param lineNumber source line number
     */
    public ModelException(String message, int lineNumber) {
        super(message, lineNumber);
    }

}
