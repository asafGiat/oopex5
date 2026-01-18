package ex5.models;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores variables declared in a specific scope.
 */
public class VariableTable {
    private final Map<String, Variable> variables = new HashMap<>();

    public void addVariable(Variable variable) throws ModelException {
        String name = variable.getName();
        if (variables.containsKey(name)) {
            throw new ModelException("Variable already declared in this scope: " + name, variable.getLineNumber());
        }
        variables.put(name, variable);
    }

    public Variable getVariable(String name) {
        return variables.get(name);
    }

    public boolean containsVariable(String name) {
        return variables.containsKey(name);
    }

    public boolean isInitialized(String name) {
        Variable variable = variables.get(name);
        return variable != null && variable.isInitialized();
    }

    public Collection<Variable> getAllVariables() {
        return variables.values();
    }
}

