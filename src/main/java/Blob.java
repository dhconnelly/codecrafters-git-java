import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public record Blob(int size, ReadableByteChannel content) {
  public static Blob parse(ByteChannel f) {
    // deflate with zlib
    // parse "blob " <size> '\0' <content>
    var empty = Channels.newChannel(new ByteArrayInputStream(new byte[0]));
    return new Blob(0, empty);
  }
}
