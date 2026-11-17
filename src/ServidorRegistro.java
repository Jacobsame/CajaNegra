import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servidor HTTP mínimo (sin frameworks).
 * GET  /          página web (web/index.html)
 * POST /clientes  con cuerpo  nombre=...&edad=...
 *   200 -> datos válidos
 *   400 -> datos inválidos
 *   405 -> método distinto de POST
 */
public class ServidorRegistro {

    public static HttpServer iniciar(int puerto) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(puerto), 0);
        // Página web: sirve web/index.html (ejecutar desde la raíz del proyecto)
        server.createContext("/", exchange -> {
            if (!"/".equals(exchange.getRequestURI().getPath())) {
                responder(exchange, 404, "No encontrado");
                return;
            }
            byte[] html = Files.readAllBytes(Path.of("web", "index.html"));
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, html.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(html);
            }
        });

        server.createContext("/clientes", exchange -> {
            if (!"POST".equals(exchange.getRequestMethod())) {
                responder(exchange, 405, "Método no permitido");
                return;
            }
            String cuerpo = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> campos = parsear(cuerpo);
            List<String> errores = ClienteValidator.validar(campos.get("nombre"), campos.get("edad"));
            if (errores.isEmpty()) {
                responder(exchange, 200, "Cliente registrado: " + campos.get("nombre").trim());
            } else {
                responder(exchange, 400, String.join("; ", errores));
            }
        });
        server.start();
        return server;
    }

    private static Map<String, String> parsear(String cuerpo) {
        Map<String, String> mapa = new HashMap<>();
        if (cuerpo.isBlank()) return mapa;
        for (String par : cuerpo.split("&")) {
            String[] kv = par.split("=", 2);
            String clave = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String valor = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            mapa.put(clave, valor);
        }
        return mapa;
    }

    private static void responder(com.sun.net.httpserver.HttpExchange ex, int codigo, String texto) throws IOException {
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void main(String[] args) throws IOException {
        iniciar(8080);
        System.out.println("Abre http://localhost:8080 en el navegador");
    }
}
