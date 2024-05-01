import java.io.IOException;
import java.io.InputStream;

public interface ObjectDatabase {
    enum ObjectType {
        Blob, Tree, Commit
    }

    /**
     * Returns a channel to the given object's content.
     * 
     * @param sha The sha-1 hash of the object to print.
     * @return A channel to the object's content.
     * @throws GitException If the object cannot be found.
     * @throws IOException  If an error is encountered when reading the object.
     */
    InputStream catFile(String sha) throws GitException, IOException;

    /**
     * Returns a sha-1 hash for a blob containing the given content.
     * 
     * @param f     The content of the blob.
     * @param write Whether the object should be written to this object database.
     * @return The hash of the blob.
     * @throws IOException If encountering an error while reading the content.
     */
    String hashObject(InputStream s, boolean write) throws IOException;
}
