import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.junit.jupiter.api.Test;

public class GitTest {
    private static byte[] asBytes(int[] ints) {
        byte[] bytes = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) {
            bytes[i] = (byte) ints[i];
        }
        return bytes;
    }

    private static void createFile(Path path, byte[] data) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, data, StandardOpenOption.CREATE_NEW);
    }

    // to reproduce:
    // git init
    // echo "hello, world" | git hash-object --stdin -w
    // xxd -i .git/objects/4b/5fa63702dd96796042e92787f464e28f09f17d
    private static final byte[] CONTENT = "hello, world\n".getBytes(UTF_8);
    private static final byte[] CONTENT_HASH_BINARY = asBytes(new int[] {
            0x4b, 0x5f, 0xa6, 0x37, 0x02, 0xdd, 0x96, 0x79, 0x60, 0x42, 0xe9, 0x27,
            0x87, 0xf4, 0x64, 0xe2, 0x8f, 0x09, 0xf1, 0x7d });
    private static final byte[] CONTENT_DATA = asBytes(new int[] {
            0x78, 0x01, 0x4b, 0xca, 0xc9, 0x4f, 0x52, 0x30, 0x34, 0x66, 0xc8, 0x48,
            0xcd, 0xc9, 0xc9, 0xd7, 0x51, 0x28, 0xcf, 0x2f, 0xca, 0x49, 0xe1, 0x02,
            0x00, 0x49, 0xb7, 0x06, 0xb6,
    });

    // echo "hello, cruel world" | git hash-object --stdin -w
    // xxd -i .git/objects/bb/d698f6f2eb4009d9950c3a0317c536b504c842
    private static final byte[] CONTENT2 = "hello, cruel world\n".getBytes(UTF_8);
    private static final byte[] CONTENT2_HASH_BINARY = asBytes(new int[] {
            0xbb, 0xd6, 0x98, 0xf6, 0xf2, 0xeb, 0x40, 0x09, 0xd9, 0x95, 0x0c, 0x3a,
            0x03, 0x17, 0xc5, 0x36, 0xb5, 0x04, 0xc8, 0x42 });
    private static final byte[] CONTENT2_DATA = asBytes(new int[] {
            0x78, 0x01, 0x4b, 0xca, 0xc9, 0x4f, 0x52, 0x30, 0xb4, 0x64, 0xc8, 0x48,
            0xcd, 0xc9, 0xc9, 0xd7, 0x51, 0x48, 0x2e, 0x2a, 0x4d, 0xcd, 0x51, 0x28,
            0xcf, 0x2f, 0xca, 0x49, 0xe1, 0x02, 0x00, 0x7b, 0x36, 0x08, 0xf7
    });

    // git update-index --add --cacheinfo 100644 \
    // 4b5fa63702dd96796042e92787f464e28f09f17d hello.txt
    // git update-index --add --cacheinfo 100644 \
    // bbd698f6f2eb4009d9950c3a0317c536b504c842 hello2.txt
    // git write-tree
    // xxd -i .git/objects/58/eed98b03a8df0e87c6b023fd8e17a939dbaa4c
    private static final List<TreeObject> TREE_FILES = List.of(
            new TreeObject("hello.txt", ObjectType.Blob, 100644, CONTENT_HASH_BINARY),
            new TreeObject("hello2.txt", ObjectType.Blob, 100644, CONTENT2_HASH_BINARY));
    private static final byte[] TREE_HASH_BINARY = asBytes(new int[] {
            0x58, 0xee, 0xd9, 0x8b, 0x03, 0xa8, 0xdf, 0x0e, 0x87, 0xc6, 0xb0, 0x23,
            0xfd, 0x8e, 0x17, 0xa9, 0x39, 0xdb, 0xaa, 0x4c });
    private static final byte[] TREE_DATA = asBytes(new int[] {
            0x78, 0x01, 0x2b, 0x29, 0x4a, 0x4d, 0x55, 0x30, 0x37, 0x65, 0x30, 0x34,
            0x30, 0x30, 0x33, 0x31, 0x51, 0xc8, 0x48, 0xcd, 0xc9, 0xc9, 0xd7, 0x2b,
            0xa9, 0x28, 0x61, 0xf0, 0x8e, 0x5f, 0x66, 0xce, 0x74, 0x77, 0x5a, 0x65,
            0x82, 0xd3, 0x4b, 0xf5, 0xf6, 0x2f, 0x29, 0x8f, 0xfa, 0x39, 0x3f, 0xd6,
            0x22, 0x2b, 0x32, 0x02, 0xab, 0xda, 0x7d, 0x6d, 0xc6, 0xb7, 0x4f, 0xaf,
            0x1d, 0x38, 0x6f, 0x4e, 0xe5, 0xb1, 0x62, 0x16, 0x3f, 0x6a, 0xb6, 0x95,
            0xe5, 0x84, 0x13, 0x00, 0xe0, 0x71, 0x20, 0x10
    });

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

    @Test
    public void testHash() throws IOException, GitException {
        // GIVEN
        var git = FsObjectDatabase.init(Files.createTempDirectory("hash"));

        // WHEN
        byte[] hash = git.hashBlob(new ByteArrayInputStream(CONTENT), CONTENT.length);

        // THEN
        assertArrayEquals(CONTENT_HASH_BINARY, hash);
    }

    @Test
    public void testHashWrite() throws IOException, GitException {
        // GIVEN
        var git = FsObjectDatabase.init(Files.createTempDirectory("write"));

        // WHEN
        byte[] hash = git.writeBlob(new ByteArrayInputStream(CONTENT), CONTENT.length);

        // THEN
        assertArrayEquals(CONTENT_HASH_BINARY, hash);
        byte[] content = git.readBlob(CONTENT_HASH_BINARY).readAllBytes();
        assertArrayEquals(CONTENT, content);
    }

    @Test
    public void testCat() throws IOException, GitException {
        // GIVEN
        var git = FsObjectDatabase.init(Files.createTempDirectory("cat"));
        createFile(git.pathFor(CONTENT_HASH_BINARY), CONTENT_DATA);

        // WHEN
        byte[] content = git.readBlob(CONTENT_HASH_BINARY).readAllBytes();

        // THEN
        assertArrayEquals(CONTENT, content);
    }

    @Test
    public void testGetTypeBlob() throws GitException, IOException {
        // GIVEN
        var git = FsObjectDatabase.init(Files.createTempDirectory("type"));
        createFile(git.pathFor(CONTENT_HASH_BINARY), CONTENT_DATA);

        // WHEN
        ObjectType type = git.getType(CONTENT_HASH_BINARY);

        // TJHEN
        assertEquals(ObjectType.Blob, type);
    }

    @Test
    public void testGetTypeTree() throws GitException, IOException {
        // GIVEN
        var git = FsObjectDatabase.init(Files.createTempDirectory("type"));
        createFile(git.pathFor(TREE_HASH_BINARY), TREE_DATA);

        // WHEN
        ObjectType type = git.getType(TREE_HASH_BINARY);

        // THEN
        assertEquals(ObjectType.Tree, type);
    }

    @Test
    public void testListTree() throws GitException, IOException {
        // GIVEN
        var git = FsObjectDatabase.init(Files.createTempDirectory("list"));
        createFile(git.pathFor(CONTENT_HASH_BINARY), CONTENT_DATA);
        createFile(git.pathFor(CONTENT2_HASH_BINARY), CONTENT2_DATA);
        createFile(git.pathFor(TREE_HASH_BINARY), TREE_DATA);

        // WHEN
        List<TreeObject> elems = git.listTree(TREE_HASH_BINARY);

        // THEN
        assertEquals(elems.size(), TREE_FILES.size());
        for (int i = 0; i < elems.size(); i++) {
            assertEquals(elems.get(i).name(), TREE_FILES.get(i).name());
            assertEquals(elems.get(i).type(), TREE_FILES.get(i).type());
            assertEquals(elems.get(i).mode(), TREE_FILES.get(i).mode());
            assertArrayEquals(elems.get(i).hash(), TREE_FILES.get(i).hash());
        }
    }

    @Test
    public void testWriteTree() throws IOException, GitException {
        // GIVEN
        Path root = Files.createTempDirectory("write");
        createFile(root.resolve(TREE_FILES.get(0).name()), CONTENT);
        createFile(root.resolve(TREE_FILES.get(1).name()), CONTENT2);

        // WHEN
        var git = FsObjectDatabase.init(root);
        byte[] hash = git.writeTree();

        // THEN
        assertArrayEquals(TREE_HASH_BINARY, hash);
    }
}
