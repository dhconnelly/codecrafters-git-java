import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class Main {
  private static void die(Exception e) {
    e.printStackTrace(System.err);
    System.exit(1);
  }

  private static void init() {
    try {
      // .git/
      final File root = new File(".git");
      // .git/objects/
      new File(root, "objects").mkdirs();
      // .git/refs/
      new File(root, "refs").mkdirs();
      // .git/HEAD
      final File head = new File(root, "HEAD");
      head.createNewFile();
      Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
    } catch (IOException e) {
      die(e);
    }
  }

  private static void catFile(List<String> opts) {
    if (opts.size() != 2 || !opts.getFirst().equals("-p")) {
      throw new IllegalArgumentException("usage: git cat-file -p <sha>");
    }
    var sha = opts.get(1);
    if (sha.length() != 40) {
      throw new IllegalArgumentException("bad sha: expected 40-byte sha-1");
    }
    var dir = sha.substring(0, 2);
    var path = sha.substring(2);
    try {
      // TODO: support arbitrary roots
      var blob = Blob.parse(
          Files.newByteChannel(Path.of(".git", "objects", dir, path)));
      try (var chan = blob.content()) {
        var content = new byte[blob.size()];
        chan.read(ByteBuffer.wrap(content));
        // TODO: handle non-UTF8 content
        var decoded = new String(content, StandardCharsets.UTF_8);
        System.out.print(decoded);
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
      default -> System.out.println("Unknown command: " + command);
    }
  }
}
