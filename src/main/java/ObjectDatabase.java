import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

public interface ObjectDatabase {
    enum ObjectType {
        Blob, Tree, Commit
    }

    class ObjectNotFoundException extends Exception {
        ObjectNotFoundException(String sha) {
            super("object not found: %s".formatted(sha));
        }
    }

    /**
     * Determines the type of an object.
     * 
     * @param sha The sha-1 hash of the object.
     * @return The type of the object.
     * @throws ObjectNotFoundException If an object with the given sha can't be
     *                                 found.
     * @throws IOException             If encountering an error when reading the
     *                                 object.
     */
    ObjectType getType(String sha)
            throws ObjectNotFoundException, IOException;

    /**
     * Returns a channel to the given object's content.
     * 
     * @param sha The sha-1 hash of the object to print.
     * @return A channel to the object's content.
     * @throws ObjectNotFoundException If the object cannot be found.
     * @throws IOException             If an error is encountered when reading the
     *                                 object.
     */
    ReadableByteChannel catFile(String sha)
            throws ObjectNotFoundException, IOException;

    /**
     * Returns a sha-1 hash for a blob containing the given content.
     * 
     * @param f     The content of the blob.
     * @param write Whether the object should be written to this object database.
     * @return The hash of the blob.
     * @throws IOException If encountering an error while reading the content.
     */
    String hashObject(ReadableByteChannel f, boolean write)
            throws IOException;
}
