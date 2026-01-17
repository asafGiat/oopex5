package ex5.main;

import ex5.parser.CodeParser;
import ex5.scope.GlobalScope;
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
            GlobalScope globalScope = parser.parse(filePath);

            // Success
            System.out.println(0);
        } catch (SJavaException e) {
            handleSJavaException(e);
        } catch (Exception e) {
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

