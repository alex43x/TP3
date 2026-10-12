/* Grupo: g_ts9
 * Tarea: TP3 - U4
 * Integrantes: Portillo Sales Angel Zacarias CI 7259245 TS y Llamosas Maidana Alex Giovanni CI 5631704 TS
 */
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public final class ArbolBMas implements AutoCloseable {
    private static final int MAGIC = 0x41454433;
    private static final int VERSION = 1;
    private static final int HOJA = 0;
    private static final int INTERNO = 1;

    private final RandomAccessFile archivo;
    private final int tamPagina;
    private final int ordenInterno;
    private final int capacidadHoja;
    private int raiz;
    private int cantidadPaginas;
    private int niveles;
    private long cantidadClaves;
    private long paginasLeidas;
    private long paginasEscritas;
    private boolean cerrado;

    private ArbolBMas(RandomAccessFile archivo, int tamPagina, int ordenInterno,
                      int capacidadHoja, int raiz, int cantidadPaginas,
                      int niveles, long cantidadClaves) {
        this.archivo = archivo;
        this.tamPagina = tamPagina;
        this.ordenInterno = ordenInterno;
        this.capacidadHoja = capacidadHoja;
        this.raiz = raiz;
        this.cantidadPaginas = cantidadPaginas;
        this.niveles = niveles;
        this.cantidadClaves = cantidadClaves;
    }

    public static ArbolBMas crear(String ruta, int tamPagina) throws IOException {
        int l = calcularL(tamPagina);
        int m = calcularM(tamPagina);
        if (tamPagina < 40 || l < 2 || m < 3) {
            throw new TamPaginaInvalidoException("Tamano de pagina invalido: " + tamPagina);
        }
        RandomAccessFile archivo = new RandomAccessFile(ruta, "rw");
        try {
            archivo.setLength(0);
            ArbolBMas arbol = new ArbolBMas(archivo, tamPagina, m, l, 1, 2, 1, 0);
            arbol.escribirCabecera();
            arbol.escribirNodo(new Nodo(HOJA, l, m), 1, false);
            arbol.paginasLeidas = 0;
            arbol.paginasEscritas = 0;
            return arbol;
        } catch (IOException | RuntimeException e) {
            archivo.close();
            throw e;
        }
    }

    public static ArbolBMas abrir(String ruta) throws IOException {
        RandomAccessFile archivo = new RandomAccessFile(ruta, "rw");
        try {
            if (archivo.length() < 40) {
                throw new FormatoInvalidoException("Cabecera incompleta");
            }
            archivo.seek(0);
            int magic = archivo.readInt();
            int version = archivo.readInt();
            int p = archivo.readInt();
            int m = archivo.readInt();
            int l = archivo.readInt();
            int raiz = archivo.readInt();
            int paginas = archivo.readInt();
            int niveles = archivo.readInt();
            long claves = archivo.readLong();
            if (magic != MAGIC || version != VERSION || p < 40
                    || m != calcularM(p) || l != calcularL(p)
                    || l < 2 || m < 3 || paginas < 2 || raiz < 1 || raiz >= paginas
                    || niveles < 1 || claves < 0 || archivo.length() != (long) paginas * p) {
                throw new FormatoInvalidoException("Cabecera o longitud invalida");
            }
            return new ArbolBMas(archivo, p, m, l, raiz, paginas, niveles, claves);
        } catch (EOFException e) {
            archivo.close();
            throw new FormatoInvalidoException("Cabecera incompleta", e);
        } catch (IOException | RuntimeException e) {
            archivo.close();
            throw e;
        }
    }

    public boolean insertar(long clave, long posicion) throws IOException {
        asegurarAbierto();
        Nodo[] camino = new Nodo[niveles];
        int[] paginas = new int[niveles];
        int[] indices = new int[niveles];
        int pagina = raiz;
        for (int nivel = 0; nivel < niveles; nivel++) {
            Nodo nodo = leerNodo(pagina, true);
            camino[nivel] = nodo;
            paginas[nivel] = pagina;
            if (nodo.tipo == HOJA) {
                int indice = buscarIgualOMayor(nodo.claves, nodo.n, clave);
                if (indice < nodo.n && nodo.claves[indice] == clave) {
                    return false;
                }
                insertarPar(nodo, indice, clave, posicion);
            } else {
                int indice = buscarHijo(nodo.claves, nodo.n, clave);
                indices[nivel] = indice;
                pagina = nodo.hijos[indice];
            }
        }

        cantidadClaves++;
        Nodo actual = camino[niveles - 1];
        int paginaActual = paginas[niveles - 1];
        long separador = 0;
        int paginaDerecha = -1;
        boolean haySeparador = false;

        if (actual.n <= capacidadHoja) {
            escribirNodo(actual, paginaActual, true);
            escribirCabecera();
            return true;
        }

        Nodo derecha = new Nodo(HOJA, capacidadHoja, ordenInterno);
        int izquierdaN = (capacidadHoja + 2) / 2;
        derecha.n = actual.n - izquierdaN;
        for (int i = 0; i < derecha.n; i++) {
            derecha.claves[i] = actual.claves[izquierdaN + i];
            derecha.posiciones[i] = actual.posiciones[izquierdaN + i];
        }
        actual.n = izquierdaN;
        paginaDerecha = reservarPagina();
        derecha.siguiente = actual.siguiente;
        actual.siguiente = paginaDerecha;
        separador = derecha.claves[0];
        haySeparador = true;
        escribirNodo(actual, paginaActual, true);
        escribirNodo(derecha, paginaDerecha, true);

        for (int nivel = niveles - 2; nivel >= 0 && haySeparador; nivel--) {
            Nodo padre = camino[nivel];
            insertarEnInterno(padre, indices[nivel], separador, paginaDerecha);
            if (padre.n < ordenInterno) {
                escribirNodo(padre, paginas[nivel], true);
                haySeparador = false;
            } else {
                Nodo nuevo = new Nodo(INTERNO, capacidadHoja, ordenInterno);
                int hijosIzquierda = (ordenInterno + 2) / 2;
                int indiceSube = hijosIzquierda - 1;
                long nuevaSeparacion = padre.claves[indiceSube];
                int hijosDerecha = padre.n + 1 - hijosIzquierda;
                nuevo.n = hijosDerecha - 1;
                for (int i = 0; i < hijosDerecha; i++) {
                    nuevo.hijos[i] = padre.hijos[hijosIzquierda + i];
                }
                for (int i = 0; i < nuevo.n; i++) {
                    nuevo.claves[i] = padre.claves[hijosIzquierda + i];
                }
                padre.n = indiceSube;
                paginaDerecha = reservarPagina();
                separador = nuevaSeparacion;
                escribirNodo(padre, paginas[nivel], true);
                escribirNodo(nuevo, paginaDerecha, true);
            }
        }

        if (haySeparador) {
            Nodo nuevaRaiz = new Nodo(INTERNO, capacidadHoja, ordenInterno);
            nuevaRaiz.n = 1;
            nuevaRaiz.claves[0] = separador;
            nuevaRaiz.hijos[0] = raiz;
            nuevaRaiz.hijos[1] = paginaDerecha;
            int paginaRaiz = reservarPagina();
            escribirNodo(nuevaRaiz, paginaRaiz, true);
            raiz = paginaRaiz;
            niveles++;
        }
        escribirCabecera();
        return true;
    }

    public long buscar(long clave) throws IOException {
        asegurarAbierto();
        int pagina = raiz;
        while (true) {
            Nodo nodo = leerNodo(pagina, true);
            if (nodo.tipo == HOJA) {
                int i = buscarIgualOMayor(nodo.claves, nodo.n, clave);
                return i < nodo.n && nodo.claves[i] == clave ? nodo.posiciones[i] : -1;
            }
            pagina = nodo.hijos[buscarHijo(nodo.claves, nodo.n, clave)];
        }
    }

    public int[] caminoBusqueda(long clave) throws IOException {
        asegurarAbierto();
        int[] camino = new int[niveles];
        int pagina = raiz;
        for (int nivel = 0; nivel < niveles; nivel++) {
            camino[nivel] = pagina;
            Nodo nodo = leerNodo(pagina, true);
            if (nodo.tipo == INTERNO) {
                pagina = nodo.hijos[buscarHijo(nodo.claves, nodo.n, clave)];
            }
        }
        return camino;
    }

    public int buscarRango(long desde, long hasta, ConsumidorEntrada consumidor) throws IOException {
        asegurarAbierto();
        if (desde > hasta) {
            throw new RangoInvalidoException("El limite inferior es mayor que el superior");
        }
        if (consumidor == null) {
            throw new NullPointerException("consumidor");
        }
        int pagina = raiz;
        Nodo nodo;
        while (true) {
            nodo = leerNodo(pagina, true);
            if (nodo.tipo == HOJA) {
                break;
            }
            pagina = nodo.hijos[buscarHijo(nodo.claves, nodo.n, desde)];
        }
        int entregados = 0;
        while (true) {
            int inicio = buscarIgualOMayor(nodo.claves, nodo.n, desde);
            for (int i = inicio; i < nodo.n; i++) {
                if (nodo.claves[i] > hasta) {
                    return entregados;
                }
                consumidor.aceptar(nodo.claves[i], nodo.posiciones[i]);
                entregados++;
            }
            if (nodo.siguiente == -1 || nodo.n == 0 || nodo.claves[nodo.n - 1] >= hasta) {
                return entregados;
            }
            nodo = leerNodo(nodo.siguiente, true);
        }
    }

    public int niveles() {
        return niveles;
    }

    public int raiz() {
        return raiz;
    }

    public int cantidadPaginas() {
        return cantidadPaginas;
    }

    public long cantidadClaves() {
        return cantidadClaves;
    }

    public int tamPagina() {
        return tamPagina;
    }

    public int ordenInterno() {
        return ordenInterno;
    }

    public int capacidadHoja() {
        return capacidadHoja;
    }

    public long paginasLeidas() {
        return paginasLeidas;
    }

    public long paginasEscritas() {
        return paginasEscritas;
    }

    public void reiniciarContadores() {
        paginasLeidas = 0;
        paginasEscritas = 0;
    }

    public String volcarPagina(int nro) throws IOException {
        asegurarPagina(nro);
        Nodo nodo = leerNodo(nro, false);
        StringBuilder texto = new StringBuilder();
        texto.append('#').append(nro).append(nodo.tipo == HOJA ? " H n=" : " I n=").append(nodo.n);
        if (nodo.tipo == HOJA) {
            texto.append(" sig=").append(nodo.siguiente).append(" [");
            for (int i = 0; i < nodo.n; i++) {
                if (i > 0) {
                    texto.append(' ');
                }
                texto.append(nodo.claves[i]).append(':').append(nodo.posiciones[i]);
            }
            return texto.append(']').toString();
        }
        texto.append(" [");
        for (int i = 0; i < nodo.n; i++) {
            texto.append('#').append(nodo.hijos[i]).append(' ').append(nodo.claves[i]).append(' ');
        }
        return texto.append('#').append(nodo.hijos[nodo.n]).append(']').toString();
    }

    public String volcarArbol() throws IOException {
        asegurarAbierto();
        int[] actuales = new int[cantidadPaginas];
        int[] siguientes = new int[cantidadPaginas];
        int cantidadActual = 1;
        actuales[0] = raiz;
        StringBuilder texto = new StringBuilder();
        for (int nivel = 1; nivel <= niveles; nivel++) {
            if (nivel > 1) {
                texto.append('\n');
            }
            texto.append(nivel).append(": ");
            int cantidadSiguiente = 0;
            for (int j = 0; j < cantidadActual; j++) {
                if (j > 0) {
                    texto.append(" | ");
                }
                int pagina = actuales[j];
                Nodo nodo = leerNodo(pagina, false);
                texto.append('#').append(pagina).append(nodo.tipo == HOJA ? '[' : '(');
                for (int i = 0; i < nodo.n; i++) {
                    if (i > 0) {
                        texto.append(',');
                    }
                    texto.append(nodo.claves[i]);
                }
                texto.append(nodo.tipo == HOJA ? ']' : ')');
                if (nodo.tipo == INTERNO) {
                    for (int i = 0; i <= nodo.n; i++) {
                        siguientes[cantidadSiguiente++] = nodo.hijos[i];
                    }
                }
            }
            int[] temporal = actuales;
            actuales = siguientes;
            siguientes = temporal;
            cantidadActual = cantidadSiguiente;
        }
        return texto.toString();
    }

    public EstadisticasBMas estadisticas() throws IOException {
        asegurarAbierto();
        long[] totales = new long[4];
        acumularEstadisticas(raiz, totales);
        double ocupacionHojas = totales[0] == 0 ? 0.0
                : (double) totales[2] / (totales[0] * capacidadHoja);
        double ocupacionInternos = totales[1] == 0 ? 0.0
                : (double) totales[3] / (totales[1] * ordenInterno);
        return new EstadisticasBMas((int) totales[0], (int) totales[1],
                ocupacionHojas, ocupacionInternos);
    }

    public String verificarInvariantes() throws IOException {
        asegurarAbierto();
        if (archivo.length() != (long) cantidadPaginas * tamPagina) {
            return "Largo del archivo invalido";
        }
        if (raiz < 1 || raiz >= cantidadPaginas || niveles < 1 || cantidadClaves < 0) {
            return "Cabecera invalida";
        }
        EstadoVerificacion estado = new EstadoVerificacion(cantidadPaginas);
        String error;
        try {
            error = verificarNodo(raiz, 1, false, 0, false, 0, estado);
        } catch (FormatoInvalidoException e) {
            return e.getMessage();
        } catch (EOFException e) {
            return "Pagina incompleta";
        }
        if (error != null) {
            return error;
        }
        if (estado.ultimaHoja != -1 && estado.siguienteUltima != -1) {
            return "La cadena de hojas no termina en -1";
        }
        if (estado.claves != cantidadClaves) {
            return "Cantidad de claves inconsistente";
        }
        for (int i = 1; i < cantidadPaginas; i++) {
            if (!estado.alcanzada[i]) {
                return "Pagina no alcanzable: #" + i;
            }
        }
        return "OK";
    }

    @Override
    public void close() throws IOException {
        if (!cerrado) {
            escribirCabecera();
            cerrado = true;
            archivo.close();
        }
    }

    private static int calcularL(int p) {
        return p < 12 ? -1 : (p - 12) / 16;
    }

    private static int calcularM(int p) {
        return p < 4 ? -1 : (p - 4) / 12;
    }

    private static int buscarIgualOMayor(long[] claves, int n, long clave) {
        int izquierda = 0;
        int derecha = n;
        while (izquierda < derecha) {
            int medio = (izquierda + derecha) >>> 1;
            if (claves[medio] < clave) {
                izquierda = medio + 1;
            } else {
                derecha = medio;
            }
        }
        return izquierda;
    }

    private static int buscarHijo(long[] claves, int n, long clave) {
        int izquierda = 0;
        int derecha = n;
        while (izquierda < derecha) {
            int medio = (izquierda + derecha) >>> 1;
            if (claves[medio] <= clave) {
                izquierda = medio + 1;
            } else {
                derecha = medio;
            }
        }
        return izquierda;
    }

    private static void insertarPar(Nodo nodo, int indice, long clave, long posicion) {
        for (int i = nodo.n; i > indice; i--) {
            nodo.claves[i] = nodo.claves[i - 1];
            nodo.posiciones[i] = nodo.posiciones[i - 1];
        }
        nodo.claves[indice] = clave;
        nodo.posiciones[indice] = posicion;
        nodo.n++;
    }

    private static void insertarEnInterno(Nodo nodo, int indice, long clave, int hijoDerecho) {
        for (int i = nodo.n; i > indice; i--) {
            nodo.claves[i] = nodo.claves[i - 1];
        }
        for (int i = nodo.n + 1; i > indice + 1; i--) {
            nodo.hijos[i] = nodo.hijos[i - 1];
        }
        nodo.claves[indice] = clave;
        nodo.hijos[indice + 1] = hijoDerecho;
        nodo.n++;
    }

    private int reservarPagina() {
        return cantidadPaginas++;
    }

    private Nodo leerNodo(int pagina, boolean contar) throws IOException {
        asegurarPagina(pagina);
        byte[] datos = new byte[tamPagina];
        archivo.seek((long) pagina * tamPagina);
        archivo.readFully(datos);
        if (contar) {
            paginasLeidas++;
        }
        ByteBuffer buffer = ByteBuffer.wrap(datos);
        int tipo = buffer.getInt();
        int n = buffer.getInt();
        int siguiente = buffer.getInt();
        int maximo = tipo == HOJA ? capacidadHoja : ordenInterno - 1;
        if ((tipo != HOJA && tipo != INTERNO) || n < 0 || n > maximo) {
            throw new FormatoInvalidoException("Nodo invalido en pagina #" + pagina);
        }
        Nodo nodo = new Nodo(tipo, capacidadHoja, ordenInterno);
        nodo.n = n;
        nodo.siguiente = siguiente;
        if (tipo == HOJA) {
            for (int i = 0; i < n; i++) {
                nodo.claves[i] = buffer.getLong(12 + 16 * i);
                nodo.posiciones[i] = buffer.getLong(20 + 16 * i);
            }
        } else {
            if (siguiente != -1) {
                throw new FormatoInvalidoException("Interno con siguiente distinto de -1 en #" + pagina);
            }
            for (int i = 0; i <= n; i++) {
                nodo.hijos[i] = buffer.getInt(12 + 4 * i);
            }
            for (int i = 0; i < n; i++) {
                nodo.claves[i] = buffer.getLong(12 + 4 * ordenInterno + 8 * i);
            }
        }
        return nodo;
    }

    private void escribirNodo(Nodo nodo, int pagina, boolean contar) throws IOException {
        byte[] datos = new byte[tamPagina];
        ByteBuffer buffer = ByteBuffer.wrap(datos);
        buffer.putInt(nodo.tipo);
        buffer.putInt(nodo.n);
        buffer.putInt(nodo.tipo == HOJA ? nodo.siguiente : -1);
        if (nodo.tipo == HOJA) {
            for (int i = 0; i < nodo.n; i++) {
                buffer.putLong(12 + 16 * i, nodo.claves[i]);
                buffer.putLong(20 + 16 * i, nodo.posiciones[i]);
            }
        } else {
            for (int i = 0; i <= nodo.n; i++) {
                buffer.putInt(12 + 4 * i, nodo.hijos[i]);
            }
            for (int i = 0; i < nodo.n; i++) {
                buffer.putLong(12 + 4 * ordenInterno + 8 * i, nodo.claves[i]);
            }
        }
        archivo.seek((long) pagina * tamPagina);
        archivo.write(datos);
        if (contar) {
            paginasEscritas++;
        }
    }

    private void escribirCabecera() throws IOException {
        byte[] datos = new byte[tamPagina];
        ByteBuffer buffer = ByteBuffer.wrap(datos);
        buffer.putInt(MAGIC);
        buffer.putInt(VERSION);
        buffer.putInt(tamPagina);
        buffer.putInt(ordenInterno);
        buffer.putInt(capacidadHoja);
        buffer.putInt(raiz);
        buffer.putInt(cantidadPaginas);
        buffer.putInt(niveles);
        buffer.putLong(cantidadClaves);
        archivo.seek(0);
        archivo.write(datos);
    }

    private void acumularEstadisticas(int pagina, long[] totales) throws IOException {
        Nodo nodo = leerNodo(pagina, false);
        if (nodo.tipo == HOJA) {
            totales[0]++;
            totales[2] += nodo.n;
            return;
        }
        totales[1]++;
        totales[3] += nodo.n + 1;
        for (int i = 0; i <= nodo.n; i++) {
            acumularEstadisticas(nodo.hijos[i], totales);
        }
    }

    private String verificarNodo(int pagina, int nivel, boolean tieneMinimo, long minimo,
                                 boolean tieneMaximo, long maximo,
                                 EstadoVerificacion estado) throws IOException {
        if (pagina < 1 || pagina >= cantidadPaginas) {
            return "Referencia de pagina invalida: #" + pagina;
        }
        if (estado.alcanzada[pagina]) {
            return "Pagina alcanzada mas de una vez: #" + pagina;
        }
        estado.alcanzada[pagina] = true;
        Nodo nodo = leerNodo(pagina, false);
        for (int i = 1; i < nodo.n; i++) {
            if (nodo.claves[i - 1] >= nodo.claves[i]) {
                return "Claves no estrictamente crecientes en #" + pagina;
            }
        }
        boolean esRaiz = pagina == raiz;
        if (nodo.tipo == HOJA) {
            int minimoHoja = (capacidadHoja + 1) / 2;
            if ((!esRaiz && (nodo.n < minimoHoja || nodo.n > capacidadHoja))
                    || (esRaiz && nodo.n > capacidadHoja)) {
                return "Ocupacion invalida en hoja #" + pagina;
            }
            if (nivel != niveles) {
                return "Hoja en nivel incorrecto: #" + pagina;
            }
            for (int i = 0; i < nodo.n; i++) {
                long clave = nodo.claves[i];
                if ((tieneMinimo && clave < minimo) || (tieneMaximo && clave >= maximo)) {
                    return "Clave fuera de las cotas en #" + pagina;
                }
            }
            if (estado.ultimaHoja != -1 && estado.siguienteUltima != pagina) {
                return "Cadena de hojas invalida entre #" + estado.ultimaHoja + " y #" + pagina;
            }
            estado.ultimaHoja = pagina;
            estado.siguienteUltima = nodo.siguiente;
            estado.claves += nodo.n;
            return null;
        }
        int hijos = nodo.n + 1;
        int minimoHijos = (ordenInterno + 1) / 2;
        if ((esRaiz && (hijos < 2 || hijos > ordenInterno))
                || (!esRaiz && (hijos < minimoHijos || hijos > ordenInterno))) {
            return "Ocupacion invalida en interno #" + pagina;
        }
        if (nivel >= niveles) {
            return "Interno en el ultimo nivel: #" + pagina;
        }
        for (int i = 0; i < nodo.n; i++) {
            long clave = nodo.claves[i];
            if ((tieneMinimo && clave < minimo) || (tieneMaximo && clave >= maximo)) {
                return "Separador fuera de las cotas en #" + pagina;
            }
            if (primeraClave(nodo.hijos[i + 1]) != clave) {
                return "Separador incorrecto en #" + pagina;
            }
        }
        for (int i = 0; i < hijos; i++) {
            boolean hijoTieneMinimo = i == 0 ? tieneMinimo : true;
            long hijoMinimo = i == 0 ? minimo : nodo.claves[i - 1];
            boolean hijoTieneMaximo = i == nodo.n ? tieneMaximo : true;
            long hijoMaximo = i == nodo.n ? maximo : nodo.claves[i];
            String error = verificarNodo(nodo.hijos[i], nivel + 1,
                    hijoTieneMinimo, hijoMinimo, hijoTieneMaximo, hijoMaximo, estado);
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    private long primeraClave(int pagina) throws IOException {
        for (int nivel = 1; nivel <= niveles; nivel++) {
            if (pagina < 1 || pagina >= cantidadPaginas) {
                throw new FormatoInvalidoException("Referencia de pagina invalida: #" + pagina);
            }
            Nodo nodo = leerNodo(pagina, false);
            if (nodo.tipo == HOJA) {
                if (nodo.n == 0) {
                    throw new FormatoInvalidoException("Hoja no raiz vacia: #" + pagina);
                }
                return nodo.claves[0];
            }
            pagina = nodo.hijos[0];
        }
        throw new FormatoInvalidoException("Subarbol sin hoja");
    }

    private void asegurarPagina(int pagina) throws IOException {
        asegurarAbierto();
        if (pagina < 1 || pagina >= cantidadPaginas) {
            throw new FormatoInvalidoException("Numero de pagina invalido: " + pagina);
        }
    }

    private void asegurarAbierto() throws IOException {
        if (cerrado) {
            throw new IOException("El arbol esta cerrado");
        }
    }

    private static final class Nodo {
        final int tipo;
        int n;
        int siguiente = -1;
        final long[] claves;
        final long[] posiciones;
        final int[] hijos;

        Nodo(int tipo, int l, int m) {
            this.tipo = tipo;
            claves = new long[tipo == HOJA ? l + 1 : m];
            posiciones = tipo == HOJA ? new long[l + 1] : null;
            hijos = tipo == INTERNO ? new int[m + 1] : null;
        }
    }

    private static final class EstadoVerificacion {
        final boolean[] alcanzada;
        int ultimaHoja = -1;
        int siguienteUltima = -1;
        long claves;

        EstadoVerificacion(int paginas) {
            alcanzada = new boolean[paginas];
        }
    }
}
