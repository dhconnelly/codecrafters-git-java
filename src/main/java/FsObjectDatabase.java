import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
            Files.writeString(head, "ref: refs/heads/main\n", StandardCharsets.UTF_8);
        }
        return new FsObjectDatabase(root);
    }

    public Path pathFor(String sha) {
        var dir = sha.substring(0, 2);
        var path = sha.substring(2);
        return root.resolve(".git/objects").resolve(dir).resolve(path);
    }

    @Override
    public InputStream readObject(String sha) throws GitException, IOException {
        var deflated = Files.newInputStream(pathFor(sha));
        var inflated = new InflaterInputStream(deflated);
        var type = eatString(inflated, (byte) ' ');
        return switch (type) {
            case "blob" -> {
                eatInt(inflated, (byte) 0); // size
                yield inflated;
            }
            default -> throw new UnsupportedOperationException("TODO");
        };
    }

    private static String hex(byte[] bytes) {
        var hash = new StringBuilder();
        for (byte b : bytes)
            hash.append("%02x".formatted(b));
        return hash.toString();
    }

    private static final String SHA_1 = "SHA-1";

    private String hashObject(OutputStream out, InputStream in, long size) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("can't find algorithm: %s".formatted(SHA_1));
        }

        var digester = new DigestOutputStream(out, digest);
        digester.write("blob %d".formatted(size).getBytes(StandardCharsets.UTF_8));
        digester.write((byte) 0);
        in.transferTo(digester);

        var hash = hex(digest.digest());
        return hash;
    }

    @Override
    public String hashObject(InputStream s, long size) throws IOException {
        return hashObject(OutputStream.nullOutputStream(), s, size);
    }

    @Override
    public String writeObject(InputStream s, long size) throws IOException {
        Path temp = Files.createTempFile("git", "obj");
        try (var out = new DeflaterOutputStream(Files.newOutputStream(temp))) {
            String hash = hashObject(out, s, size);
            Path target = pathFor(hash);
            Files.createDirectories(target.getParent());
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            return hash;
        }
    }

    private static int eatInt(InputStream is, byte until) throws GitException, IOException {
        return eat(is, until, 0, (result, b) -> 10 * result + (b - '0'), b -> (b >= '0' && b <= '9'));
    }

    private static String eatString(InputStream is, byte until) throws GitException, IOException {
        return eat(is, until, new StringBuilder(), (s, b) -> s.append((char) b.byteValue()), b -> b <= 127).toString();
    }

    private static <T> T eat(InputStream is, byte until, T initial, BiFunction<T, Byte, T> f, Predicate<Byte> valid)
            throws IOException, GitException {
        int read;
        T acc = initial;
        while ((read = is.read()) >= 0 && read != until) {
            if (!valid.test((byte) read)) {
                throw new GitException("invalid git object: got %d".formatted(read));
            }
            acc = f.apply(acc, (byte) read);
        }
        if (read != until) {
            throw new GitException("invalid object: unexpected eof");
        }
        return acc;
    }
}
