package ex5.validator;

import ex5.models.Variable;
import ex5.scope.Scope;

/**
 * Validates if/while conditions according to simplified language rules.
 */
public final class ControlFlowValidator {
    private ControlFlowValidator() {
    }

    public static void validateCondition(String condition, Scope scope, int lineNumber)
            throws ConditionException, VariableException {
        // Split by logical operators && and ||
        String[] tokens = condition.split("\\&\\&|\\|\\|");
        for (String rawToken : tokens) {
            String token = rawToken.trim();
            if (token.isEmpty()) {
                throw new ConditionException("Empty condition segment", lineNumber);
            }
            if (RegexManager.BOOLEAN_VALUE.matcher(token).matches()) {
                continue;
            }
            if (RegexManager.INT_VALUE.matcher(token).matches()) {
                continue;
            }
            if (RegexManager.DOUBLE_VALUE.matcher(token).matches()) {
                continue;
            }
            if (RegexManager.VARIABLE_NAME.matcher(token).matches()) {
                Variable variable = scope.findVariable(token);
                if (variable == null) {
                    throw new ConditionException("Variable not declared in condition: " + token, lineNumber);
                }
                if (!variable.isInitialized()) {
                    throw new VariableException("Variable not initialized in condition: " + token,
                            lineNumber);
                }
                String type = variable.getType();
                if (!(type.equals("boolean") || type.equals("int") || type.equals("double"))) {
                    throw new ConditionException("Invalid condition type: " + type, lineNumber);
                }
                continue;
            }
            throw new ConditionException("Invalid condition token: " + token, lineNumber);
        }

        // Validate operator placement: no leading/trailing operators
        if (condition.trim().startsWith("&&") || condition.trim().startsWith("||")
                || condition.trim().endsWith("&&") || condition.trim().endsWith("||")) {
            throw new ConditionException("Condition cannot start or end with logical operator", lineNumber);
        }
    }
}

