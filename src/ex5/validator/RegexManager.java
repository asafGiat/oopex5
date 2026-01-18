package ex5.validator;

import ex5.models.Variable;
import ex5.scope.Scope;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Centralizes all regex patterns and type compatibility utilities for s-Java validation.
 */
public final class RegexManager {

    private RegexManager() {
    }

    // Type compatibility map: target type -> allowed source types
    private static final Map<String, Set<String>> TYPE_COMPATIBILITY = new HashMap<>();

    // Basic identifier patterns
    public static final Pattern TYPE_PATTERN = Pattern.compile("(int|double|boolean|char|String)");
    public static final Pattern VARIABLE_NAME = Pattern.compile("([a-zA-Z][a-zA-Z0-9_]*|_[a-zA-Z0-9][a-zA-Z0-9_]*)");
    public static final Pattern METHOD_NAME = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*");

    // Literal value patterns
    public static final Pattern INT_VALUE = Pattern.compile("[+-]?\\d+");
    public static final Pattern DOUBLE_VALUE = Pattern.compile("[+-]?(?:\\d+\\.\\d*|\\d*\\.\\d+|\\d+\\.|\\.\\d+)");
    public static final Pattern BOOLEAN_VALUE = Pattern.compile("true|false");
    public static final Pattern CHAR_VALUE = Pattern.compile("'[^'\\\\]'");
    public static final Pattern STRING_VALUE = Pattern.compile("\"[^\"\\\\]*\"");

    // Combined value pattern: literal or identifier
    public static final Pattern VALUE = Pattern.compile("(" + INT_VALUE + ")|(" + DOUBLE_VALUE + ")|(" + BOOLEAN_VALUE + ")|(" + CHAR_VALUE + ")|(" + STRING_VALUE + ")|(" + VARIABLE_NAME + ")");

    // Statement patterns
    public static final Pattern VARIABLE_DECLARATION = Pattern.compile(
            "(?:final\\s+)?" + TYPE_PATTERN + "\\s+" + VARIABLE_NAME + "(?:\\s*=\\s*[^,;]+)?(?:\\s*,\\s*" + VARIABLE_NAME + "(?:\\s*=\\s*[^,;]+)?)*\\s*;"
    );
    public static final Pattern VARIABLE_ASSIGNMENT = Pattern.compile(
            VARIABLE_NAME + "\\s*=\\s*[^,;]+(?:\\s*,\\s*" + VARIABLE_NAME + "\\s*=\\s*[^,;]+)*\\s*;"
    );
    public static final Pattern METHOD_DECLARATION = Pattern.compile("void\\s+" + METHOD_NAME + "\\s*\\(([^)]*)\\)\\s*\\{");
    public static final Pattern METHOD_CALL = Pattern.compile(METHOD_NAME + "\\s*\\(([^)]*)\\)\\s*;");
    public static final Pattern IF_STATEMENT = Pattern.compile("if\\s*\\((.+)\\)\\s*\\{");
    public static final Pattern WHILE_STATEMENT = Pattern.compile("while\\s*\\((.+)\\)\\s*\\{");
    public static final Pattern RETURN_STATEMENT = Pattern.compile("return\\s*;");
    public static final Pattern OPEN_BRACE = Pattern.compile(".*\\{\\s*$");
    public static final Pattern CLOSE_BRACE = Pattern.compile("^\\s*}\\s*$");

    static {
        TYPE_COMPATIBILITY.put("int", Set.of("int"));
        TYPE_COMPATIBILITY.put("double", Set.of("int", "double"));
        TYPE_COMPATIBILITY.put("boolean", Set.of("int", "double", "boolean"));
        TYPE_COMPATIBILITY.put("String", Set.of("String"));
        TYPE_COMPATIBILITY.put("char", Set.of("char"));
    }

    public static boolean isTypeCompatible(String targetType, String sourceType) {
        Set<String> allowed = TYPE_COMPATIBILITY.get(targetType);
        return allowed != null && allowed.contains(sourceType);
    }

    public static boolean matchesPattern(String line, Pattern pattern) {
        return pattern.matcher(line).matches();
    }

    public static Matcher getMatcher(String line, Pattern pattern) {
        return pattern.matcher(line);
    }

    public static String[] extractGroups(String line, Pattern pattern) {
        Matcher matcher = pattern.matcher(line);
        if (!matcher.matches()) {
            return new String[0];
        }
        String[] groups = new String[matcher.groupCount()];
        for (int i = 1; i <= matcher.groupCount(); i++) {
            groups[i - 1] = matcher.group(i);
        }
        return groups;
    }

    /**
     * Attempts to validate that a given value is appropriate for the expected type.
     * Literal checks are performed here; variable resolution is delegated to scope lookup.
     */
    public static boolean isValidValueForType(String value, String expectedType, Scope scope) {
        value = value.trim();
        if (matchesLiteral(expectedType, value)) {
            return true;
        }
        if (VARIABLE_NAME.matcher(value).matches() && scope != null) {
            Variable variable = scope.findVariable(value);
            if (variable == null || !variable.isInitialized()) {
                return false;
            }
            return isTypeCompatible(expectedType, variable.getType());
        }
        return false;
    }

    /**
     * Determines if a literal string matches the expected type without involving scope.
     */
    public static boolean matchesLiteral(String expectedType, String value) {
        return switch (expectedType) {
            case "int" -> INT_VALUE.matcher(value).matches();
            case "double" -> DOUBLE_VALUE.matcher(value).matches() || INT_VALUE.matcher(value).matches();
            case "boolean" -> BOOLEAN_VALUE.matcher(value).matches()
                    || INT_VALUE.matcher(value).matches()
                    || DOUBLE_VALUE.matcher(value).matches();
            case "char" -> CHAR_VALUE.matcher(value).matches();
            case "String" -> STRING_VALUE.matcher(value).matches();
            default -> false;
        };
    }
}

