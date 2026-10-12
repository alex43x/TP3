/* Grupo: g_ts9
 * Tarea: TP3 - U4
 * Integrantes: Portillo Sales Angel Zacarias CI 7259245 TS y Llamosas Maidana Alex Giovanni CI 5631704 TS
 */
/* No es chequeada porque desde mayor que hasta viola el contrato del llamador. */
public class RangoInvalidoException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public RangoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
