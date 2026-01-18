package ex5.scope;

import ex5.models.Method;
import ex5.models.ModelException;
import ex5.models.ProcessedLine;
import ex5.models.Variable;
import ex5.validator.*;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Global scope: top level of the program. Contains global variable declarations and method definitions.
 * Performs two-pass validation:
 *   1) Register all methods and global variables
 *   2) Validate each method body
 */
public class GlobalScope extends Scope {

    private static final int LIMIT = 2;

    public GlobalScope(List<ProcessedLine> allLines) {
        super(null, 1, allLines);
    }

    /**
     * Execute validation over the entire file: register globals and then validate method bodies.
     */
    @Override
    public void validate() throws ScopeException, VariableException, MethodException, ConditionException, ModelException {
        // Pass 1: Register methods and global variables
        firstPass();

        // Pass 2: Validate each method body
        secondPass();
    }

    private void firstPass() throws ScopeException, VariableException, MethodException, ModelException {
        int i = 0;
        while (i < allLines.size()) {
            ProcessedLine line = allLines.get(i);
            String content = line.getContent();
            int lineNumber = line.getOriginalLineNumber();

            // Check for method declaration
            Matcher methodMatcher = RegexManager.getMatcher(content, RegexManager.METHOD_DECLARATION);
            if (methodMatcher.matches()) {
                String methodName = extractMethodName(content);
                String paramsContent = methodMatcher.group(1);

                List<Variable> params = MethodValidator.parseParameterList(paramsContent, lineNumber);
                MethodValidator.validateMethodSignature(methodName, params, lineNumber);

                Method method = new Method(methodName, lineNumber);
                for (Variable param : params) {
                    method.addParameter(param);
                }

                // Find method closing brace
                int endLine = findClosingBrace(i, lineNumber);
                method.setScope(new MethodScope(this, method, i, endLine));

                registerMethod(method);

                // Skip to end of method
                i = endLine + 1;
                continue;
            }

            // Check for global variable declaration
            Matcher varMatcher = RegexManager.getMatcher(content, RegexManager.VARIABLE_DECLARATION);
            if (varMatcher.matches()) {
                parseAndAddVariableDeclaration(content, lineNumber);
                i++;
                continue;
            }

            Matcher assignMatcher = RegexManager.getMatcher(content, RegexManager.VARIABLE_ASSIGNMENT);
            if (assignMatcher.matches()) {
                processAssignment(content, lineNumber);
                i++;
                continue;
            }
            // Any other line at global scope is an error
            throw new ScopeException("Invalid statement at global scope", lineNumber);
        }
    }

    private void secondPass() throws MethodException, VariableException, ScopeException, ConditionException, ModelException {
        // Validate each registered method's body
        for (int i = 0; i < allLines.size(); i++) {
            ProcessedLine line = allLines.get(i);
            String content = line.getContent();

            Matcher methodMatcher = RegexManager.getMatcher(content, RegexManager.METHOD_DECLARATION);
            if (methodMatcher.matches()) {
                String methodName = extractMethodName(content);
                Method method = getMethod(methodName);

                if (method != null && method.getScope() != null) {
                    method.getScope().validate();
                }
            }
        }
    }

    private void parseAndAddVariableDeclaration(String line, int lineNumber)
            throws VariableException, ModelException {
        boolean isFinal = line.startsWith("final ");
        String withoutFinal = isFinal ? line.substring("final ".length()).trim() : line;

        Matcher typeMatcher = RegexManager.getMatcher(withoutFinal, RegexManager.TYPE_PATTERN);
        if (!typeMatcher.find() || typeMatcher.start() != 0) {
            throw new VariableException("Invalid type in variable declaration", lineNumber);
        }

        String type = typeMatcher.group();
        String rest = withoutFinal.substring(type.length()).trim();

        // Remove trailing semicolon
        if (rest.endsWith(";")) {
            rest = rest.substring(0, rest.length() - 1).trim();
        }

        // Split by comma for multiple declarations
        String[] varDecls = rest.split(",");

        for (String varDecl : varDecls) {
            String trimmed = varDecl.trim();
            String varName;
            boolean hasInit = trimmed.contains("=");

            if (hasInit) {
                String[] parts = trimmed.split("=", LIMIT);
                varName = parts[0].trim();
                String value = parts[1].trim();

                VariableValidator.validateVariableName(varName, lineNumber);
                VariableValidator.validateValue(value, type, this, lineNumber);

                Variable variable = new Variable(varName, type, isFinal, true, false, lineNumber);
                addVariable(variable);
            } else {
                varName = trimmed;
                VariableValidator.validateVariableName(varName, lineNumber);
                VariableValidator.validateDeclarationInitialization(isFinal, false, lineNumber);

                Variable variable = new Variable(varName, type, isFinal, false, false, lineNumber);
                addVariable(variable);
            }
        }
    }

    private void processAssignment(String line, int lineNumber)
            throws VariableException {
        // Remove trailing semicolon
        String withoutSemicolon = line.trim();
        if (withoutSemicolon.endsWith(";")) {
            withoutSemicolon = withoutSemicolon.substring(0, withoutSemicolon.length() - 1).trim();
        }

        // Split by comma for multiple assignments
        String[] assignments = withoutSemicolon.split(",");

        for (String assignment : assignments) {
            String trimmed = assignment.trim();
            String[] parts = trimmed.split("=", LIMIT);

            if (parts.length != LIMIT) {
                throw new VariableException("Invalid assignment syntax", lineNumber);
            }

            String varName = parts[0].trim();
            String value = parts[1].trim();

            Variable variable = findVariable(varName);
            if (variable == null) {
                throw new VariableException("Variable not declared: " + varName, lineNumber);
            }

            VariableValidator.validateAssignmentTarget(variable, lineNumber);
            VariableValidator.validateValue(value, variable.getType(), this, lineNumber);

            // Mark variable as initialized
            variable.initialize();
        }
    }

    private String extractMethodName(String line) {
        // Assumes line matches METHOD_DECLARATION pattern
        // Format: void methodName(...) {
        String withoutVoid = line.substring(4).trim(); // remove "void"
        int parenIndex = withoutVoid.indexOf('(');
        return withoutVoid.substring(0, parenIndex).trim();
    }

    private int findClosingBrace(int startIndex, int startLine) throws ScopeException {
        int braceCount = 1; // We've seen the opening brace

        for (int i = startIndex + 1; i < allLines.size(); i++) {
            String content = allLines.get(i).getContent();

            if (content.contains("{")) {
                braceCount++;
            }
            if (content.contains("}")) {
                braceCount--;
                if (braceCount == 0) {
                    return i;
                }
            }
        }

        throw new ScopeException("Unclosed method body", startLine);
    }

    /**
     * Return the list of processed lines for the entire file.
     * This is provided as a convenience and delegates to the shared lines held by the global scope.
     *
     * @return list of ProcessedLine representing the preprocessed file contents with original line numbers
     */
    public List<ProcessedLine> getAllLines() {
        return allLines;
    }
}
