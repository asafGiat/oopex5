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

    public Method(String name, int declarationLine) {
        this.name = name;
        this.declarationLine = declarationLine;
    }

    public String getName() {
        return name;
    }

    public int getDeclarationLine() {
        return declarationLine;
    }

    public void addParameter(Variable variable) {
        parameters.add(variable);
    }

    public int getParameterCount() {
        return parameters.size();
    }

    public List<Variable> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    public List<String> getParameterTypes() {
        List<String> types = new ArrayList<>();
        for (Variable variable : parameters) {
            types.add(variable.getType());
        }
        return types;
    }

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

    public MethodScope getScope() {
        return scope;
    }

    public void setScope(MethodScope scope) {
        this.scope = scope;
    }
}

