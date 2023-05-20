import java.io.*;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        final var validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

        Server myServer = new Server();
        myServer.start(validPaths);

    }
}


