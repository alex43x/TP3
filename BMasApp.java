/*
 * Grupo g_ts9 - Tarea TP3-U4
 * Portillo Sales Angel Zacarias, CI 7259245, TS
 * Llamosas Maidana Alex Giovanni, CI 5631704, TS
 */

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.zip.GZIPInputStream;

public final class BMasApp {
    private static final String DATOS = "datos.dat";
    private static final String INDICE = "indice.dat";

    private BMasApp() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            uso();
            return;
        }
        try {
            switch (args[0]) {
                case "construir" -> construir(args);
                case "buscar" -> buscar(args);
                case "rango" -> rango(args);
                case "secuencial" -> secuencial(args);
                case "info" -> info(args);
                case "verificar" -> verificar(args);
                case "pagina" -> pagina(args);
                default -> uso();
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void construir(String[] args) throws IOException {
        if (args.length < 2 || args.length > 5) {
            uso();
            return;
        }
        int filas = 300_000;
        int tamPagina = 4096;
        boolean aleatorio = false;
        int numero = 0;
        for (int i = 2; i < args.length; i++) {
            if ("--aleatorio".equals(args[i])) {
                aleatorio = true;
            } else if (numero++ == 0) {
                filas = enteroPositivo(args[i], "filas");
            } else if (numero == 2) {
                tamPagina = enteroPositivo(args[i], "tamPagina");
            } else {
                throw new IllegalArgumentException("Argumento inesperado: " + args[i]);
            }
        }

        long[] claves = aleatorio ? new long[filas] : null;
        long[] posiciones = aleatorio ? new long[filas] : null;
        long inicio = System.nanoTime();
        int cantidad = 0;
        try (BufferedReader lector = abrirTsv(args[1]);
             ArchivoDatos datos = ArchivoDatos.crear(DATOS);
             ArbolBMas indice = ArbolBMas.crear(INDICE, tamPagina)) {
            lector.readLine();
            String linea;
            while (cantidad < filas && (linea = lector.readLine()) != null) {
                long clave = claveDeLinea(linea);
                long posicion = datos.agregar(clave, linea);
                if (aleatorio) {
                    claves[cantidad] = clave;
                    posiciones[cantidad] = posicion;
                } else if (!indice.insertar(clave, posicion)) {
                    throw new IOException("Clave duplicada en el TSV: " + clave);
                }
                cantidad++;
            }
            if (aleatorio) {
                barajar(claves, posiciones, cantidad);
                for (int i = 0; i < cantidad; i++) {
                    if (!indice.insertar(claves[i], posiciones[i])) {
                        throw new IOException("Clave duplicada en el TSV: " + claves[i]);
                    }
                }
            }
            double segundos = (System.nanoTime() - inicio) / 1_000_000_000.0;
            System.out.printf("registros: %d%n", cantidad);
            System.out.printf("datos.dat: %d bytes%n", datos.tamano());
            System.out.printf("indice.dat: %d bytes%n", (long) indice.cantidadPaginas() * indice.tamPagina());
            System.out.printf("paginas escritas: %d%n", indice.paginasEscritas());
            System.out.printf("tiempo: %.3f s%n", segundos);
        }
    }

    private static BufferedReader abrirTsv(String ruta) throws IOException {
        InputStream entrada = new BufferedInputStream(new FileInputStream(ruta));
        if (ruta.endsWith(".gz")) {
            entrada = new GZIPInputStream(entrada);
        }
        return new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8));
    }

    private static void barajar(long[] claves, long[] posiciones, int n) {
        Random rng = new Random(2026);
        for (int i = n - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            long auxiliar = claves[i];
            claves[i] = claves[j];
            claves[j] = auxiliar;
            auxiliar = posiciones[i];
            posiciones[i] = posiciones[j];
            posiciones[j] = auxiliar;
        }
    }

    private static void buscar(String[] args) throws IOException {
        exigirCantidad(args, 2);
        long clave = clave(args[1]);
        try (ArbolBMas indice = ArbolBMas.abrir(INDICE);
             ArchivoDatos datos = ArchivoDatos.abrir(DATOS)) {
            int[] camino = indice.caminoBusqueda(clave);
            indice.reiniciarContadores();
            long posicion = indice.buscar(clave);
            System.out.print("paginas leidas: " + indice.paginasLeidas() + " (");
            imprimirCamino(camino);
            System.out.println(")");
            if (posicion < 0) {
                System.out.println("no encontrado");
            } else {
                System.out.println("pos=" + posicion + " " + datos.leer(posicion));
            }
        }
    }

    private static void rango(String[] args) throws IOException {
        if (args.length < 3 || args.length > 4) {
            uso();
            return;
        }
        long desde = clave(args[1]);
        long hasta = clave(args[2]);
        int mostrar = args.length == 4 ? enteroNoNegativo(args[3], "mostrar") : 10;
        try (ArbolBMas indice = ArbolBMas.abrir(INDICE);
             ArchivoDatos datos = ArchivoDatos.abrir(DATOS)) {
            int[] mostrados = {0};
            int cantidad = indice.buscarRango(desde, hasta, (clave, posicion) -> {
                if (mostrados[0] < mostrar) {
                    try {
                        System.out.println("pos=" + posicion + " " + datos.leer(posicion));
                    } catch (IOException e) {
                        throw new FalloLectura(e);
                    }
                    mostrados[0]++;
                }
            });
            System.out.println("registros: " + cantidad);
            System.out.println("paginas leidas: " + indice.paginasLeidas());
        } catch (FalloLectura e) {
            throw e.causa;
        }
    }

    private static void secuencial(String[] args) throws IOException {
        exigirCantidad(args, 2);
        long clave = clave(args[1]);
        try (ArchivoDatos datos = ArchivoDatos.abrir(DATOS)) {
            long posicion = datos.busquedaSecuencial(clave);
            long leidos = datos.registrosLeidos();
            if (posicion < 0) {
                System.out.println("no encontrado");
            } else {
                System.out.println("pos=" + posicion + " " + datos.leer(posicion));
            }
            System.out.println("registros leidos: " + leidos);
        }
    }

    private static void info(String[] args) throws IOException {
        exigirCantidad(args, 1);
        try (ArbolBMas indice = ArbolBMas.abrir(INDICE)) {
            EstadisticasBMas e = indice.estadisticas();
            System.out.println("P=" + indice.tamPagina());
            System.out.println("M=" + indice.ordenInterno());
            System.out.println("L=" + indice.capacidadHoja());
            System.out.println("raiz=" + indice.raiz());
            System.out.println("niveles=" + indice.niveles());
            System.out.println("paginas=" + indice.cantidadPaginas());
            System.out.println("claves=" + indice.cantidadClaves());
            System.out.println("hojas=" + e.hojas);
            System.out.println("internos=" + e.internos);
            System.out.printf("ocupacion hojas=%.6f%n", e.ocupacionHojas);
            System.out.printf("ocupacion internos=%.6f%n", e.ocupacionInternos);
        }
    }

    private static void verificar(String[] args) throws IOException {
        exigirCantidad(args, 1);
        try (ArbolBMas indice = ArbolBMas.abrir(INDICE)) {
            System.out.println(indice.verificarInvariantes());
        }
    }

    private static void pagina(String[] args) throws IOException {
        exigirCantidad(args, 2);
        int numero = enteroNoNegativo(args[1], "pagina");
        try (ArbolBMas indice = ArbolBMas.abrir(INDICE)) {
            System.out.println(indice.volcarPagina(numero));
        }
    }

    private static long claveDeLinea(String linea) {
        int tabulador = linea.indexOf('\t');
        if (tabulador < 0) {
            throw new IllegalArgumentException("Fila TSV sin tabulador");
        }
        return clave(linea.substring(0, tabulador));
    }

    private static long clave(String texto) {
        String numero = texto.startsWith("tt") ? texto.substring(2) : texto;
        if (numero.isEmpty()) {
            throw new IllegalArgumentException("Clave invalida: " + texto);
        }
        for (int i = 0; i < numero.length(); i++) {
            if (numero.charAt(i) < '0' || numero.charAt(i) > '9') {
                throw new IllegalArgumentException("Clave invalida: " + texto);
            }
        }
        return Long.parseLong(numero);
    }

    private static int enteroPositivo(String texto, String nombre) {
        int valor = Integer.parseInt(texto);
        if (valor <= 0) {
            throw new IllegalArgumentException(nombre + " debe ser positivo");
        }
        return valor;
    }

    private static int enteroNoNegativo(String texto, String nombre) {
        int valor = Integer.parseInt(texto);
        if (valor < 0) {
            throw new IllegalArgumentException(nombre + " no puede ser negativo");
        }
        return valor;
    }

    private static void exigirCantidad(String[] args, int cantidad) {
        if (args.length != cantidad) {
            throw new IllegalArgumentException("Cantidad de argumentos invalida");
        }
    }

    private static void imprimirCamino(int[] camino) {
        for (int i = 0; i < camino.length; i++) {
            if (i > 0) {
                System.out.print(" -> ");
            }
            System.out.print("#" + camino[i]);
        }
    }

    private static void uso() {
        System.out.println("Uso:");
        System.out.println("  java BMasApp construir archivo.tsv[.gz] [filas=300000] [tamPagina=4096] [--aleatorio]");
        System.out.println("  java BMasApp buscar clave");
        System.out.println("  java BMasApp rango desde hasta [mostrar=10]");
        System.out.println("  java BMasApp secuencial clave");
        System.out.println("  java BMasApp info");
        System.out.println("  java BMasApp verificar");
        System.out.println("  java BMasApp pagina numero");
    }

    private static final class FalloLectura extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final IOException causa;

        private FalloLectura(IOException causa) {
            this.causa = causa;
        }
    }
}
