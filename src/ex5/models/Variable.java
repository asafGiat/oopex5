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

    /**
     * Create a Variable descriptor.
     *
     * @param name          variable name
     * @param type          variable type name
     * @param isFinal       whether the variable is declared final
     * @param isInitialized whether the variable is already initialized
     * @param isParameter   whether the variable is a method parameter
     * @param lineNumber    declaration line number
     */
    public Variable(String name, String type, boolean isFinal, boolean isInitialized, boolean isParameter,
                    int lineNumber) {
        this.name = name;
        this.type = type;
        this.isFinal = isFinal;
        this.isInitialized = isInitialized;
        this.isParameter = isParameter;
        this.lineNumber = lineNumber;
    }

    /** @return variable name */
    public String getName() {
        return name;
    }

    /** @return variable type */
    public String getType() {
        return type;
    }

    /** @return true when variable is final */
    public boolean isFinal() {
        return isFinal;
    }

    /** @return true when variable is initialized */
    public boolean isInitialized() {
        return isInitialized;
    }

    /** @return true when variable is a parameter */
    public boolean isParameter() {
        return isParameter;
    }

    /** @return declaration line number */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Mark this variable as initialized (e.g. after assignment)
     */
    public void initialize() {
        this.isInitialized = true;
    }

    /**
     * Assert that the variable can be assigned to (throws ModelException on illegal reassign to final).
     *
     * @throws ModelException when attempting to reassign an already-initialized final variable
     */
    public void assertCanAssign() throws ModelException {
        if (isFinal && isInitialized) {
            throw new ModelException("Cannot reassign a final variable: " + name, this.lineNumber);
        }
    }
}
