package ex5.models;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores variables declared in a specific scope.
 * Container for Variables that supports lookups and simple registration.
 */
public class VariableTable {
    private final Map<String, Variable> variables = new HashMap<>();

    /**
     * Add a variable to this table. Throws if a variable with the same name already exists.
     *
     * @param variable Variable descriptor to add
     * @throws ModelException when a variable with the same name already exists in this table
     */
    public void addVariable(Variable variable) throws ModelException {
        String name = variable.getName();
        if (variables.containsKey(name)) {
            throw new ModelException("Variable already declared in this scope: " + name,
                    variable.getLineNumber());
        }
        variables.put(name, variable);
    }

    /**
     * Lookup a variable by name in this table only.
     *
     * @param name variable name
     * @return Variable instance or null if not present
     */
    public Variable getVariable(String name) {
        return variables.get(name);
    }

    /** @return true when a variable with the given name exists in this table */
    public boolean containsVariable(String name) {
        return variables.containsKey(name);
    }

    /** @return true when the named variable is present and initialized */
    public boolean isInitialized(String name) {
        Variable variable = variables.get(name);
        return variable != null && variable.isInitialized();
    }

    /** @return collection of all variables held in this table */
    public Collection<Variable> getAllVariables() {
        return variables.values();
    }
}
