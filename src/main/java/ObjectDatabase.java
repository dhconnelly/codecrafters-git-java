import java.io.IOException;
import java.io.InputStream;

public interface ObjectDatabase {
    /**
     * Returns a stream of the object's content.
     * 
     * @param hash The hash of the object to print.
     * @return A stream of the object's content.
     * @throws GitException If the object cannot be found.
     * @throws IOException  If an error is encountered when reading the object.
     */
    InputStream readObject(String hash) throws GitException, IOException;

    /**
     * Returns a hash for a blob containing the given content.
     * 
     * @param s    The content of the blob.
     * @param size The size of the content.
     * @return The hash of the blob.
     * @throws IOException If encountering an error while reading the content.
     */
    String hashObject(InputStream s, long size) throws IOException;

    /**
     * Writes the given content to the database as a blob and returns its hash.
     * 
     * @param s    The content of the blob.
     * @param size The size of the content.
     * @return The hash of the blbo.
     * @throws IOException If encountering an error while reading the content or
     *                     writing the blob.
     */
    String writeObject(InputStream s, long size) throws IOException;
}
