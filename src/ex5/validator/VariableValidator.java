package ex5.validator;

import ex5.models.Variable;
import ex5.scope.Scope;

import java.util.regex.Pattern;

/**
 * Utility validation for variables: naming, declarations, assignments, and usage.
 */
public final class VariableValidator {
    private VariableValidator() {
    }

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

    public static void validateAssignmentTarget(Variable variable, int lineNumber) throws VariableException {
        if (variable.isFinal() && variable.isInitialized()) {
            throw new VariableException("Cannot reassign final variable: " + variable.getName(), lineNumber);
        }
    }

    public static void validateDeclarationInitialization(boolean isFinal, boolean hasInitialization, int lineNumber)
            throws VariableException {
        if (isFinal && !hasInitialization) {
            throw new VariableException("Final variable must be initialized at declaration", lineNumber);
        }
    }
}

