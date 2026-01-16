package ex5.main;

/**
 * Base class for syntax-related errors in s-Java code.
 */
public class SyntaxException extends SJavaException {
    public SyntaxException(String message, int lineNumber) {
        super(message, lineNumber);
    }
}

