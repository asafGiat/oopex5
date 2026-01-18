package ex5.preprocessor;

import ex5.main.SyntaxException;
import ex5.main.SJavaException;

/**
 * Represents errors encountered during preprocessing (IO or unsupported file state).
 * Thrown when a preprocessing error occurs (IO or syntax).
 */
public class PreprocessorException extends SyntaxException {
    /**
     * Create a PreprocessorException for IO or preprocessing problems. Line number is unknown.
     *
     * @param message human-readable error description
     */
    public PreprocessorException(String message) {
        super(message, SJavaException.NO_LINE);
    }

}
