package ex5.main;

/**
 * Base exception for s-Java validation with line-aware messaging.
 */
public class SJavaException extends Exception {
    private final int lineNumber;

    public SJavaException(String message, int lineNumber) {
        super(message);
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getFormattedMessage() {
        if (lineNumber > 0) {
            return "Error at line " + lineNumber + ": " + getMessage();
        }
        return getMessage();
    }

    /**
     * Returns the appropriate exit code for this exception.
     * Default is 1 (syntax error). Override in subclasses for different behavior.
     */
    public int getExitCode() {
        return 1;
    }
}

