/* Grupo: g_ts9
 * Tarea: TP3-U4
 * Integrantes: Portillo Sales Angel Zacarias CI 7259245 TS y Llamosas Maidana Alex Giovanni CI 5631704 TS
 */
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public final class TestArbolBMas {
    private static final int P = 60;
    private static final String RUTA = "test-arbol-bmas.dat";
    private static final String RUTA_INVALIDA = "test-arbol-bmas-invalido.dat";
    private static int casosOK;

    private TestArbolBMas() {
    }

    public static void main(String[] args) throws Exception {
        File archivo = new File(RUTA);
        File archivoInvalido = new File(RUTA_INVALIDA);
        archivo.delete();
        archivoInvalido.delete();
        ArbolBMas arbol = null;
        try {
            arbol = ArbolBMas.crear(RUTA, P);
            String vacio = "1: #1[]";
            arbol.reiniciarContadores();
            long busquedaVacia = arbol.buscar(50);
            boolean caso1 = arbol.niveles() == 1 && arbol.cantidadPaginas() == 2
                    && arbol.raiz() == 1 && arbol.cantidadClaves() == 0
                    && busquedaVacia == -1 && arbol.paginasLeidas() == 1
                    && arbol.paginasEscritas() == 0 && vacio.equals(arbol.volcarArbol())
                    && "OK".equals(arbol.verificarInvariantes());
            imprimirCaso(1, "arbol vacio: niveles=1 paginas=2 buscar(50)=-1, " + vacio, caso1);
            System.out.println("Traza completa P=60");
            System.out.println("(crear): paginas=2");
            System.out.println(vacio);
            System.out.println("verificarInvariantes(): " + arbol.verificarInvariantes());

            boolean caso2 = true;
            caso2 &= insertarYVerificar(arbol, 50, 1, 1, 2,
                    "1: #1[50]", true);
            caso2 &= insertarYVerificar(arbol, 20, 1, 1, 2,
                    "1: #1[20,50]", true);
            caso2 &= insertarYVerificar(arbol, 80, 1, 1, 2,
                    "1: #1[20,50,80]", true);
            imprimirCaso(2, "inserciones sin division (50, 20, 80)", caso2);

            boolean caso3 = insertarYVerificar(arbol, 10, 1, 3, 4,
                    "1: #3(50)\n2: #1[10,20] | #2[50,80]", true);
            caso3 &= insertarYVerificar(arbol, 30, 2, 1, 4,
                    "1: #3(50)\n2: #1[10,20,30] | #2[50,80]", true);
            caso3 &= insertarYVerificar(arbol, 60, 2, 1, 4,
                    "1: #3(50)\n2: #1[10,20,30] | #2[50,60,80]", true);
            caso3 &= insertarYVerificar(arbol, 90, 2, 3, 5,
                    "1: #3(50,80)\n2: #1[10,20,30] | #2[50,60] | #4[80,90]", true);
            caso3 &= insertarYVerificar(arbol, 40, 2, 3, 6,
                    "1: #3(30,50,80)\n2: #1[10,20] | #5[30,40] | #2[50,60] | #4[80,90]", true);
            caso3 &= "#5 H n=2 sig=2 [30:300 40:400]".equals(arbol.volcarPagina(5));
            imprimirCaso(3, "division de raiz hoja (10) y hoja intermedia reenlazada (40)", caso3);

            boolean preparacion = insertarYVerificar(arbol, 70, 2, 1, 6,
                    "1: #3(30,50,80)\n2: #1[10,20] | #5[30,40] | #2[50,60,70] | #4[80,90]", true);
            preparacion &= insertarYVerificar(arbol, 25, 2, 1, 6,
                    "1: #3(30,50,80)\n2: #1[10,20,25] | #5[30,40] | #2[50,60,70] | #4[80,90]", true);
            preparacion &= insertarYVerificar(arbol, 35, 2, 1, 6,
                    "1: #3(30,50,80)\n2: #1[10,20,25] | #5[30,35,40] | #2[50,60,70] | #4[80,90]", true);
            String despues45 = "1: #8(50)\n"
                    + "2: #3(30,40) | #7(80)\n"
                    + "3: #1[10,20,25] | #5[30,35] | #6[40,45] | #2[50,60,70] | #4[80,90]";
            boolean insercion45 = insertarYVerificar(arbol, 45, 2, 5, 9, despues45, true);
            boolean caso4 = preparacion && insercion45
                    && "#3 I n=2 [#1 30 #5 40 #6]".equals(arbol.volcarPagina(3))
                    && "#7 I n=1 [#2 80 #4]".equals(arbol.volcarPagina(7));
            imprimirCaso(4, "division de nodo interno al insertar 45", caso4);

            boolean caso5 = insercion45 && arbol.raiz() == 8 && arbol.cantidadPaginas() == 9
                    && "#8 I n=1 [#3 50 #7]".equals(arbol.volcarPagina(8));
            imprimirCaso(5, "division de la raiz y raiz nueva #8", caso5);

            String finalEsperado = "1: #8(50)\n"
                    + "2: #3(20,30,40) | #7(80)\n"
                    + "3: #1[10,15] | #9[20,25] | #5[30,35] | #6[40,45] | #2[50,60,70] | #4[80,90]";
            boolean caso6 = arbol.niveles() == 3;
            caso6 &= insertarYVerificar(arbol, 15, 3, 3, 10, finalEsperado, true);
            caso6 &= arbol.niveles() == 3 && arbol.raiz() == 8
                    && "#5 H n=2 sig=6 [30:300 35:350]".equals(arbol.volcarPagina(5))
                    && "#3 I n=3 [#1 20 #9 30 #5 40 #6]".equals(arbol.volcarPagina(3));
            imprimirCaso(6, "crecimiento a tres niveles y formatos exactos", caso6);

            arbol.reiniciarContadores();
            long primera = arbol.buscar(10);
            long lecturasPrimera = arbol.paginasLeidas();
            arbol.reiniciarContadores();
            long ultima = arbol.buscar(90);
            long lecturasUltima = arbol.paginasLeidas();
            arbol.reiniciarContadores();
            int[] camino = arbol.caminoBusqueda(35);
            boolean caso7 = primera == 100 && ultima == 900
                    && lecturasPrimera == 3 && lecturasUltima == 3
                    && iguales(camino, 8, 3, 5);
            imprimirCaso(7, "buscar(10)=100, buscar(90)=900, camino(35)=[8, 3, 5]", caso7);

            arbol.reiniciarContadores();
            long inexistente = arbol.buscar(33);
            long lecturasInexistente = arbol.paginasLeidas();
            boolean duplicado = insertarDuplicadoYVerificar(arbol, finalEsperado);
            boolean caso8 = inexistente == -1 && lecturasInexistente == 3
                    && duplicado && arbol.cantidadClaves() == 13;
            imprimirCaso(8, "buscar(33)=-1 y duplicado insertar(35,1)=false", caso8);

            arbol.reiniciarContadores();
            ResultadoRango dentro = buscarRango(arbol, 31, 35);
            boolean caso9 = dentro.cantidad == 1 && "35".equals(dentro.claves)
                    && arbol.paginasLeidas() == 3 && arbol.paginasEscritas() == 0;
            imprimirCaso(9, "rango [31,35]: 35, R=3", caso9);

            arbol.reiniciarContadores();
            ResultadoRango varias = buscarRango(arbol, 22, 62);
            long lecturasVarias = arbol.paginasLeidas();
            arbol.reiniciarContadores();
            ResultadoRango parada = buscarRango(arbol, 20, 45);
            long lecturasParada = arbol.paginasLeidas();
            arbol.reiniciarContadores();
            boolean rangoInvalido = false;
            try {
                arbol.buscarRango(9, 5, (clave, posicion) -> { });
            } catch (RangoInvalidoException e) {
                rangoInvalido = true;
            }
            boolean caso10 = varias.cantidad == 7
                    && "25 30 35 40 45 50 60".equals(varias.claves) && lecturasVarias == 6
                    && parada.cantidad == 6 && "20 25 30 35 40 45".equals(parada.claves)
                    && lecturasParada == 5 && rangoInvalido
                    && arbol.paginasLeidas() == 0 && arbol.paginasEscritas() == 0;
            imprimirCaso(10, "rangos entre varias hojas R=6, parada en #6 R=5 y rango invalido", caso10);

            arbol.close();
            arbol = null;
            boolean cabecera = verificarCabecera(archivo);
            try (RandomAccessFile invalido = new RandomAccessFile(archivoInvalido, "rw")) {
                invalido.setLength(0);
                invalido.writeLong(0L);
            }
            boolean rechazo = false;
            try (ArbolBMas ignorado = ArbolBMas.abrir(RUTA_INVALIDA)) {
                rechazo = false;
            } catch (FormatoInvalidoException e) {
                rechazo = true;
            }
            arbol = ArbolBMas.abrir(RUTA);
            boolean caso11 = cabecera && rechazo && arbol.raiz() == 8 && arbol.niveles() == 3
                    && arbol.cantidadPaginas() == 10 && arbol.cantidadClaves() == 13
                    && arbol.paginasLeidas() == 0 && arbol.paginasEscritas() == 0
                    && "OK".equals(arbol.verificarInvariantes());
            imprimirCaso(11, "cierre/reapertura, cabecera, tamano=600 y archivo invalido", caso11);

            arbol.reiniciarContadores();
            long reabierta45 = arbol.buscar(45);
            long reabierta15 = arbol.buscar(15);
            long lecturasReabiertas = arbol.paginasLeidas();
            arbol.reiniciarContadores();
            ResultadoRango rangoReabierto = buscarRango(arbol, 20, 45);
            boolean caso12 = reabierta45 == 450 && reabierta15 == 150
                    && lecturasReabiertas == 6 && rangoReabierto.cantidad == 6
                    && "20 25 30 35 40 45".equals(rangoReabierto.claves)
                    && arbol.paginasLeidas() == 5
                    && finalEsperado.equals(arbol.volcarArbol())
                    && "OK".equals(arbol.verificarInvariantes());
            imprimirCaso(12, "busquedas reabiertas: 45=450, 15=150, R=6 y rango estable", caso12);

            System.out.println("Resumen: " + casosOK + "/12 casos OK - "
                    + (casosOK == 12 ? "OK" : "FALLA"));
        } finally {
            if (arbol != null) {
                try {
                    arbol.close();
                } catch (IOException e) {
                    System.out.println("FALLA al cerrar: " + e.getMessage());
                }
            }
            archivo.delete();
            archivoInvalido.delete();
        }
    }

    private static boolean insertarYVerificar(ArbolBMas arbol, long clave, long rEsperadas,
                                               long wEsperadas, int paginasEsperadas,
                                               String arbolEsperado, boolean retornoEsperado)
            throws IOException {
        arbol.reiniciarContadores();
        boolean retorno = arbol.insertar(clave, clave * 10);
        String volcado = arbol.volcarArbol();
        String invariantes = arbol.verificarInvariantes();
        long r = arbol.paginasLeidas();
        long w = arbol.paginasEscritas();
        boolean correcto = retorno == retornoEsperado && r == rEsperadas && w == wEsperadas
                && arbol.cantidadPaginas() == paginasEsperadas
                && arbolEsperado.equals(volcado) && "OK".equals(invariantes);
        System.out.println("insertar(" + clave + "): resultado=" + retorno + " R=" + r
                + " W=" + w + " paginas=" + arbol.cantidadPaginas());
        System.out.println(volcado);
        System.out.println("verificarInvariantes(): " + invariantes + " - "
                + (correcto ? "OK" : "FALLA"));
        return correcto;
    }

    private static ResultadoRango buscarRango(ArbolBMas arbol, long desde, long hasta)
            throws IOException {
        StringBuilder claves = new StringBuilder();
        int cantidad = arbol.buscarRango(desde, hasta, (clave, posicion) -> {
            if (claves.length() > 0) {
                claves.append(' ');
            }
            claves.append(clave);
        });
        return new ResultadoRango(cantidad, claves.toString());
    }

    private static boolean insertarDuplicadoYVerificar(ArbolBMas arbol, String arbolEsperado)
            throws IOException {
        arbol.reiniciarContadores();
        boolean retorno = arbol.insertar(35, 1);
        String volcado = arbol.volcarArbol();
        String invariantes = arbol.verificarInvariantes();
        boolean correcto = !retorno && arbol.paginasLeidas() == 3
                && arbol.paginasEscritas() == 0 && arbol.cantidadPaginas() == 10
                && arbolEsperado.equals(volcado) && "OK".equals(invariantes);
        System.out.println("insertar(35, 1): resultado=" + retorno + " R="
                + arbol.paginasLeidas() + " W=" + arbol.paginasEscritas()
                + " paginas=" + arbol.cantidadPaginas());
        System.out.println(volcado);
        System.out.println("verificarInvariantes(): " + invariantes + " - "
                + (correcto ? "OK" : "FALLA"));
        return correcto;
    }

    private static boolean verificarCabecera(File archivo) throws IOException {
        try (RandomAccessFile entrada = new RandomAccessFile(archivo, "r")) {
            return entrada.length() == 600
                    && entrada.readInt() == 0x41454433
                    && entrada.readInt() == 1
                    && entrada.readInt() == 60
                    && entrada.readInt() == 4
                    && entrada.readInt() == 3
                    && entrada.readInt() == 8
                    && entrada.readInt() == 10
                    && entrada.readInt() == 3
                    && entrada.readLong() == 13;
        }
    }

    private static boolean iguales(int[] valores, int a, int b, int c) {
        return valores.length == 3 && valores[0] == a && valores[1] == b && valores[2] == c;
    }

    private static void imprimirCaso(int numero, String resultado, boolean correcto) {
        if (correcto) {
            casosOK++;
        }
        System.out.println("Caso " + numero + ": " + resultado + " - "
                + (correcto ? "OK" : "FALLA"));
    }

    private static final class ResultadoRango {
        private final int cantidad;
        private final String claves;

        private ResultadoRango(int cantidad, String claves) {
            this.cantidad = cantidad;
            this.claves = claves;
        }
    }
}
