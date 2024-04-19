import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

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

  public static void main(String[] args) {
    if (args.length == 0) {
      die(new IllegalArgumentException("usage: git <command>"));
    }
    final String command = args[0];
    switch (command) {
      case "init" -> init();
      default -> System.out.println("Unknown command: " + command);
    }
  }
}
