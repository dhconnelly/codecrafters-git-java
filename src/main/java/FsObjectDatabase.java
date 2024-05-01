import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FsObjectDatabase implements ObjectDatabase {

    private FsObjectDatabase(Path root) {
    }

    public static FsObjectDatabase create(Path root) throws IOException {
        Files.createDirectories(root.resolve(".git"));
        Files.createDirectories(root.resolve(".git/objects"));
        Files.createDirectories(root.resolve(".git/refs"));
        Files.writeString(root.resolve(".git/HEAD"), "ref: refs/heads/main\n", StandardCharsets.UTF_8);
        return new FsObjectDatabase(root);
    }

    @Override
    public ObjectDatabase.ObjectType getType(String sha) throws ObjectDatabase.ObjectNotFoundException, IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getType'");
    }

    @Override
    public ReadableByteChannel catFile(String sha) throws ObjectDatabase.ObjectNotFoundException, IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'catFile'");
    }

    @Override
    public String hashObject(ReadableByteChannel f, boolean write) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'hashObject'");
    }

}
