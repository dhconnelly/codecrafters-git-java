import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Main {
    private static void die(Exception e) {
        e.printStackTrace(System.err);
        System.exit(1);
    }

    private static void init() {
        try {
            FsObjectDatabase.init(Path.of("."));
        } catch (Exception e) {
            die(e);
        }
    }

    private static Path pathFor(String sha) {
        var dir = sha.substring(0, 2);
        var path = sha.substring(2);
        return Path.of(".git", "objects", dir, path);
    }

    private static void catFile(List<String> opts) {
        if (opts.size() != 2 || !opts.getFirst().equals("-p")) {
            throw new IllegalArgumentException("usage: git cat-file -p <sha>");
        }
        var sha = opts.get(1);
        if (sha.length() != 40) {
            throw new IllegalArgumentException("bad sha: expected 40-byte sha-1");
        }
        try {
            // TODO: support arbitrary roots
            var blob = Blob.parse(Files.newByteChannel(pathFor(sha)));
            try (var chan = blob.content()) {
                var content = new byte[(int) blob.size()];
                chan.read(ByteBuffer.wrap(content));
                // TODO: handle non-UTF8 content
                var decoded = new String(content, StandardCharsets.UTF_8);
                System.out.print(decoded);
            }
        } catch (IOException e) {
            die(e);
        }
    }

    private static void hashObject(List<String> opts) {
        boolean write = false;
        var path = Optional.<String>empty();
        for (var opt : opts) {
            if (opt.equals("-w"))
                write = true;
            else
                path = Optional.of(opt);
        }
        if (path.isEmpty()) {
            throw new IllegalArgumentException("usage: git hash-object [-w] <path>");
        }
        try (var in = Files.newByteChannel(Path.of(path.get()))) {
            var blob = new Blob(in.size(), in);
            var sha = blob.hash();
            System.out.println(sha);
            var outPath = pathFor(sha);
            Files.createDirectories(outPath.getParent());
            if (write)
                try (var out = Files.newByteChannel(
                        outPath,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING)) {
                    // TODO: this is a mess, back blob by a file?
                    in.position(0);
                    new Blob(in.size(), in).write(out);
                }
        } catch (IOException e) {
            die(e);
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            die(new IllegalArgumentException("usage: git <command>"));
        }
        // TODO: command parsing
        final String command = args[0];
        final List<String> opts = Arrays.asList(args).subList(1, args.length);
        switch (command) {
            case "init" -> init();
            case "cat-file" -> catFile(opts);
            case "hash-object" -> hashObject(opts);
            default -> System.out.println("Unknown command: " + command);
        }
    }
}
