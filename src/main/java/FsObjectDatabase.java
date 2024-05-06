import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class FsObjectDatabase implements ObjectDatabase {
    private final Path root;

    private FsObjectDatabase(Path root) {
        this.root = root;
    }

    public static FsObjectDatabase init(Path root) throws GitException, IOException {
        Files.createDirectories(root.resolve(".git"));
        Files.createDirectories(root.resolve(".git/objects"));
        Files.createDirectories(root.resolve(".git/refs"));
        Path head = root.resolve(".git/HEAD");
        if (!Files.exists(head)) {
            Files.writeString(head, "ref: refs/heads/main\n", UTF_8);
        }
        return new FsObjectDatabase(root);
    }

    public Path pathFor(byte[] hash) {
        var sha = hex(hash);
        var dir = sha.substring(0, 2);
        var path = sha.substring(2);
        return root.resolve(".git/objects").resolve(dir).resolve(path);
    }

    @Override
    public ObjectType getType(byte[] hash) throws IOException, GitException {
        var deflated = Files.newInputStream(pathFor(hash));
        var inflated = new InflaterInputStream(deflated);
        String type = eatString(inflated, (byte) ' ').t.toString();
        return ObjectType.parse(type);
    }

    private record ObjectInputStream(ObjectType type, int size, InputStream stream) {
        InputStream as(ObjectType want) throws GitException {
            if (type != want) {
                throw new GitException("invalid object type: want %s, got %s".formatted(want, type));
            }
            return stream;
        }
    }

    private ObjectInputStream readObject(byte[] hash) throws IOException, GitException {
        var deflated = Files.newInputStream(pathFor(hash));
        var inflated = new InflaterInputStream(deflated);
        String type = eatString(inflated, (byte) ' ').t.toString();
        int size = eatInt(inflated, (byte) 0).t;
        return new ObjectInputStream(ObjectType.parse(type), size, inflated);
    }

    @Override
    public InputStream readBlob(byte[] sha) throws GitException, IOException {
        return readObject(sha).as(ObjectType.Blob);
    }

    private static String hex(byte[] bytes) {
        var hash = new StringBuilder();
        for (byte b : bytes)
            hash.append("%02x".formatted(b));
        return hash.toString();
    }

    private static final String SHA_1 = "SHA-1";

    @FunctionalInterface
    private interface CheckedStreamConsumer {
        void accept(OutputStream out) throws IOException;
    }

    private byte[] hashStream(OutputStream out, CheckedStreamConsumer f) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("can't find algorithm: %s".formatted(SHA_1));
        }
        var digester = new DigestOutputStream(out, digest);
        f.accept(digester);
        return digest.digest();
    }

    private byte[] writeObject(CheckedStreamConsumer f) throws IOException {
        Path temp = Files.createTempFile("git", "obj");
        try (var out = new DeflaterOutputStream(Files.newOutputStream(temp))) {
            byte[] hash = hashStream(out, f);
            Path target = pathFor(hash);
            Files.createDirectories(target.getParent());
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            return hash;
        }
    }

    private byte[] hashAndWriteBlob(OutputStream out, InputStream in, long size) throws IOException {
        return hashStream(out, digester -> {
            digester.write("blob %d".formatted(size).getBytes(UTF_8));
            digester.write((byte) 0);
            in.transferTo(digester);
        });
    }

    @Override
    public byte[] hashBlob(InputStream s, long size) throws IOException {
        return hashAndWriteBlob(OutputStream.nullOutputStream(), s, size);
    }

    @Override
    public byte[] writeBlob(InputStream s, long size) throws IOException {
        return writeObject(out -> hashAndWriteBlob(out, s, size));
    }

    private static Sized<Integer> eatInt(InputStream is, byte until) throws GitException, IOException {
        return eat(is, until, 0, (result, b) -> 10 * result + (b - '0'), b -> (b >= '0' && b <= '9'));
    }

    private static Sized<StringBuilder> eatString(InputStream is, byte until) throws GitException, IOException {
        return eat(is, until, new StringBuilder(), (s, b) -> s.append((char) b.byteValue()), b -> b <= 127);
    }

    @Override
    public List<TreeObject> listTree(byte[] hash) throws GitException, IOException {
        var obj = readObject(hash);
        InputStream stream = obj.as(ObjectType.Tree);
        var elems = new ArrayList<TreeObject>();
        for (int read = 0; read < obj.size;) {
            var mode = eatInt(stream, (byte) ' ');
            read += mode.size;
            var name = eatString(stream, (byte) 0);
            read += name.size;
            var objectHash = stream.readNBytes(20);
            var objectType = getType(objectHash);
            read += 20;
            elems.add(new TreeObject(name.t.toString(), objectType, mode.t, objectHash));
        }
        return elems;
    }

    private record Sized<T>(T t, int size) {
    }

    private static <T> Sized<T> eat(
            InputStream is, byte until,
            T initial, BiFunction<T, Byte, T> f,
            Predicate<Byte> valid)
            throws IOException, GitException {
        int read, n = 0;
        T acc = initial;
        while ((read = is.read()) >= 0) {
            n++;
            if (read == until) {
                break;
            }
            if (!valid.test((byte) read)) {
                throw new GitException("invalid git object: got %d".formatted(read));
            }
            acc = f.apply(acc, (byte) read);
        }
        if (read != until) {
            throw new GitException("invalid object: unexpected eof");
        }
        return new Sized<T>(acc, n);
    }

    private byte[] writeTree(Path base) throws GitException, IOException {
        var objects = new ArrayList<TreeObject>();
        long treeSize = 0;
        for (var path : Files.list(base).toList()) {
            if (path.equals(root.resolve(".git"))) {
                continue;
            }
            if (Files.isDirectory(path)) {
                var hash = writeTree(path);
                objects.add(new TreeObject(
                        base.relativize(path).toString(),
                        getType(hash), 40000, hash));
            } else {
                var hash = writeBlob(Files.newInputStream(path), Files.size(path));
                objects.add(new TreeObject(
                        base.relativize(path).toString(),
                        getType(hash), 100644, hash));
            }
            treeSize += Long.toString(objects.getLast().mode()).length();
            treeSize += 1; // space
            treeSize += objects.getLast().name().length(); // name
            treeSize += 1; // nul
            treeSize += 20; // sha
        }
        objects.sort(Comparator.comparing(TreeObject::name));
        final long size = treeSize;
        return writeObject(out -> {
            out.write("tree %d".formatted(size).getBytes(UTF_8));
            out.write((byte) 0);
            for (var object : objects) {
                out.write("%d %s".formatted(object.mode(), object.name()).getBytes(UTF_8));
                out.write((byte) 0);
                out.write(object.hash());
            }
        });
    }

    @Override
    public byte[] writeTree() throws GitException, IOException {
        return writeTree(root);
    }

    @Override
    public byte[] commitTree(byte[] treeHash, List<byte[]> parentCommitHashes, String message)
            throws GitException, IOException {
        var buf = new ByteArrayOutputStream();
        buf.write("tree ".getBytes(UTF_8));
        buf.write(hex(treeHash).getBytes(UTF_8));
        for (byte[] hash : parentCommitHashes) {
            buf.write("\nparent ".getBytes(UTF_8));
            buf.write(hex(hash).getBytes(UTF_8));
        }
        buf.write("\nauthor daniel connelly <dhconnelly@gmail.com> 0 +0000".getBytes(UTF_8));
        buf.write("\ncommitter daniel connelly <dhconnelly@gmail.com> 0 +0000".getBytes(UTF_8));
        buf.write("\n\n%s\n".formatted(message).getBytes(UTF_8));
        byte[] content = buf.toByteArray();
        return writeObject(out -> {
            out.write("commit %d".formatted(content.length).getBytes(UTF_8));
            out.write((byte) 0);
            out.write(content);
        });
    }
}
