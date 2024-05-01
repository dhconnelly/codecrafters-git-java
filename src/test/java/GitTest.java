import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GitTest {
    @Test
    public void testInit() throws IOException {
        Path root = Files.createTempDirectory("init");
        FsObjectDatabase.create(root);
        assertTrue(Files.isDirectory(root.resolve(".git")));
        assertTrue(Files.isDirectory(root.resolve(".git/objects")));
        assertTrue(Files.isDirectory(root.resolve(".git/refs")));
        assertTrue(Files.exists(root.resolve(".git/HEAD")));
        assertEquals(
                "ref: refs/heads/main\n",
                Files.readString(root.resolve(".git/HEAD")));
    }

    @Test
    public void testInitExisting() {

    }
}
