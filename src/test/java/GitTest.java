import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class GitTest {
    @Test
    public void testInit() throws IOException, GitException {
        Path root = Files.createTempDirectory("init");
        FsObjectDatabase.init(root);
        assertTrue(Files.isDirectory(root.resolve(".git")));
        assertTrue(Files.isDirectory(root.resolve(".git/objects")));
        assertTrue(Files.isDirectory(root.resolve(".git/refs")));
        assertTrue(Files.exists(root.resolve(".git/HEAD")));
        assertEquals(
                "ref: refs/heads/main\n",
                Files.readString(root.resolve(".git/HEAD")));
    }

    private static byte[] asBytes(int[] ints) {
        byte[] bytes = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) {
            bytes[i] = (byte) ints[i];
        }
        return bytes;
    }

    // to reproduce:
    // echo "hello, world" | git hash-object --stdin -w
    // xxd -i .git/objects/4b/5fa63702dd96796042e92787f464e28f09f17d
    private static final byte[] CONTENT = "hello, world\n".getBytes(StandardCharsets.UTF_8);
    private static final String CONTENT_HASH = "4b5fa63702dd96796042e92787f464e28f09f17d";
    private static final byte[] CONTENT_BLOB = asBytes(new int[] {
            0x78, 0x01, 0x4b, 0xca, 0xc9, 0x4f, 0x52, 0x30, 0x34, 0x66, 0xc8, 0x48,
            0xcd, 0xc9, 0xc9, 0xd7, 0x51, 0x28, 0xcf, 0x2f, 0xca, 0x49, 0xe1, 0x02,
            0x00, 0x49, 0xb7, 0x06, 0xb6,
    });

    @Test
    public void testHash() throws IOException, GitException {
        // GIVEN
        var git = FsObjectDatabase.init(Files.createTempDirectory("cat"));

        // WHEN
        String hash = git.hashObject(new ByteArrayInputStream(CONTENT), CONTENT.length, false);

        // THEN
        assertEquals(CONTENT_HASH, hash);
    }

    @Test
    public void testHashWrite() throws IOException, GitException {

    }

    @Test
    public void testCat() throws IOException, GitException {
        // GIVEN
        var git = FsObjectDatabase.init(Files.createTempDirectory("cat"));
        Path path = git.pathFor(CONTENT_HASH);
        Files.createDirectories(path.getParent());
        Files.write(path, CONTENT_BLOB, StandardOpenOption.CREATE_NEW);

        // WHEN
        byte[] content = git.catFile(CONTENT_HASH).readAllBytes();

        // THEN
        assertArrayEquals(CONTENT, content);
    }
}
