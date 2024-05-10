import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

import javax.net.ssl.SSLSocketFactory;

public class GitSmartClient {
    private final String host;
    private final String repoPath;

    public GitSmartClient(String host, String repoPath) {
        this.host = host;
        this.repoPath = repoPath;
    }

    private class GitSocket implements AutoCloseable {
        private final Socket socket;
        private final InputStream in;
        private final OutputStream out;

        GitSocket() throws IOException {
            SSLSocketFactory ssl = (SSLSocketFactory) SSLSocketFactory.getDefault();
            socket = ssl.createSocket(host, 443);
            in = new BufferedInputStream(socket.getInputStream());
            out = new BufferedOutputStream(socket.getOutputStream());
        }

        @Override
        public void close() throws IOException {
            out.close();
            in.close();
            socket.close();
        }

        void sendLine(String line) throws IOException {
            out.write(line.getBytes(US_ASCII));
            out.write(new byte[] { '\r', '\n' });
            out.flush();
        }

        void sendLine() throws IOException {
            sendLine("");
        }

        String readLine() throws IOException {
            var line = new StringBuilder();
            int c;
            while ((c = in.read()) >= 0) {
                line.append((char) c);
                int n = line.length();
                if (n >= 2 && line.charAt(n - 2) == '\r' && line.charAt(n - 1) == '\n') {
                    return line.substring(0, n - 2);
                }
            }
            return line.toString();
        }
    }

    public List<byte[]> listRefs() throws IOException {
        try (var sock = new GitSocket()) {
            System.out.println("connected.");
            sock.sendLine("GET %s.git/info/refs?service=git-upload-pack HTTP/1.0".formatted(repoPath));
            sock.sendLine("Host: %s".formatted(host));
            sock.sendLine("Git-Protocol: version=2");
            sock.sendLine();
            for (String line; !(line = sock.readLine()).isEmpty();) {
                System.out.println(line);
            }
        }
        return List.of();
    }
}
