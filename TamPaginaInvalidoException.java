/* Grupo: g_ts9
 * Tarea: TP3 - U4
 * Integrantes: Portillo Sales Angel Zacarias CI 7259245 TS y Llamosas Maidana Alex Giovanni CI 5631704 TS
 */
/* No es chequeada porque el tamano de pagina es una precondicion de crear. */
public class TamPaginaInvalidoException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public TamPaginaInvalidoException(String mensaje) {
        super(mensaje);
    }
}
