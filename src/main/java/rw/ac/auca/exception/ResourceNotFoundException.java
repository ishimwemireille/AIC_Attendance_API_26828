package rw.ac.auca.exception;

/**
 * The Class ResourceNotFoundException. Answered with 404.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
