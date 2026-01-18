package ex5.scope;

import ex5.models.Method;
import ex5.models.ModelException;
import ex5.models.ProcessedLine;
import ex5.models.Variable;
import ex5.validator.*;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Method scope: represents a method body with parameters and statements.
 * Validates parameters, method body statements, and ensures return statement is last.
 */
public class MethodScope extends Scope {
    private final Method methodDefinition;
    //private final List<ProcessedLine> allLines;
    private final int methodStartIndex; // Index of method declaration line
    private final int methodEndIndex; // Index of closing brace
    private int lastStatementLine = -1; // Track last actual statement line

    public MethodScope(Scope parentScope, Method method, int startIndex, int endIndex) {
        super(parentScope, method.getDeclarationLine());
        this.methodDefinition = method;
        //this.allLines = ((GlobalScope) parentScope).getAllLines();
        this.methodStartIndex = startIndex;
        this.methodEndIndex = endIndex;
        this.endLine = allLines.get(endIndex).getOriginalLineNumber();
    }

    @Override
    public void validate() throws ScopeException, VariableException, MethodException, ConditionException, ModelException {
        // Step 1: Add parameters to variable table (already validated in GlobalScope)
        for (Variable param : methodDefinition.getParameters()) {
            addVariable(param);
        }

        // Step 2: Validate method body statements
        validateMethodBody();

        // Step 3: Validate return statement exists and is last
        validateReturnStatement();
    }

    private void validateMethodBody() throws ScopeException, VariableException, MethodException, ConditionException,
            ModelException {
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
                String[] parts = trimmed.split("=", 2);
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
            String[] parts = trimmed.split("=", 2);

            if (parts.length != 2) {
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
        if (lastStatementLine == -1) {
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

    public Method getMethodDefinition() {
        return methodDefinition;
    }
}

