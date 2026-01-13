package ex5.preprocessor;

import ex5.models.ProcessedLine;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for cleaning the raw s-Java file: removes empty lines and full-line // comments,
 * while preserving original line numbers for accurate error reporting.
 */
public class CodePreprocessor {

    /**
     * Preprocess the file: drop empty lines and lines that begin with // (ignoring leading whitespace).
     * No syntax validation is performed here.
     *
     * @param filePath path to the .sjava file
     * @return list of processed lines with original line numbers
     * @throws PreprocessorException when IO errors occur
     */
    public List<ProcessedLine> preprocess(String filePath) throws PreprocessorException {
        List<ProcessedLine> processedLines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    lineNumber++;
                    continue;
                }
                String trimmed = line.trim();
                // Treat lines whose first non-space chars are // as comments to skip
                if (trimmed.startsWith("//")) {
                    lineNumber++;
                    continue;
                }
                processedLines.add(new ProcessedLine(lineNumber, trimmed));
                lineNumber++;
            }
        } catch (IOException e) {
            throw new PreprocessorException("Failed to read file: " + e.getMessage());
        }

        return processedLines;
    }
}

