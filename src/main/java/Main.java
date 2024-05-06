import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

public class Main {
    private static void die(Exception e) {
        e.printStackTrace(System.err);
        System.exit(1);
    }

    private static void die(String message) {
        System.err.println(message);
        System.exit(1);
    }

    private static void init() {
        try {
            FsObjectDatabase.init(Path.of("."));
        } catch (Exception e) {
            die(e);
        }
    }

    private static String hex(byte[] bytes) {
        var hash = new StringBuilder();
        for (byte b : bytes)
            hash.append("%02x".formatted(b));
        return hash.toString();
    }

    private static byte[] fromHex(String hash) {
        return HexFormat.of().parseHex(hash);
    }

    private static void catFile(List<String> opts) {
        if (opts.size() != 2 || !opts.getFirst().equals("-p")) {
            die("usage: git cat-file -p <sha>");
        }
        var sha = opts.get(1);
        if (sha.length() != 40) {
            die("bad sha: expected 40-byte sha-1");
        }
        try {
            var git = FsObjectDatabase.init(Path.of("."));
            try (var content = git.readBlob(fromHex(sha))) {
                content.transferTo(System.out);
            }
        } catch (Exception e) {
            die(e);
        }
    }

    private static void hashObject(List<String> opts) {
        boolean write = false;
        var path = Optional.<Path>empty();
        for (var opt : opts) {
            switch (opt) {
                case "-w" -> write = true;
                default -> path = Optional.of(Path.of(opt));
            }
        }
        if (path.isEmpty()) {
            die("usage: git hash-object [-w] <path>");
        }
        try (var s = Files.newInputStream(path.get())) {
            var git = FsObjectDatabase.init(Path.of("."));
            String hash = write
                    ? hex(git.writeBlob(s, Files.size(path.get())))
                    : hex(git.hashBlob(s, Files.size(path.get())));
            System.out.println(hash);
        } catch (Exception e) {
            die(e);
        }
    }

    private static void lsTree(List<String> opts) {
        if (opts.size() != 2) {
            die("usage: git ls-tree --name-only <hash>");
        }
        try {
            var git = FsObjectDatabase.init(Path.of("."));
            for (var obj : git.listTree(fromHex(opts.get(1)))) {
                System.out.println(obj.name());
            }
        } catch (Exception e) {
            die(e);
        }
    }

    private static void writeTree(List<String> opts) {
        if (!opts.isEmpty()) {
            die("usage: git write-tree");
        }
        try {
            var git = FsObjectDatabase.init(Path.of("."));
            System.out.println(hex(git.writeTree()));
        } catch (Exception e) {
            die(e);
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            die("usage: git <command>");
        }
        // TODO: command parsing
        final String command = args[0];
        final List<String> opts = Arrays.asList(args).subList(1, args.length);
        switch (command) {
            case "init" -> init();
            case "cat-file" -> catFile(opts);
            case "hash-object" -> hashObject(opts);
            case "ls-tree" -> lsTree(opts);
            case "write-tree" -> writeTree(opts);
            default -> System.out.println("Unknown command: " + command);
        }
    }
}
