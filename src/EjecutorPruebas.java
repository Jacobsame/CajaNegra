import com.sun.net.httpserver.HttpServer;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Ejecuta la tabla de casos de caja negra contra el servidor real
 * y compara el código HTTP obtenido con el esperado.
 */
public class EjecutorPruebas {

    record Caso(String id, String nombre, String edad, String particion, String tecnica, int esperado) {}

    static String repetir(char c, int n) { return String.valueOf(c).repeat(n); }

    public static void main(String[] args) throws Exception {
        List<Caso> casos = List.of(
            // ---- Edad (nombre válido fijo: "Carlos") ----
            new Caso("CP-01", "Carlos", "17",  "Edad inválida < 18",       "BVA (límite inferior - 1)", 400),
            new Caso("CP-02", "Carlos", "18",  "Edad válida 18-99",        "BVA (límite inferior)",     200),
            new Caso("CP-03", "Carlos", "19",  "Edad válida 18-99",        "BVA (inferior + 1)",        200),
            new Caso("CP-04", "Carlos", "45",  "Edad válida 18-99",        "Partición equivalencia",    200),
            new Caso("CP-05", "Carlos", "98",  "Edad válida 18-99",        "BVA (superior - 1)",        200),
            new Caso("CP-06", "Carlos", "99",  "Edad válida 18-99",        "BVA (límite superior)",     200),
            new Caso("CP-07", "Carlos", "100", "Edad inválida > 99",       "BVA (límite superior + 1)", 400),
            new Caso("CP-08", "Carlos", "abc", "Edad inválida no numérica","Partición equivalencia",    400),
            // ---- Nombre (edad válida fija: 25) ----
            new Caso("CP-09", repetir('a', 2),  "25", "Nombre inválido < 3",    "BVA (límite inferior - 1)", 400),
            new Caso("CP-10", repetir('a', 3),  "25", "Nombre válido 3-30",     "BVA (límite inferior)",     200),
            new Caso("CP-11", repetir('a', 4),  "25", "Nombre válido 3-30",     "BVA (inferior + 1)",        200),
            new Caso("CP-12", "Mariana",        "25", "Nombre válido 3-30",     "Partición equivalencia",    200),
            new Caso("CP-13", repetir('a', 29), "25", "Nombre válido 3-30",     "BVA (superior - 1)",        200),
            new Caso("CP-14", repetir('a', 30), "25", "Nombre válido 3-30",     "BVA (límite superior)",     200),
            new Caso("CP-15", repetir('a', 31), "25", "Nombre inválido > 30",   "BVA (límite superior + 1)", 400),
            new Caso("CP-16", "",               "25", "Nombre inválido vacío",  "Partición equivalencia",    400)
        );

        HttpServer server = ServidorRegistro.iniciar(8089);
        HttpClient cliente = HttpClient.newHttpClient();
        int ok = 0;

        System.out.printf("%-6s | %-31s | %-5s | %-26s | %-27s | %-3s | %-3s | %s%n",
            "ID", "Nombre (largo)", "Edad", "Partición", "Técnica", "Esp", "Obt", "Resultado");
        System.out.println("-".repeat(150));

        for (Caso c : casos) {
            String cuerpo = "nombre=" + URLEncoder.encode(c.nombre(), StandardCharsets.UTF_8)
                          + "&edad=" + URLEncoder.encode(c.edad(), StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:8089/clientes"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build();
            int obtenido = cliente.send(req, HttpResponse.BodyHandlers.ofString()).statusCode();
            boolean paso = obtenido == c.esperado();
            if (paso) ok++;
            String nombreMostrado = c.nombre().length() > 12
                ? c.nombre().substring(0, 3) + "… (" + c.nombre().length() + ")"
                : "\"" + c.nombre() + "\" (" + c.nombre().length() + ")";
            System.out.printf("%-6s | %-31s | %-5s | %-26s | %-27s | %-3d | %-3d | %s%n",
                c.id(), nombreMostrado, c.edad(), c.particion(), c.tecnica(),
                c.esperado(), obtenido, paso ? "PASÓ" : "FALLÓ");
        }
        System.out.println("-".repeat(150));
        System.out.printf("Total: %d/%d casos pasaron%n", ok, casos.size());
        server.stop(0);
        if (ok != casos.size()) System.exit(1);
    }
}
