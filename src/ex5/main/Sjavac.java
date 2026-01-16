package ex5.main;

import ex5.parser.CodeParser;
import ex5.scope.GlobalScope;

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

            // Parse and validate the file
            CodeParser parser = new CodeParser();
            GlobalScope globalScope = parser.parse(filePath);

            // Success
            System.out.println(0);
        } catch (InvalidFileException e) {
            System.err.println(e.getFormattedMessage());
            System.out.println(2);
        } catch (SyntaxException e) {
            System.err.println(e.getFormattedMessage());
            System.out.println(1);
        } catch (Exception e) {
            // Any other exception treated as IO/invalid usage per safety
            System.err.println("Unexpected Error: " + e.getMessage());
            System.out.println(2);
        }
    }
}

