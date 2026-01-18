package ex5.main;

/**
 * Base class for syntax-related errors in s-Java code.
 * Base exception for syntactic and semantic validation errors in s-Java.
 */
public class SyntaxException extends SJavaException {
    /**
     * Construct a SyntaxException with message and line number.
     *
     * @param message    description of the syntax error
     * @param lineNumber original source line number
     */
    public SyntaxException(String message, int lineNumber) {
        super(message, lineNumber);
    }
}
