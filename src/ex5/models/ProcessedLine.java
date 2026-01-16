package ex5.models;

/**
 * Represents a single preprocessed line and preserves its original line number for accurate reporting.
 */
public class ProcessedLine {
    private final int originalLineNumber;
    private final String content;

    public ProcessedLine(int originalLineNumber, String content) {
        this.originalLineNumber = originalLineNumber;
        this.content = content;
    }

    public int getOriginalLineNumber() {
        return originalLineNumber;
    }

    public String getContent() {
        return content;
    }
}

