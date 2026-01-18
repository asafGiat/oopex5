package ex5.models;

/**
 * Represents a single preprocessed line and preserves its original line number for accurate reporting.
 */
public class ProcessedLine {
    private final int originalLineNumber;
    private final String content;

    /**
     * @param originalLineNumber original line number in the source file
     * @param content            trimmed line content kept for parsing
     */
    public ProcessedLine(int originalLineNumber, String content) {
        this.originalLineNumber = originalLineNumber;
        this.content = content;
    }

    /**
     * @return original source line number for this processed line
     */
    public int getOriginalLineNumber() {
        return originalLineNumber;
    }

    /**
     * @return the trimmed content of the original line (comments/empties removed)
     */
    public String getContent() {
        return content;
    }
}
