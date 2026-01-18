package ex5.scope;

import ex5.models.*;
import ex5.validator.*;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Represents the scope of a single method in an s-Java source file.
 * @see ex5.models.Method
 * @see Scope
 */
public class MethodScope extends Scope {
    private static final int LIMIT = 2;
    private final Method methodDefinition;
    private final int methodStartIndex; // Index of method declaration line
    private final int methodEndIndex; // Index of closing brace
    private static final int NO_STATEMENT = -1;
    private int lastStatementLine = NO_STATEMENT; // Track last actual statement line

    // Per-method table that holds copies of global variables that were UNINITIALIZED in global scope
    private final VariableTable methodGlobalInits = new VariableTable();

    /**
     * Create a MethodScope for the provided Method descriptor and line indices in the processed lines list.
     *
     * @param parentScope enclosing scope (usually GlobalScope)
     * @param method      method descriptor
     * @param startIndex  index of the method declaration line within the processed lines list
     * @param endIndex    index of the closing brace line within the processed lines list
     */
    public MethodScope(Scope parentScope, Method method, int startIndex, int endIndex) {
        super(parentScope, method.getDeclarationLine());
        this.methodDefinition = method;
        //this.allLines = ((GlobalScope) parentScope).getAllLines();
        this.methodStartIndex = startIndex;
        this.methodEndIndex = endIndex;
        this.endLine = allLines.get(endIndex).getOriginalLineNumber();
    }

    /**
     * Validate the method scope:
     * 1. Adds parameters to the variable table.
     * 2. Populates uninitialized globals.
     * 3. Validates method body and return statement.
     */
    @Override
    public void validate() throws ScopeException, VariableException, MethodException, ConditionException, ModelException {
        // Step 1: Add parameters to variable table (already validated in GlobalScope)
        for (Variable param : methodDefinition.getParameters()) {
            addVariable(param);
        }

        // Populate per-method table with uninitialized globals (only those not shadowed by params/locals)
        copyUninitializedGlobalsFromRoot();

        // Step 2: Validate method body statements
        validateMethodBody();

        // Step 3: Validate return statement exists and is last
        validateReturnStatement();
    }

    /**
     * Lookup variables: check local vars/params, then per-method uninitialized-global copies,
     * then delegate to parent scope (which will eventually find global initialized vars).
     */
    @Override
    public Variable findVariable(String name) {
        // Check locals and parameters first
        Variable local = this.variables.getVariable(name);
        if (local != null) {
            return local;
        }
        // Then check the per-method table of globals that were uninitialized in the global scope
        Variable methodGlobal = this.methodGlobalInits.getVariable(name);
        if (methodGlobal != null) {
            return methodGlobal;
        }
        // Fallback to parent/global
        if (parentScope != null) {
            return parentScope.findVariable(name);
        }
        return null;
    }

    /**
     * Copy only those global variables that were not initialized at the global scope into
     * this method's local per-method table. This allows each method to track its own initialization
     * of previously-uninitialized globals without mutating the global Variable objects.
     */
    private void copyUninitializedGlobalsFromRoot() {
        // Find root/global scope
        Scope root = this;
        while (root.getParentScope() != null) {
            root = root.getParentScope();
        }

        // Copy uninitialized globals that are not shadowed by a parameter/local in this method
        for (Variable g : root.getVariables().getAllVariables()) {
            if (!g.isInitialized() && !this.variables.containsVariable(g.getName())) {
                Variable copy = new Variable(g.getName(), g.getType(), g.isFinal(), false, false, g.getLineNumber());
                try {
                    this.methodGlobalInits.addVariable(copy);
                } catch (ModelException ignored) {
                    // If already present, ignore
                }
            }
        }
    }

    /**
     * Validate the method body:
     * - Processes each line from the start of the method to the closing brace.
     * - Supports variable declarations, assignments, method calls, return statements, and control flow blocks (if/while).
     *
     * @throws ScopeException     if there's an invalid statement.
     * @throws VariableException  if there's a variable-related issue.
     * @throws MethodException    if there's a method-related issue.
     * @throws ConditionException if there's a condition-related issue.
     * @throws ModelException     if there's a model-related issue.
     */
    private void validateMethodBody() throws ScopeException, VariableException, MethodException, ConditionException, ModelException {
        int i = methodStartIndex + 1; // Start after method declaration line

        while (i <= methodEndIndex) {
            ProcessedLine line = allLines.get(i);
            String content = line.getContent();
            int lineNumber = line.getOriginalLineNumber();

            // Skip closing brace of method
            if (RegexManager.matchesPattern(content, RegexManager.CLOSE_BRACE)) {
                break;
            }

            // Process statement and update index
            i = processStatement(content, lineNumber, i);
        }
    }

