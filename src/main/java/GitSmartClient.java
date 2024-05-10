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
    private static final String CONTENT_TYPE = "application/x-git-upload-pack-advertisement";

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

    private void validateStatus(String statusLine) throws GitRemoteException {
        var toks = statusLine.split(" ");
        int code = Integer.parseInt(toks[1]);
        if (code != 200) {
            throw new GitRemoteException("expected http status 200, got %d".formatted(code));
        }
    }

    private void validateHeader(String headerLine) throws GitRemoteException {
        var toks = headerLine.split(": ");
        String name = toks[0], value = toks[1];
        if (name.equals("Content-Type") && !value.equals(CONTENT_TYPE)) {
            throw new GitRemoteException("expected content type %s, got %s".formatted(CONTENT_TYPE, value));
        }
    }

    public List<byte[]> listRefs() throws IOException, GitRemoteException {
        try (var sock = new GitSocket()) {
            System.out.println("connected.");
            sock.sendLine("GET %s.git/info/refs?service=git-upload-pack HTTP/1.0".formatted(repoPath));
            sock.sendLine("Host: %s".formatted(host));
            sock.sendLine("Git-Protocol: version=2");
            sock.sendLine();
            validateStatus(sock.readLine());
            for (String header; !(header = sock.readLine()).isEmpty();) {
                validateHeader(header);
            }
        }
        return List.of();
    }
}
