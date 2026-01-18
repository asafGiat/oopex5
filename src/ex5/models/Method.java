package ex5.models;

import ex5.scope.MethodScope;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a method signature and its associated scope.
 */
public class Method {
    private final String name;
    private final List<Variable> parameters = new ArrayList<>();
    private MethodScope scope;
    private final int declarationLine;

    /**
     * Construct a Method descriptor with the given name and declaration line.
     *
     * @param name            method name
     * @param declarationLine line number where method is declared
     */
    public Method(String name, int declarationLine) {
        this.name = name;
        this.declarationLine = declarationLine;
    }

    /** @return method name */
    public String getName() {
        return name;
    }

    /** @return declaration line number for the method */
    public int getDeclarationLine() {
        return declarationLine;
    }

    /**
     * Add a parameter descriptor to this method (order matters).
     *
     * @param variable parameter Variable descriptor
     */
    public void addParameter(Variable variable) {
        parameters.add(variable);
    }

    /** @return number of parameters */
    public int getParameterCount() {
        return parameters.size();
    }

    /** @return an unmodifiable view of the method parameter list */
    public List<Variable> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    /** @return list of parameter type names in order */
    public List<String> getParameterTypes() {
        List<String> types = new ArrayList<>();
        for (Variable variable : parameters) {
            types.add(variable.getType());
        }
        return types;
    }

    /**
     * Determine whether this method matches the provided name and ordered argument types.
     *
     * @param methodName    name to compare
     * @param argumentTypes ordered list of argument type names
     * @return true when name and types match exactly
     */
    public boolean matchesSignature(String methodName, List<String> argumentTypes) {
        if (!name.equals(methodName) || argumentTypes.size() != parameters.size()) {
            return false;
        }
        for (int i = 0; i < parameters.size(); i++) {
            if (!parameters.get(i).getType().equals(argumentTypes.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** @return the MethodScope associated with this method, or null if not yet set */
    public MethodScope getScope() {
        return scope;
    }

    /** @param scope attach a MethodScope to this method */
    public void setScope(MethodScope scope) {
        this.scope = scope;
    }
}