    /**
     * Process a single statement and return the next index to continue from.
     * Shared logic for statement validation in methods and blocks.
     */
    private int processStatement(String content, int lineNumber, int currentIndex)
            throws ScopeException, VariableException, MethodException, ConditionException, ModelException {

        // Variable declaration
        Matcher varDeclMatcher = RegexManager.getMatcher(content, RegexManager.VARIABLE_DECLARATION);
        if (varDeclMatcher.matches()) {
            parseAndAddVariableDeclaration(content, lineNumber);
            lastStatementLine = lineNumber;
            return currentIndex + 1;
        }

        // Variable assignment
        Matcher assignMatcher = RegexManager.getMatcher(content, RegexManager.VARIABLE_ASSIGNMENT);
        if (assignMatcher.matches()) {
            processAssignment(content, lineNumber);
            lastStatementLine = lineNumber;
            return currentIndex + 1;
        }

        // Method call
        Matcher methodCallMatcher = RegexManager.getMatcher(content, RegexManager.METHOD_CALL);
        if (methodCallMatcher.matches()) {
            processMethodCall(content, lineNumber);
            lastStatementLine = lineNumber;
            return currentIndex + 1;
        }

        // Return statement
        Matcher returnMatcher = RegexManager.getMatcher(content, RegexManager.RETURN_STATEMENT);
        if (returnMatcher.matches()) {
            lastStatementLine = lineNumber;
            return currentIndex + 1;
        }

        // If block
        Matcher ifMatcher = RegexManager.getMatcher(content, RegexManager.IF_STATEMENT);
        if (ifMatcher.matches()) {
            String condition = ifMatcher.group(1);
            int blockEnd = createAndValidateBlock("if", condition, lineNumber, currentIndex);
            lastStatementLine = lineNumber; // The if statement itself counts
            return blockEnd + 1;
        }

        // While block
        Matcher whileMatcher = RegexManager.getMatcher(content, RegexManager.WHILE_STATEMENT);
        if (whileMatcher.matches()) {
            String condition = whileMatcher.group(1);
            int blockEnd = createAndValidateBlock("while", condition, lineNumber, currentIndex);
            lastStatementLine = lineNumber; // The while statement itself counts
            return blockEnd + 1;
        }

        // Unrecognized statement
        throw new ScopeException("Invalid statement in method body", lineNumber);
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

    private void processMethodCall(String line, int lineNumber)
            throws MethodException, VariableException {
        // Extract method name and arguments
        int openParen = line.indexOf('(');
        int closeParen = line.lastIndexOf(')');

        String methodName = line.substring(0, openParen).trim();
        String argsContent = line.substring(openParen + 1, closeParen).trim();

        List<String> args = MethodValidator.splitArguments(argsContent);
        MethodValidator.validateMethodCall(methodName, args, this, lineNumber);
    }

    private int createAndValidateBlock(String blockType, String condition, int lineNumber, int currentIndex)
            throws ScopeException, VariableException, MethodException, ConditionException, ModelException {
        // Validate condition
        ControlFlowValidator.validateCondition(condition, this, lineNumber);

        // Find block end
        int blockEnd = findBlockEnd(currentIndex, lineNumber);

        // Create and validate block scope
        BlockScope blockScope = new BlockScope(this, blockType, condition, currentIndex, blockEnd);
        addChildScope(blockScope);
        blockScope.validate();

        return blockEnd;
    }

    private int findBlockEnd(int startIndex, int startLine) throws ScopeException {
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

        throw new ScopeException("Unclosed block", startLine);
    }

    private void validateReturnStatement() throws MethodException {
        if (lastStatementLine == NO_STATEMENT) {
            throw new MethodException("Method must contain at least one statement", startLine);
        }

        // Find the line content for the last statement
        ProcessedLine lastLine = null;
        for (ProcessedLine line : allLines) {
            if (line.getOriginalLineNumber() == lastStatementLine) {
                lastLine = line;
                break;
            }
        }

        if (lastLine == null) {
            throw new MethodException("Cannot find last statement", lastStatementLine);
        }

        if (!RegexManager.matchesPattern(lastLine.getContent(), RegexManager.RETURN_STATEMENT)) {
            throw new MethodException("Last statement must be return", lastStatementLine);
        }
    }
}
