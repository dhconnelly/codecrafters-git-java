import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface ObjectDatabase {
    /**
     * Returns the type of the object with the given hash.
     * 
     * @param hash The hash of the object.
     * @return The type of the object.
     * @throws IOException If an error is encountered when reading the object.
     */
    ObjectType getType(byte[] hash) throws GitException, IOException;

    /**
     * Returns a stream of the object's content.
     * 
     * @param hash The hash of the object to print.
     * @return A stream of the object's content.
     * @throws GitException If the object cannot be found.
     * @throws IOException  If an error is encountered when reading the object.
     */
    InputStream readBlob(byte[] hash) throws GitException, IOException;

    /**
     * Returns a hash for a blob containing the given content.
     * 
     * @param s    The content of the blob.
     * @param size The size of the content.
     * @return The hash of the blob.
     * @throws IOException If encountering an error while reading the content.
     */
    byte[] hashBlob(InputStream s, long size) throws IOException;

    /**
     * Writes the given content to the database as a blob and returns its hash.
     * 
     * @param s    The content of the blob.
     * @param size The size of the content.
     * @return The hash of the blbo.
     * @throws IOException If encountering an error while reading the content or
     *                     writing the blob.
     */
    byte[] writeBlob(InputStream s, long size) throws IOException;

    /**
     * Lists the objects belonging to a tree.
     *
     * @param hash The hash of the tree.
     * @return The objects in the tree.
     * @throws GitException If the hash does not specify a tree.
     * @throws IOException  If encountering an error while reading any of the files.
     */
    List<TreeObject> listTree(byte[] hash) throws GitException, IOException;

    /**
     * Writes a tree object for the entire object database, recursively writing
     * intermediate trees.
     * 
     * @return A hash representing the new tree object.
     * @throws GitException If any invalid Git objects are found.
     * @throws IOException  If an error is encountered while writing the tree.
     */
    byte[] writeTree() throws GitException, IOException;

    /**
     * Creates a commit from the given tree, parent commit, and commit message.
     * 
     * @param treeHash           The tree to associate with the commit.
     * @param parentCommitHashes The parents of the commit.
     * @param message            The message for the commit.
     * @return The hash of the commit object.
     * @throws GitException If any objects have unexpected type or are invalid.
     * @throws IOException  If any errors are encountered while reading or writing
     *                      objects.
     */
    byte[] commitTree(byte[] treeHash, List<byte[]> parentCommitHashes, String message)
            throws GitException, IOException;
}
