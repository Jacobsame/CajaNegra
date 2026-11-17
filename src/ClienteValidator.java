import java.util.ArrayList;
import java.util.List;

/**
 * Reglas de validación del registro de clientes.
 * Nombre: 3 a 30 caracteres. Edad: entero entre 18 y 99.
 */
public class ClienteValidator {

    public static final int NOMBRE_MIN = 3;
    public static final int NOMBRE_MAX = 30;
    public static final int EDAD_MIN = 18;
    public static final int EDAD_MAX = 99;

    public static List<String> validar(String nombre, String edadTexto) {
        List<String> errores = new ArrayList<>();
        validarNombre(nombre, errores);
        validarEdad(edadTexto, errores);
        return errores;
    }

    private static void validarNombre(String nombre, List<String> errores) {
        if (nombre == null || nombre.isBlank()) {
            errores.add("El nombre es obligatorio");
            return;
        }
        int largo = nombre.trim().length();
        if (largo < NOMBRE_MIN) {
            errores.add("El nombre debe tener al menos " + NOMBRE_MIN + " caracteres");
        } else if (largo > NOMBRE_MAX) {
            errores.add("El nombre no puede superar " + NOMBRE_MAX + " caracteres");
        }
    }

    private static void validarEdad(String edadTexto, List<String> errores) {
        if (edadTexto == null || edadTexto.isBlank()) {
            errores.add("La edad es obligatoria");
            return;
        }
        int edad;
        try {
            edad = Integer.parseInt(edadTexto.trim());
        } catch (NumberFormatException e) {
            errores.add("La edad debe ser un número entero");
            return;
        }
        if (edad < EDAD_MIN || edad > EDAD_MAX) {
            errores.add("La edad debe estar entre " + EDAD_MIN + " y " + EDAD_MAX);
        }
    }
}
