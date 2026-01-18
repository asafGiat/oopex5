package ex5.models;

/**
 * Represents a variable in a given scope with initialization and final-state tracking.
 */
public class Variable {
    private final String name;
    private final String type;
    private final boolean isFinal;
    private boolean isInitialized;
    private final boolean isParameter;
    private final int lineNumber;

    public Variable(String name, String type, boolean isFinal, boolean isInitialized, boolean isParameter,
                    int lineNumber) {
        this.name = name;
        this.type = type;
        this.isFinal = isFinal;
        this.isInitialized = isInitialized;
        this.isParameter = isParameter;
        this.lineNumber = lineNumber;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public boolean isParameter() {
        return isParameter;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void initialize() {
        this.isInitialized = true;
    }

    public void assertCanAssign() throws ModelException {
        if (isFinal && isInitialized) {
            throw new ModelException("Cannot reassign a final variable: " + name, this.lineNumber);
        }
    }
}

