/*
 * Grupo g_ts9 - Tarea TP3-U4
 * Portillo Sales Angel Zacarias, CI 7259245, TS
 * Llamosas Maidana Alex Giovanni, CI 5631704, TS
 */

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

public final class ExperimentoBMas {
    private static final int[] TAMANOS = {128, 256, 512, 1024, 2048, 4096};
    private static final int[] ANCHOS = {10, 100, 1000, 10_000};
    private static final String DATOS = "datos.dat";
    private static final String INDICE = "indice.dat";
    private static final String INDICE_ORDEN = "experimento-orden.dat";
    private static final String INDICE_ALEATORIO = "experimento-aleatorio.dat";

    private ExperimentoBMas() {
    }

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                uso();
                return;
            }
            switch (args[0]) {
                case "a", "sintetico" -> tablaA(args.length == 2 ? enteroPositivo(args[1]) : 1_000_000);
                case "b", "imdb" -> tablaB(args.length == 2 ? enteroPositivo(args[1]) : 4096);
                case "c", "busquedas" -> tablaC(INDICE_ORDEN);
                case "d", "rangos" -> tablaD(INDICE_ORDEN);
                case "todo" -> todo(args.length == 2 ? enteroPositivo(args[1]) : 1_000_000);
                default -> uso();
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void todo(int n) throws IOException {
        tablaA(n);
        tablaB(4096);
        tablaC(INDICE_ORDEN);
        tablaD(INDICE_ORDEN);
    }

    private static void tablaA(int n) throws IOException {
        System.out.println("Tabla A - estructura sintetica, N=" + n);
        System.out.println("P\tM\tL\tcrecNiveles\tcrecHojas\tcrecOcupHojas\tcrecOcupInternos\taleaNiveles\taleaHojas\taleaOcupHojas\taleaOcupInternos");
        long[] aleatorias = new long[n];
        for (int i = 0; i < n; i++) {
            aleatorias[i] = i + 1L;
        }
        barajar(aleatorias, n);
        for (int p : TAMANOS) {
            String creciente = "tabla-a-creciente-" + p + ".dat";
            String aleatorio = "tabla-a-aleatorio-" + p + ".dat";
            ResultadoA crec = construirSintetico(creciente, p, n, null);
            ResultadoA alea = construirSintetico(aleatorio, p, n, aleatorias);
            System.out.printf("%d\t%d\t%d\t%d\t%d\t%.6f\t%.6f\t%d\t%d\t%.6f\t%.6f%n",
                    p, crec.m, crec.l, crec.niveles, crec.hojas,
                    crec.ocupacionHojas, crec.ocupacionInternos,
                    alea.niveles, alea.hojas, alea.ocupacionHojas, alea.ocupacionInternos);
            borrar(creciente);
            borrar(aleatorio);
        }
    }

    private static ResultadoA construirSintetico(String ruta, int p, int n, long[] claves)
            throws IOException {
        try (ArbolBMas arbol = ArbolBMas.crear(ruta, p)) {
            for (int i = 0; i < n; i++) {
                long clave = claves == null ? i + 1L : claves[i];
                arbol.insertar(clave, clave);
            }
            String resultado = arbol.verificarInvariantes();
            if (!"OK".equals(resultado)) {
                throw new IOException(ruta + ": " + resultado);
            }
            EstadisticasBMas e = arbol.estadisticas();
            return new ResultadoA(arbol.ordenInterno(), arbol.capacidadHoja(), arbol.niveles(),
                    e.hojas, e.ocupacionHojas, e.ocupacionInternos);
        }
    }

    private static void tablaB(int p) throws IOException {
        DatosClaves registros = leerClavesDatos(DATOS);
        System.out.println("Tabla B - construccion con IMDb");
        System.out.println("orden\tregistros\tdatosBytes\tindiceBytes\tP\tM\tL\tniveles\tpaginas\thojas\tinternos\tocupHojas\tocupInternos\tpagEscritas\ttiempoUs");
        construirIndiceMedido(INDICE_ORDEN, p, registros, false, "archivo");
        construirIndiceMedido(INDICE_ALEATORIO, p, registros, true, "aleatorio");
    }

    private static void construirIndiceMedido(String ruta, int p, DatosClaves datos,
                                               boolean aleatorio, String nombre) throws IOException {
        long[] orden = new long[datos.cantidad];
        for (int i = 0; i < datos.cantidad; i++) {
            orden[i] = i;
        }
        if (aleatorio) {
            barajar(orden, datos.cantidad);
        }
        long inicio = System.nanoTime();
        try (ArbolBMas arbol = ArbolBMas.crear(ruta, p)) {
            for (int i = 0; i < datos.cantidad; i++) {
                int posicionArreglo = (int) orden[i];
                arbol.insertar(datos.claves[posicionArreglo], datos.posiciones[posicionArreglo]);
            }
            long micros = (System.nanoTime() - inicio) / 1_000;
            String resultado = arbol.verificarInvariantes();
            if (!"OK".equals(resultado)) {
                throw new IOException(ruta + ": " + resultado);
            }
            EstadisticasBMas e = arbol.estadisticas();
            System.out.printf("%s\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%.6f\t%.6f\t%d\t%d%n",
                    nombre, datos.cantidad, datos.tamano,
                    (long) arbol.cantidadPaginas() * arbol.tamPagina(), arbol.tamPagina(),
                    arbol.ordenInterno(), arbol.capacidadHoja(), arbol.niveles(),
                    arbol.cantidadPaginas(), e.hojas, e.internos,
                    e.ocupacionHojas, e.ocupacionInternos,
                    arbol.paginasEscritas(), micros);
        }
    }

    private static void tablaC(String rutaIndice) throws IOException {
        try (ArbolBMas arbol = ArbolBMas.abrir(rutaIndice);
             ArchivoDatos datos = ArchivoDatos.abrir(DATOS)) {
            int n = cantidadComoInt(arbol.cantidadClaves());
            if (n == 0) {
                throw new IOException("El indice esta vacio");
            }
            long[] claves = obtenerClaves(arbol, n);
            long[] existentes = new long[1000];
            long[] inexistentes = new long[1000];
            Random rng = new Random(2026);
            for (int i = 0; i < existentes.length; i++) {
                existentes[i] = claves[rng.nextInt(n)];
            }
            long minimo = claves[0];
            long maximo = claves[n - 1];
            for (int i = 0; i < inexistentes.length; i++) {
                long candidata;
                do {
                    candidata = aleatoriaEnRango(rng, minimo, maximo);
                } while (contiene(claves, candidata));
                inexistentes[i] = candidata;
            }

            System.out.println("Tabla C - busquedas");
            System.out.println("metodo\tconsultas\tpaginasIndicePorConsulta\tregistrosPorConsulta\tmicrosPorConsulta");
            medirBMas(arbol, datos, existentes, "B+ existente");
            medirBMas(arbol, datos, inexistentes, "B+ inexistente");
            medirSecuencial(datos, existentes, 50, "secuencial existente");
            medirSecuencial(datos, inexistentes, 50, "secuencial inexistente");
        }
    }

    private static long[] obtenerClaves(ArbolBMas arbol, int n) throws IOException {
        long[] claves = new long[n];
        int[] posicion = {0};
        arbol.buscarRango(Long.MIN_VALUE, Long.MAX_VALUE,
                (clave, pos) -> claves[posicion[0]++] = clave);
        if (posicion[0] != n) {
            throw new IllegalStateException("Cantidad de claves inconsistente");
        }
        return claves;
    }

    private static void medirBMas(ArbolBMas arbol, ArchivoDatos datos, long[] consultas,
                                  String nombre) throws IOException {
        arbol.reiniciarContadores();
        datos.reiniciarContadores();
        long inicio = System.nanoTime();
        for (long clave : consultas) {
            long posicion = arbol.buscar(clave);
            if (posicion >= 0) {
                datos.leer(posicion);
            }
        }
        long nanos = System.nanoTime() - inicio;
        imprimirMedicion(nombre, consultas.length, arbol.paginasLeidas(),
                datos.registrosLeidos(), nanos);
    }

    private static void medirSecuencial(ArchivoDatos datos, long[] consultas, int cantidad,
                                        String nombre) throws IOException {
        datos.reiniciarContadores();
        long inicio = System.nanoTime();
        for (int i = 0; i < cantidad; i++) {
            datos.busquedaSecuencial(consultas[i]);
        }
        long nanos = System.nanoTime() - inicio;
        imprimirMedicion(nombre, cantidad, 0, datos.registrosLeidos(), nanos);
    }

    private static void imprimirMedicion(String nombre, int consultas, long paginas,
                                         long registros, long nanos) {
        System.out.printf("%s\t%d\t%.3f\t%.3f\t%.3f%n", nombre, consultas,
                (double) paginas / consultas, (double) registros / consultas,
                (double) nanos / consultas / 1_000.0);
    }

    private static void tablaD(String rutaIndice) throws IOException {
        try (ArbolBMas arbol = ArbolBMas.abrir(rutaIndice)) {
            int n = cantidadComoInt(arbol.cantidadClaves());
            if (n == 0) {
                throw new IOException("El indice esta vacio");
            }
            long[] claves = obtenerClaves(arbol, n);
            Random rng = new Random(2026);
            avanzarRngDeTablaC(rng, claves);
            System.out.println("Tabla D - rangos");
            System.out.println("w\tregistrosPorConsulta\tpaginasPorConsulta\thojasPorConsulta\tmicrosPorConsulta");
            for (int ancho : ANCHOS) {
                long entregados = 0;
                arbol.reiniciarContadores();
                long inicio = System.nanoTime();
                for (int i = 0; i < 100; i++) {
                    long desde = claves[rng.nextInt(n)];
                    long hasta = desde > Long.MAX_VALUE - ancho + 1
                            ? Long.MAX_VALUE : desde + ancho - 1;
                    entregados += arbol.buscarRango(desde, hasta, (clave, posicion) -> { });
                }
                long nanos = System.nanoTime() - inicio;
                double paginas = arbol.paginasLeidas() / 100.0;
                double hojas = paginas - (arbol.niveles() - 1);
                System.out.printf("%d\t%.3f\t%.3f\t%.3f\t%.3f%n", ancho,
                        entregados / 100.0, paginas, hojas, nanos / 100.0 / 1_000.0);
            }
        }
    }

    private static DatosClaves leerClavesDatos(String ruta) throws IOException {
        File archivo = new File(ruta);
        long largoArchivo = archivo.length();
        int capacidad = 1024;
        int cantidad = 0;
        long[] claves = new long[capacidad];
        long[] posiciones = new long[capacidad];
        try (RandomAccessFile entrada = new RandomAccessFile(archivo, "r")) {
            while (entrada.getFilePointer() < entrada.length()) {
                long posicion = entrada.getFilePointer();
                if (entrada.length() - posicion < 12) {
                    throw new IOException("Registro incompleto en " + posicion);
                }
                long clave = entrada.readLong();
                int largo = entrada.readInt();
                if (largo < 0 || (long) largo > entrada.length() - entrada.getFilePointer()) {
                    throw new IOException("Largo de registro invalido en " + posicion);
                }
                if (cantidad == capacidad) {
                    int nuevaCapacidad = capacidad > Integer.MAX_VALUE / 2
                            ? Integer.MAX_VALUE : capacidad * 2;
                    if (nuevaCapacidad == capacidad) {
                        throw new IOException("Demasiados registros");
                    }
                    long[] nuevasClaves = new long[nuevaCapacidad];
                    long[] nuevasPosiciones = new long[nuevaCapacidad];
                    for (int i = 0; i < cantidad; i++) {
                        nuevasClaves[i] = claves[i];
                        nuevasPosiciones[i] = posiciones[i];
                    }
                    claves = nuevasClaves;
                    posiciones = nuevasPosiciones;
                    capacidad = nuevaCapacidad;
                }
                claves[cantidad] = clave;
                posiciones[cantidad] = posicion;
                cantidad++;
                entrada.seek(entrada.getFilePointer() + largo);
            }
        }
        return new DatosClaves(claves, posiciones, cantidad, largoArchivo);
    }

    private static long aleatoriaEnRango(Random rng, long minimo, long maximo) {
        double amplitud = (double) maximo - minimo + 1.0;
        return minimo + (long) (rng.nextDouble() * amplitud);
    }

    private static void avanzarRngDeTablaC(Random rng, long[] claves) throws IOException {
        for (int i = 0; i < 1000; i++) {
            rng.nextInt(claves.length);
        }
        long minimo = claves[0];
        long maximo = claves[claves.length - 1];
        if ((double) maximo - minimo + 1.0 <= claves.length) {
            throw new IOException("No existen claves ausentes entre minimo y maximo");
        }
        for (int i = 0; i < 1000; i++) {
            long candidata;
            do {
                candidata = aleatoriaEnRango(rng, minimo, maximo);
            } while (contiene(claves, candidata));
        }
    }

    private static boolean contiene(long[] valores, long buscado) {
        int izquierda = 0;
        int derecha = valores.length - 1;
        while (izquierda <= derecha) {
            int medio = izquierda + (derecha - izquierda) / 2;
            if (valores[medio] == buscado) {
                return true;
            }
            if (valores[medio] < buscado) {
                izquierda = medio + 1;
            } else {
                derecha = medio - 1;
            }
        }
        return false;
    }

    private static void barajar(long[] valores, int n) {
        Random rng = new Random(2026);
        for (int i = n - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            long auxiliar = valores[i];
            valores[i] = valores[j];
            valores[j] = auxiliar;
        }
    }

    private static int cantidadComoInt(long cantidad) throws IOException {
        if (cantidad > Integer.MAX_VALUE) {
            throw new IOException("La cantidad de claves no cabe en un arreglo");
        }
        return (int) cantidad;
    }

    private static int enteroPositivo(String texto) {
        int valor = Integer.parseInt(texto);
        if (valor <= 0) {
            throw new IllegalArgumentException("El valor debe ser positivo");
        }
        return valor;
    }

    private static void borrar(String ruta) {
        new File(ruta).delete();
    }

    private static void uso() {
        System.out.println("Uso:");
        System.out.println("  java ExperimentoBMas a|sintetico [N=1000000]");
        System.out.println("  java ExperimentoBMas b|imdb [tamPagina=4096]");
        System.out.println("  java ExperimentoBMas c|busquedas");
        System.out.println("  java ExperimentoBMas d|rangos");
        System.out.println("  java ExperimentoBMas todo [N=1000000]");
    }

    private static final class DatosClaves {
        private final long[] claves;
        private final long[] posiciones;
        private final int cantidad;
        private final long tamano;

        private DatosClaves(long[] claves, long[] posiciones, int cantidad, long tamano) {
            this.claves = claves;
            this.posiciones = posiciones;
            this.cantidad = cantidad;
            this.tamano = tamano;
        }
    }

    private static final class ResultadoA {
        private final int m;
        private final int l;
        private final int niveles;
        private final int hojas;
        private final double ocupacionHojas;
        private final double ocupacionInternos;

        private ResultadoA(int m, int l, int niveles, int hojas,
                           double ocupacionHojas, double ocupacionInternos) {
            this.m = m;
            this.l = l;
            this.niveles = niveles;
            this.hojas = hojas;
            this.ocupacionHojas = ocupacionHojas;
            this.ocupacionInternos = ocupacionInternos;
        }
    }
}
