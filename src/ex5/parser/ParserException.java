package ex5.parser;

import ex5.main.SyntaxException;

public class ParserException extends SyntaxException {
    public ParserException(String message, int lineNumber) {
        super(message, lineNumber);
    }
}

