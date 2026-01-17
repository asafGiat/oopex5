package ex5.preprocessor;

import ex5.main.SyntaxException;

/**
 * Represents errors encountered during preprocessing (IO or unsupported file state).
 */
public class PreprocessorException extends SyntaxException {
    public PreprocessorException(String message) {
        super(message, -1);
    }

}

