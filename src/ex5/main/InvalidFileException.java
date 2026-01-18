package ex5.main;

import ex5.models.ReturnCodes;

/**
 * Represents invalid file usage or IO errors at program entry.
 * Exit code for this exception is OTHER_ERROR.
 */
public class InvalidFileException extends SJavaException {
    public InvalidFileException(String message) {
        super(message, NO_LINE);
    }

    @Override
    public int getExitCode() {
        return ReturnCodes.OTHER_ERROR;
    }
}
