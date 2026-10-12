/* Grupo: g_ts9
 * Tarea: TP3 - U4
 * Integrantes: Portillo Sales Angel Zacarias CI 7259245 TS y Llamosas Maidana Alex Giovanni CI 5631704 TS
 */
import java.io.IOException;

/* Es chequeada porque un archivo externo invalido es una falla de E/S recuperable. */
public class FormatoInvalidoException extends IOException {
    private static final long serialVersionUID = 1L;

    public FormatoInvalidoException(String mensaje) {
        super(mensaje);
    }

    public FormatoInvalidoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
