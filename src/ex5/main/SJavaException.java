package ex5.main;

import ex5.models.ReturnCodes;

/**
 * Base exception for s-Java validation with line-aware messaging.
 */
public class SJavaException extends Exception {
    /**
     *  Constant indicating no line number is associated with the error.
     *  */
    public static final int NO_LINE = -1;

    /**
     *  Original source line number, or NO_LINE if not applicable. */
    private final int lineNumber;

    /**
     * Construct an SJavaException with a message and optional line number.
     *
     * @param message    description of the error
     * @param lineNumber original source line number, or NO_LINE if not applicable
     */
    public SJavaException(String message, int lineNumber) {
        super(message);
        this.lineNumber = lineNumber;
    }

    /** Returns the line number associated with this exception, or NO_LINE. */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Returns a formatted error message including line number if available.
     */
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
