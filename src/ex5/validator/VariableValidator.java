package ex5.validator;

import ex5.models.Variable;
import ex5.scope.Scope;

/**
 * Utility validation for variables: naming, declarations, assignments, and usage.
 */
public final class VariableValidator {
    private VariableValidator() {
    }

    /**
     * Validate that a variable name conforms to identifier rules and additional project constraints.
     *
     * @param name       the variable name to validate
     * @param lineNumber original source line number (used for exception reporting)
     * @throws VariableException if the name is invalid according to regex or project rules
     */
    public static void validateVariableName(String name, int lineNumber) throws VariableException {
        if (!RegexManager.VARIABLE_NAME.matcher(name).matches()) {
            throw new VariableException("Invalid variable name: " + name, lineNumber);
        }
        if (name.startsWith("__")) {
            throw new VariableException("Variable name cannot start with double underscore: " + name, lineNumber);
        }
        if ("_".equals(name)) {
            throw new VariableException("Variable name cannot be a single underscore", lineNumber);
        }
    }

    /**
     * Validate that a given value (literal or identifier) is appropriate for the expected type.
     *
     * This method will:
     * - accept matching literals for the expected type
     * - resolve identifiers against the provided scope and verify the variable exists, is initialized,
     *   and its type is compatible with the expected type
     *
     * @param value        the value string (literal or variable name)
     * @param expectedType the expected target type (e.g. "int", "double", "String")
     * @param scope        scope used to resolve variable identifiers (may be null when only literals are expected)
     * @param lineNumber   original source line number for error reporting
     * @throws VariableException when the value is invalid, the variable is missing/uninitialized, or types mismatch
     */
    public static void validateValue(String value, String expectedType, Scope scope, int lineNumber)
            throws VariableException {
        value = value.trim();
        if (RegexManager.matchesLiteral(expectedType, value)) {
            return; // literal is valid
        }
        if (RegexManager.VARIABLE_NAME.matcher(value).matches()) {
            Variable variable = scope.findVariable(value);
            if (variable == null) {
                throw new VariableException("Variable not declared: " + value, lineNumber);
            }
            if (!variable.isInitialized()) {
                throw new VariableException("Variable not initialized: " + value, lineNumber);
            }
            if (!RegexManager.isTypeCompatible(expectedType, variable.getType())) {
                throw new VariableException(
                        "Type mismatch: cannot assign " + variable.getType() + " to " + expectedType,
                        lineNumber
                );
            }
            return;
        }
        throw new VariableException("Invalid value for type " + expectedType + ": " + value, lineNumber);
    }

    /**
     * Validate that an assignment target may be assigned to (e.g. not reassigning an already-initialized final).
     *
     * @param variable   the target variable
     * @param lineNumber original source line number for error reporting
     * @throws VariableException when assignment is not allowed (final already initialized)
     */
    public static void validateAssignmentTarget(Variable variable, int lineNumber) throws VariableException {
        if (variable.isFinal() && variable.isInitialized()) {
            throw new VariableException("Cannot reassign final variable: " + variable.getName(), lineNumber);
        }
    }

    /**
     * Validate that declaration-time initialization rules are satisfied (final variables must be initialized).
     *
     * @param isFinal        whether the declared variable is final
     * @param hasInitialization whether an initializer was provided
     * @param lineNumber     original source line number for error reporting
     * @throws VariableException when a final variable is declared without initialization
     */
    public static void validateDeclarationInitialization(boolean isFinal, boolean hasInitialization, int lineNumber)
            throws VariableException {
        if (isFinal && !hasInitialization) {
            throw new VariableException("Final variable must be initialized at declaration", lineNumber);
        }
    }
}
