package ex5.models;

/**
 * Centralized return code constants for the s-Java verifier.
 */
public final class ReturnCodes {
    private ReturnCodes() { /* no instances */ }

    /** Exit code indicating valid s-Java code. */
    public static final int VALID = 0;
    /** Exit code indicating a syntax or semantic error in s-Java code. */
    public static final int SYNTAX_ERROR = 1;
    /** Exit code indicating other errors (e.g., file IO issues). */
    public static final int OTHER_ERROR = 2;
}

