package ex5.scope;

import ex5.models.ModelException;
import ex5.models.ProcessedLine;
import ex5.models.Variable;
import ex5.validator.*;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Block scope: represents an if or while block body.
 * Validates statements within the block.
 */
public class BlockScope extends Scope {
    private final String blockType; // "if" or "while"
    private final String condition;
    //private final List<ProcessedLine> allLines;
    private final int blockStartIndex; // Index of if/while statement line
    private final int blockEndIndex; // Index of closing brace

    public BlockScope(Scope parentScope, String blockType, String condition, int startIndex, int endIndex) {
        super(parentScope, parentScope.getAllLines().get(startIndex).getOriginalLineNumber());
        this.blockType = blockType;
        this.condition = condition;
        //this.allLines = getTopLevelLines(parentScope);
        this.blockStartIndex = startIndex;
        this.blockEndIndex = endIndex;
        this.endLine = allLines.get(endIndex).getOriginalLineNumber();
    }

    private List<ProcessedLine> getTopLevelLines(Scope scope) {
        // Traverse up to GlobalScope to get all lines
        while (scope.getParentScope() != null) {
            scope = scope.getParentScope();
        }
        return ((GlobalScope) scope).getAllLines();
    }

    @Override
    public void validate() throws ScopeException, VariableException, MethodException, ConditionException, ModelException {
        validateBlockBody();
    }

    private void validateBlockBody() throws ScopeException, VariableException, MethodException, ConditionException, ModelException {
        int i = blockStartIndex + 1; // Start after if/while statement line

        while (i <= blockEndIndex) {
            ProcessedLine line = allLines.get(i);
            String content = line.getContent();
            int lineNumber = line.getOriginalLineNumber();

            // Skip closing brace of block
            if (RegexManager.matchesPattern(content, RegexManager.CLOSE_BRACE)) {
                break;
            }

            // Process statement and update index
            i = processStatement(content, lineNumber, i);
        }
    }

    /**
     * Process a single statement and return the next index to continue from.
     */
    private int processStatement(String content, int lineNumber, int currentIndex)
            throws ScopeException, VariableException, MethodException, ConditionException, ModelException {

        // Variable declaration
        Matcher varDeclMatcher = RegexManager.getMatcher(content, RegexManager.VARIABLE_DECLARATION);
        if (varDeclMatcher.matches()) {
            parseAndAddVariableDeclaration(content, lineNumber);
            return currentIndex + 1;
        }

        // Variable assignment
        Matcher assignMatcher = RegexManager.getMatcher(content, RegexManager.VARIABLE_ASSIGNMENT);
        if (assignMatcher.matches()) {
            processAssignment(content, lineNumber);
            return currentIndex + 1;
        }

        // Method call
        Matcher methodCallMatcher = RegexManager.getMatcher(content, RegexManager.METHOD_CALL);
        if (methodCallMatcher.matches()) {
            processMethodCall(content, lineNumber);
            return currentIndex + 1;
        }

        // Return statement (allowed in blocks within methods)
        Matcher returnMatcher = RegexManager.getMatcher(content, RegexManager.RETURN_STATEMENT);
        if (returnMatcher.matches()) {
            return currentIndex + 1;
        }

        // If block
        Matcher ifMatcher = RegexManager.getMatcher(content, RegexManager.IF_STATEMENT);
        if (ifMatcher.matches()) {
            String nestedCondition = ifMatcher.group(1);
            int blockEnd = createAndValidateBlock("if", nestedCondition, lineNumber, currentIndex);
            return blockEnd + 1;
        }

        // While block
        Matcher whileMatcher = RegexManager.getMatcher(content, RegexManager.WHILE_STATEMENT);
        if (whileMatcher.matches()) {
            String nestedCondition = whileMatcher.group(1);
            int blockEnd = createAndValidateBlock("while", nestedCondition, lineNumber, currentIndex);
            return blockEnd + 1;
        }

        // Unrecognized statement
        throw new ScopeException("Invalid statement in " + blockType + " block", lineNumber);
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

    private int createAndValidateBlock(String nestedBlockType, String nestedCondition, int lineNumber, int currentIndex)
            throws ScopeException, VariableException, MethodException, ConditionException, ModelException {
        // Validate condition
        ControlFlowValidator.validateCondition(nestedCondition, this, lineNumber);

        // Find block end
        int blockEnd = findBlockEnd(currentIndex, lineNumber);

        // Create and validate block scope
        BlockScope blockScope = new BlockScope(this, nestedBlockType, nestedCondition, currentIndex, blockEnd);
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

    public String getBlockType() {
        return blockType;
    }

    public String getCondition() {
        return condition;
    }

}

