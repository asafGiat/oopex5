package ex5.main;

import ex5.models.ModelException;
import ex5.parser.CodeParser;
import ex5.parser.ParserException;
import ex5.preprocessor.PreprocessorException;
import ex5.scope.ScopeException;
import ex5.validator.ConditionException;
import ex5.validator.MethodException;
import ex5.validator.VariableException;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Entry point for s-Java verifier.
 * Prints 0 (valid), 1 (syntax error), or 2 (IO/invalid usage) per requirements.
 */
public class Sjavac {
    public static void main(String[] args) {
        try {
            // Validate command-line arguments
            if (args.length != 1) {
                throw new InvalidFileException("Usage: java ex5.main.Sjavac <file.sjava>");
            }
            String filePath = args[0];
            if (!filePath.endsWith(".sjava")) {
                throw new InvalidFileException("File must have .sjava extension");
            }

            // Check if file exists
            if (!Files.exists(Paths.get(filePath))) {
                throw new InvalidFileException("File not found: " + filePath);
            }

            // Parse and validate the file
            CodeParser parser = new CodeParser();
            parser.parse(filePath);

            // Success
            System.out.println(0);
        } catch (InvalidFileException | ModelException | PreprocessorException | ParserException |
                 ScopeException | ConditionException | VariableException | MethodException e) {
            // we use polymorphism so we could have just used SJavaException here, but the demand was to
            // catch the concrete exception type
            handleSJavaException(e);
        } catch (SJavaException e){
            // Catch any other SJavaExceptions not previously caught
            // not expected to be here
            handleSJavaException(e);
        }
        catch (Exception e) {
            // Unexpected non-SJava exceptions
            System.out.println(2);
            System.err.println("Unexpected Error: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Centralized handler for all SJava-related exceptions.
     * Uses polymorphic methods to determine exit code and error message.
     */
    private static void handleSJavaException(SJavaException e) {
        System.out.println(e.getExitCode());
        System.err.println(e.getFormattedMessage());
    }
}
