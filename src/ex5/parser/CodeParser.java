package ex5.parser;

import ex5.models.ModelException;
import ex5.models.ProcessedLine;
import ex5.preprocessor.CodePreprocessor;
import ex5.preprocessor.PreprocessorException;
import ex5.main.SyntaxException;
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
     * @throws PreprocessorException when file IO errors occur
     * @throws SyntaxException when syntax errors occur during validation
     */
    public GlobalScope parse(String filePath) throws PreprocessorException, SyntaxException, ModelException {
        CodePreprocessor preprocessor = new CodePreprocessor();
        List<ProcessedLine> lines = preprocessor.preprocess(filePath);

        GlobalScope globalScope = new GlobalScope(lines);
        globalScope.validate();

        return globalScope;
    }
}

