package ex5.main;

/**
 * Represents invalid file usage or IO errors at program entry.
 * Exit code for this exception is 2.
 */
public class InvalidFileException extends SJavaException {
    public InvalidFileException(String message) {
        super(message, -1);
    }

    @Override
    public int getExitCode() {
        return 2;
    }
}

