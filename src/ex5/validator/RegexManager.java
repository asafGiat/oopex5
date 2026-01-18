package ex5.validator;

import ex5.models.Variable;
import ex5.scope.Scope;

import java.util.HashMap;
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
    /** Pattern that matches valid s-Java types (int, double, boolean, char, String) */
    public static final Pattern TYPE_PATTERN = Pattern.compile("(int|double|boolean|char|String)");
    /** Pattern matching valid variable identifiers in the simplified language */
    public static final Pattern VARIABLE_NAME = Pattern.compile("([a-zA-Z][a-zA-Z0-9_]*|_[a-zA-Z0-9][a-zA-Z0-9_]*)");
    /** Pattern matching valid method names (same rules as identifiers) */
    public static final Pattern METHOD_NAME = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*");

    // Literal value patterns
    /** Pattern that matches integer literals */
    public static final Pattern INT_VALUE = Pattern.compile("[+-]?\\d+");
    /** Pattern that matches floating point literals */
    public static final Pattern DOUBLE_VALUE = Pattern.compile("[+-]?(?:\\d+\\.\\d*|\\d*\\.\\d+|\\d+\\.|\\.\\d+)");
    /** Pattern that matches boolean literal tokens (true/false) */
    public static final Pattern BOOLEAN_VALUE = Pattern.compile("true|false");
    /** Pattern that matches a single quoted char literal (simple form) */
    public static final Pattern CHAR_VALUE = Pattern.compile("'[^'\\\\]'");
    /** Pattern for quoted string literals (does not validate escapes thoroughly) */
    public static final Pattern STRING_VALUE = Pattern.compile("\"[^\"\\\\]*\"");

    // Combined value pattern: literal or identifier
    /** Pattern that matches any value token: literal or variable name. Kept for completeness though not used elsewhere. */
    public static final Pattern VALUE = Pattern.compile("(" + INT_VALUE + ")|(" + DOUBLE_VALUE + ")|(" + BOOLEAN_VALUE + ")|(" + CHAR_VALUE + ")|(" + STRING_VALUE + ")|(" + VARIABLE_NAME + ")");

    // Statement patterns
    /** Pattern for variable declarations (may include multiple comma-separated declarations) */
    public static final Pattern VARIABLE_DECLARATION = Pattern.compile(
            "(?:final\\s+)?" + TYPE_PATTERN + "\\s+" + VARIABLE_NAME + "(?:\\s*=\\s*[^,;]+)?(?:\\s*,\\s*" + VARIABLE_NAME + "(?:\\s*=\\s*[^,;]+)?)*\\s*;"
    );
    /** Pattern for variable assignment statements (supports multiple comma-separated assignments) */
    public static final Pattern VARIABLE_ASSIGNMENT = Pattern.compile(
            VARIABLE_NAME + "\\s*=\\s*[^,;]+(?:\\s*,\\s*" + VARIABLE_NAME + "\\s*=\\s*[^,;]+)*\\s*;"
    );
    /** Pattern for method declarations: void name(params) { */
    public static final Pattern METHOD_DECLARATION = Pattern.compile("void\\s+" + METHOD_NAME + "\\s*\\(([^)]*)\\)\\s*\\{");
    /** Pattern for a method call followed by semicolon */
    public static final Pattern METHOD_CALL = Pattern.compile(METHOD_NAME + "\\s*\\(([^)]*)\\)\\s*;");
    /** Pattern for if statements with condition and opening brace */
    public static final Pattern IF_STATEMENT = Pattern.compile("if\\s*\\((.+)\\)\\s*\\{");
    /** Pattern for while statements with condition and opening brace */
    public static final Pattern WHILE_STATEMENT = Pattern.compile("while\\s*\\((.+)\\)\\s*\\{");
    /** Pattern matching a bare return statement */
    public static final Pattern RETURN_STATEMENT = Pattern.compile("return\\s*;");
    /** Pattern to detect a line ending with an opening brace */
    public static final Pattern OPEN_BRACE = Pattern.compile(".*\\{\\s*$");
    /** Pattern to detect a line that contains only a closing brace */
    public static final Pattern CLOSE_BRACE = Pattern.compile("^\\s*}\\s*$");

    static {
        TYPE_COMPATIBILITY.put("int", Set.of("int"));
        TYPE_COMPATIBILITY.put("double", Set.of("int", "double"));
        TYPE_COMPATIBILITY.put("boolean", Set.of("int", "double", "boolean"));
        TYPE_COMPATIBILITY.put("String", Set.of("String"));
        TYPE_COMPATIBILITY.put("char", Set.of("char"));
    }

    /**
     * Returns true if a source type can be assigned to the target type according to s-Java rules.
     *
     * @param targetType the type that will receive the value
     * @param sourceType the type of the value being assigned
     * @return true when assignment is allowed, false otherwise
     */
    public static boolean isTypeCompatible(String targetType, String sourceType) {
        Set<String> allowed = TYPE_COMPATIBILITY.get(targetType);
        return allowed != null && allowed.contains(sourceType);
    }

    /**
     * Convenience wrapper to check if a line matches a given Pattern.
     *
     * @param line    input text
     * @param pattern regex pattern
     * @return true if the pattern matches the entire line
     */
    public static boolean matchesPattern(String line, Pattern pattern) {
        return pattern.matcher(line).matches();
    }

    /**
     * Return a Matcher for the provided line and pattern (matcher not advanced).
     *
     * @param line    input text
     * @param pattern regex pattern
     * @return Matcher instance for further inspection
     */
    public static Matcher getMatcher(String line, Pattern pattern) {
        return pattern.matcher(line);
    }

    /**
     * Extracts capturing groups from a pattern if it matches the entire line.
     *
     * @param line    input text
     * @param pattern regex with groups
     * @return array of group strings (empty if pattern does not match)
     */
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
     *
     * @param value        the string value (literal or variable name)
     * @param expectedType the expected type name
     * @param scope        scope used to resolve identifiers (may be null)
     * @return true if the value is valid for the expected type
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
     *
     * @param expectedType expected type name
     * @param value        literal string to test
     * @return true when the literal represents a value of the expected type
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

