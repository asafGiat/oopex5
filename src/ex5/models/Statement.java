package ex5.models;

/**
 * Represents statement types encountered during parsing/validation.
 */
public enum Statement {
    /**
     * Variable declaration statement.
     */
    VAR_DECLARATION,

    /**
     * Assignment statement.
     */
    ASSIGNMENT,

    /**
     * Method call statement.
     */
    METHOD_CALL,

    /**
     * If block statement.
     */
    IF_BLOCK,

    /**
     * While block statement.
     */
    WHILE_BLOCK,

    /**
     * Return statement.
     */
    RETURN
}
