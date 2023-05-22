import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

public class Server {

    private static final int DEFAULT_THREAD_POOL_SIZE = 64;
    private static final int DEFAULT_PORT = 9999;
    private int threadPoolSize;
    private int port;
    ThreadPoolExecutor executor;

    public Server() {
        this(DEFAULT_THREAD_POOL_SIZE, DEFAULT_PORT);
    }

    public Server(int threadPoolSize, int port) {
        this.threadPoolSize = threadPoolSize;
        this.port = port;
        ThreadPoolExecutor executor;
    }

    public void start(List<String> validPaths) throws IOException {
        this.start(DEFAULT_PORT, validPaths);
    }

    public void start(int port, List<String> validPaths) throws IOException {
//        executor = new ThreadPoolExecutor(threadPoolSize, threadPoolSize, 1000, null, null);

        try (final var serverSocket = new ServerSocket(port)) {

            while (true) {
                try (
                        final var socket = serverSocket.accept();
                ) {
                    executor = new ThreadPoolExecutor(threadPoolSize, threadPoolSize, 1000, null, null);
                    executor.execute(() -> {
                        try {
                            handle(socket, validPaths);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


//        ThreadPoolExecutor executor = new ThreadPoolExecutor(, , , , );
//        executor.execute();
//        ExecutorService threadPool = Executors.newFixedThreadPool(this.threadPoolSize);
    }

    private void handle(Socket socket, List<String> validPaths) throws IOException {
        final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        final var out = new BufferedOutputStream(socket.getOutputStream());

        // read only request line for simplicity
        // must be in form GET /path HTTP/1.1
        final var requestLine = in.readLine();
        final var parts = requestLine.split(" ");

        if (parts.length != 3) {
            // just close socket
            return;
        }

        final var path = parts[1];
        if (!validPaths.contains(path)) {
            out.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
            return;
        }

        final var filePath = Path.of(".", "public", path);
        final var mimeType = Files.probeContentType(filePath);

        // special case for classic
        if (path.equals("/classic.html")) {
            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.write(content);
            out.flush();
            return;
        }

        final var length = Files.size(filePath);
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        Files.copy(filePath, out);
        out.flush();

    }
}
