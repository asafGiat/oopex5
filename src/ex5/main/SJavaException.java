package ex5.main;

import ex5.models.ReturnCodes;

/**
 * Base exception for s-Java validation with line-aware messaging.
 */
public class SJavaException extends Exception {
    public static final int NO_LINE = -1;

    private final int lineNumber;

    public SJavaException(String message, int lineNumber) {
        super(message);
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getFormattedMessage() {
        if (lineNumber != NO_LINE) {
            return "Error at line " + lineNumber + ": " + getMessage();
        }
        return getMessage();
    }

    /**
     * Returns the appropriate exit code for this exception.
     * Default is SYNTAX_ERROR. Override in subclasses for different behavior.
     */
    public int getExitCode() {
        return ReturnCodes.SYNTAX_ERROR;
    }
}
