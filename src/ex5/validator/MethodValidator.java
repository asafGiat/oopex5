package ex5.validator;

import ex5.models.Method;
import ex5.models.Variable;
import ex5.scope.Scope;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Method declaration and invocation validation helpers.
 */
public final class MethodValidator {
    private MethodValidator() {
    }

    public static void validateMethodSignature(String methodName, List<Variable> params, int lineNumber)
            throws MethodException {
        if (!RegexManager.METHOD_NAME.matcher(methodName).matches()) {
            throw new MethodException("Invalid method name: " + methodName, lineNumber);
        }
        // No overloading support: method name must be unique
        Method existing = Scope.getMethod(methodName);
        if (existing != null) {
            throw new MethodException("Method already declared: " + methodName, lineNumber);
        }
    }

    public static void validateMethodCall(String methodName, List<String> argStrings, Scope scope, int lineNumber)
            throws MethodException, VariableException {
        Method method = Scope.getMethod(methodName);
        if (method == null) {
            throw new MethodException("Method not declared: " + methodName, lineNumber);
        }
        List<Variable> params = method.getParameters();
        if (params.size() != argStrings.size()) {
            throw new MethodException("Argument count mismatch for method: " + methodName, lineNumber);
        }

        for (int i = 0; i < params.size(); i++) {
            Variable param = params.get(i);
            String arg = argStrings.get(i).trim();
            String inferredType = inferType(arg, scope, lineNumber);
            if (!RegexManager.isTypeCompatible(param.getType(), inferredType)) {
                throw new MethodException(
                        "Type mismatch for argument " + (i + 1) + " in call to " + methodName,
                        lineNumber
                );
            }
        }
    }

    private static String inferType(String value, Scope scope, int lineNumber) throws MethodException, VariableException {
        if (RegexManager.INT_VALUE.matcher(value).matches()) {
            return "int";
        }
        if (RegexManager.DOUBLE_VALUE.matcher(value).matches()) {
            return "double";
        }
        if (RegexManager.BOOLEAN_VALUE.matcher(value).matches()) {
            return "boolean";
        }
        if (RegexManager.CHAR_VALUE.matcher(value).matches()) {
            return "char";
        }
        if (RegexManager.STRING_VALUE.matcher(value).matches()) {
            return "String";
        }
        if (RegexManager.VARIABLE_NAME.matcher(value).matches()) {
            Variable variable = scope.findVariable(value);
            if (variable == null) {
                throw new MethodException("Variable not declared: " + value, lineNumber);
            }
            if (!variable.isInitialized()) {
                throw new VariableException("Variable not initialized: " + value, lineNumber);
            }
            return variable.getType();
        }
        throw new MethodException("Invalid argument: " + value, lineNumber);
    }

    public static List<String> splitArguments(String argsContent) {
        List<String> args = new ArrayList<>();
        if (argsContent == null || argsContent.isBlank()) {
            return args;
        }
        // Simple split on commas (no nested structures in simplified language)
        for (String arg : argsContent.split(",")) {
            args.add(arg.trim());
        }
        return args;
    }

    public static List<Variable> parseParameterList(String paramsContent, int lineNumber) throws MethodException {
        List<Variable> params = new ArrayList<>();
        if (paramsContent == null || paramsContent.isBlank()) {
            return params;
        }
        String[] parts = paramsContent.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            boolean isFinal = trimmed.startsWith("final ");
            String withoutFinal = isFinal ? trimmed.substring("final ".length()).trim() : trimmed;

            Matcher typeMatcher = RegexManager.TYPE_PATTERN.matcher(withoutFinal);
            if (!typeMatcher.find() || typeMatcher.start() != 0) {
                throw new MethodException("Invalid parameter type in: " + trimmed, lineNumber);
            }
            String type = typeMatcher.group();
            String name = withoutFinal.substring(type.length()).trim();
            if (name.isEmpty()) {
                throw new MethodException("Missing parameter name in: " + trimmed, lineNumber);
            }
            Variable param = new Variable(name, type, isFinal, true, true, lineNumber);
            params.add(param);
        }
        return params;
    }
}

