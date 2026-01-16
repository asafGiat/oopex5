package ex5.parser;

import ex5.models.ProcessedLine;
import ex5.preprocessor.CodePreprocessor;
import ex5.preprocessor.PreprocessorException;
import ex5.scope.GlobalScope;

import java.util.List;

/**
 * Parser entry point: preprocesses the file, constructs GlobalScope, and triggers validation.
 */
public class CodeParser {

    /**
     * Parse and validate an s-Java source file at the given path.
     *
     * @param filePath path to .sjava file
     * @return validated GlobalScope
     * @throws ParserException when syntax errors occur during validation
     */
    public GlobalScope parse(String filePath) throws ParserException {
        try {
            CodePreprocessor preprocessor = new CodePreprocessor();
            List<ProcessedLine> lines = preprocessor.preprocess(filePath);

            GlobalScope globalScope = new GlobalScope(lines);
            globalScope.validate();

            return globalScope;
        } catch (PreprocessorException e) {
            // Wrap preprocessor issues as parser exceptions with line -1
            throw new ParserException(e.getMessage(), -1);
        } catch (Exception e) {
            if (e instanceof ParserException) {
                throw (ParserException) e;
            }
            // Other SyntaxExceptions will bubble up via main
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        }
    }
}

