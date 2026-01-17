package ex5.scope;

import ex5.models.*;
import ex5.validator.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base scope abstraction. Provides variable lookup chain, child linkage, and global method registry.
 */
public abstract class Scope {
    protected final VariableTable variables = new VariableTable();
    protected final Scope parentScope;
    protected final List<Scope> childScopes = new ArrayList<>();
    protected final List<Statement> statements = new ArrayList<>();
    protected final List<ProcessedLine> allLines;
    protected final int startLine;
    protected int endLine;

    // Global registry of methods across the file (shared across all scopes)
    private static final Map<String, Method> globalMethods = new HashMap<>();

    protected Scope(Scope parentScope, int startLine) {
        this(parentScope, startLine, null);
    }

    protected Scope(Scope parentScope, int startLine, List<ProcessedLine> allLines) {
        this.parentScope = parentScope;
        this.startLine = startLine;
        // If parentScope is null, use provided list (GlobalScope); otherwise reuse parent's list.
        if (parentScope == null) {
            this.allLines = (allLines != null) ? allLines : new ArrayList<>();
        } else {
            this.allLines = parentScope.getAllLines();
        }
    }

    public List<ProcessedLine> getAllLines() {
        return allLines;
    }



    public void addVariable(Variable variable) {
        variables.addVariable(variable);
    }

    public Variable findVariable(String name) {
        Variable local = variables.getVariable(name);
        if (local != null) {
            return local;
        }
        if (parentScope != null) {
            return parentScope.findVariable(name);
        }
        return null;
    }

    public void addStatement(Statement statement) {
        statements.add(statement);
    }

    public void addChildScope(Scope child) {
        childScopes.add(child);
    }

    public int getDepth() {
        int depth = 0;
        Scope current = parentScope;
        while (current != null) {
            depth++;
            current = current.parentScope;
        }
        return depth;
    }

    public static void registerMethod(Method method) {
        globalMethods.put(method.getName(), method);
    }

    public static Method getMethod(String name) {
        return globalMethods.get(name);
    }

    public static boolean methodExists(String name) {
        return globalMethods.containsKey(name);
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public VariableTable getVariables() {
        return variables;
    }

    public List<Scope> getChildScopes() {
        return childScopes;
    }

    public List<Statement> getStatements() {
        return statements;
    }

    public Scope getParentScope() {
        return parentScope;
    }

    /**
     * Validate this scope's content. Implementations parse and validate their relevant lines.
     */
    public abstract void validate() throws ScopeException, VariableException, MethodException, ConditionException;
}

