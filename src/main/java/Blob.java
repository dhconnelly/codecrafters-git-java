import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.zip.InflaterInputStream;

public record Blob(int size, ReadableByteChannel content) {
  private static void eat(InputStream is, byte b) throws IOException {
    int read = is.read();
    if (read < 0) throw new IllegalArgumentException("unexpected eof");
    if (read != b) {
      throw new IllegalArgumentException(
          "bad blob: want %d, got %d".formatted(b, read));
    }
  }

  private static int eatInt(InputStream is, byte until) throws IOException {
    int result = 0;
    int read;
    while ((read = is.read()) >= 0 && read != until) {
      if (read < '0' || read > '9') {
        throw new IllegalArgumentException(
            "bad blob: expected int, got %d".formatted(read));
      }
      result = 10 * result + (read - '0');
    }
    return result;
  }

  public static Blob parse(ByteChannel f) throws IOException {
    var deflated = Channels.newInputStream(f);
    var inflated = new InflaterInputStream(deflated);
    eat(inflated, (byte) 'b');
    eat(inflated, (byte) 'l');
    eat(inflated, (byte) 'o');
    eat(inflated, (byte) 'b');
    eat(inflated, (byte) ' ');
    int size = eatInt(inflated, (byte) 0);
    var channel = Channels.newChannel(inflated);
    return new Blob(size, channel);
  }
}
