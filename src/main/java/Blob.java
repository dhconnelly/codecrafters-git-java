import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public record Blob(long size, ReadableByteChannel content) {
  private static final String SHA_1 = "SHA-1";

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

  private ByteBuffer encodeUncompressed() throws IOException {
    var header = "blob %d".formatted(size);
    var encoded = ByteBuffer.allocate((int) (header.length() + 1 + size));
    encoded.put(header.getBytes(StandardCharsets.UTF_8));
    encoded.put((byte) 0);
    // TODO: avoid reading it all into memory
    var contentBuf = ByteBuffer.allocate((int) size);
    content.read(contentBuf);
    contentBuf.rewind();
    encoded.put(contentBuf);
    encoded.rewind();
    return encoded;
  }

  public void write(WritableByteChannel out) throws IOException {
    var stream = Channels.newOutputStream(out);
    var deflater = new DeflaterOutputStream(stream);
    try (var chan = Channels.newChannel(deflater)) {
      var written = chan.write(encodeUncompressed());
      System.out.printf("wrote %d bytes\n", written);
    }
  }

  public String hash() throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance(SHA_1);
      // TODO: update in chunks to avoid OOM
      var bs = encodeUncompressed();
      digest.update(bs);
      var hash = new StringBuilder();
      for (byte b : digest.digest()) hash.append("%02x".formatted(b));
      return hash.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError("can't find algorithm: %s".formatted(SHA_1));
    }
  }
}
