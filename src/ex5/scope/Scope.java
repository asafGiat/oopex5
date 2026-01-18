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

    /**
     * @return the list of all processed lines for the file (shared from the global scope)
     */
    public List<ProcessedLine> getAllLines() {
        return allLines;
    }



    /**
     * Add a variable to this scope's variable table.
     *
     * @param variable variable descriptor to add
     * @throws ModelException when a variable with the same name already exists in this scope
     */
    public void addVariable(Variable variable) throws ModelException {
        variables.addVariable(variable);
    }

    /**
     * Find a variable by name, searching this scope and then parent scopes up the chain.
     *
     * @param name variable name to search for
     * @return the Variable if found, or null if not declared in this scope chain
     */
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

    /**
     * Record a parsed statement for this scope (used for bookkeeping).
     *
     * @param statement statement kind
     */
    public void addStatement(Statement statement) {
        statements.add(statement);
    }

    /**
     * Add a child scope to this scope's hierarchy.
     *
     * @param child child Scope instance
     */
    public void addChildScope(Scope child) {
        childScopes.add(child);
    }

    /**
     * Calculate the nesting depth of this scope relative to the file root.
     *
     * @return 0 for top-level/global scope, increasing for deeper nested scopes
     */
    public int getDepth() {
        int depth = 0;
        Scope current = parentScope;
        while (current != null) {
            depth++;
            current = current.parentScope;
        }
        return depth;
    }

    /**
     * Register a method globally for lookup by name.
     *
     * @param method method descriptor to register
     */
    public static void registerMethod(Method method) {
        globalMethods.put(method.getName(), method);
    }

    /**
     * Retrieve a globally registered method by name.
     *
     * @param name method name
     * @return Method descriptor or null when not found
     */
    public static Method getMethod(String name) {
        return globalMethods.get(name);
    }

    /**
     * Check whether a method with the given name has been registered globally.
     *
     * @param name method name
     * @return true when a method is registered with this name
     */
    public static boolean methodExists(String name) {
        return globalMethods.containsKey(name);
    }

    /** @return the start line number for this scope */
    public int getStartLine() {
        return startLine;
    }

    /** @return the end line number for this scope (may be 0/unset until determined) */
    public int getEndLine() {
        return endLine;
    }

    /**
     * Set the end line number for this scope (used when matching braces).
     *
     * @param endLine original source line number of the closing brace
     */
    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    /** @return the VariableTable instance backing this scope */
    public VariableTable getVariables() {
        return variables;
    }

    /** @return list of direct child scopes */
    public List<Scope> getChildScopes() {
        return childScopes;
    }

    /** @return list of statements parsed within this scope */
    public List<Statement> getStatements() {
        return statements;
    }

    /** @return the parent scope, or null for the global scope */
    public Scope getParentScope() {
        return parentScope;
    }

    /**
     * Validate this scope's content. Implementations parse and validate their relevant lines.
     *
     * @throws ScopeException     when a scope-level structural error is found
     * @throws VariableException  when variable-related validation fails
     * @throws MethodException    when method-related validation fails
     * @throws ConditionException when condition expressions are invalid
     * @throws ModelException     when model-level errors occur (e.g. duplicate declarations)
     */
    public abstract void validate() throws ScopeException, VariableException, MethodException,
            ConditionException, ModelException;
}

