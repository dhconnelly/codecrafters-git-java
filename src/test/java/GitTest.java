import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    public void testCat() throws IOException, GitException {
        Path root = Files.createTempDirectory("cat");
        var git = FsObjectDatabase.init(root);
        String sha = "4b5fa63702dd96796042e92787f464e28f09f17d";
        var blob = asBytes(new int[] {
                0x78, 0x01, 0x4b, 0xca, 0xc9, 0x4f, 0x52, 0x30, 0x34, 0x66, 0xc8, 0x48,
                0xcd, 0xc9, 0xc9, 0xd7, 0x51, 0x28, 0xcf, 0x2f, 0xca, 0x49, 0xe1, 0x02,
                0x00, 0x49, 0xb7, 0x06, 0xb6,
        });
        Path path = git.pathFor(sha);
        Files.createDirectories(path.getParent());
        Files.write(path, blob, StandardOpenOption.CREATE_NEW);
        assertArrayEquals(
                "hello, world\n".getBytes(StandardCharsets.UTF_8),
                git.catFile(sha).readAllBytes());
    }
}
