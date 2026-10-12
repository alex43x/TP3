/*
 * Grupo g_ts9 - Tarea TP3-U4
 * Portillo Sales Angel Zacarias, CI 7259245, TS
 * Llamosas Maidana Alex Giovanni, CI 5631704, TS
 */

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public final class ArchivoDatos implements AutoCloseable {
    private final RandomAccessFile archivo;
    private final boolean escritura;
    private long registrosLeidos;

    private ArchivoDatos(RandomAccessFile archivo, boolean escritura) {
        this.archivo = archivo;
        this.escritura = escritura;
    }

    public static ArchivoDatos crear(String ruta) throws IOException {
        RandomAccessFile archivo = new RandomAccessFile(ruta, "rw");
        archivo.setLength(0);
        return new ArchivoDatos(archivo, true);
    }

    public static ArchivoDatos abrir(String ruta) throws IOException {
        return new ArchivoDatos(new RandomAccessFile(ruta, "r"), false);
    }

    public long agregar(long clave, String linea) throws IOException {
        if (!escritura) {
            throw new IOException("El archivo fue abierto solo para lectura");
        }
        byte[] bytes = linea.getBytes(StandardCharsets.UTF_8);
        long posicion = archivo.length();
        archivo.seek(posicion);
        archivo.writeLong(clave);
        archivo.writeInt(bytes.length);
        archivo.write(bytes);
        return posicion;
    }

    public String leer(long posicion) throws IOException {
        if (posicion < 0 || posicion > archivo.length() - 12) {
            throw new IOException("Posicion de registro invalida: " + posicion);
        }
        archivo.seek(posicion);
        archivo.readLong();
        int largo = archivo.readInt();
        validarLargo(largo, posicion + 12);
        byte[] bytes = new byte[largo];
        archivo.readFully(bytes);
        registrosLeidos++;
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public long busquedaSecuencial(long clave) throws IOException {
        archivo.seek(0);
        while (archivo.getFilePointer() < archivo.length()) {
            long posicion = archivo.getFilePointer();
            if (archivo.length() - posicion < 12) {
                throw new EOFException("Registro incompleto en " + posicion);
            }
            long claveActual = archivo.readLong();
            int largo = archivo.readInt();
            validarLargo(largo, archivo.getFilePointer());
            registrosLeidos++;
            if (claveActual == clave) {
                return posicion;
            }
            archivo.seek(archivo.getFilePointer() + largo);
        }
        return -1;
    }

    private void validarLargo(int largo, long inicioDatos) throws IOException {
        if (largo < 0 || (long) largo > archivo.length() - inicioDatos) {
            throw new IOException("Largo de registro invalido: " + largo);
        }
    }

    public long registrosLeidos() {
        return registrosLeidos;
    }

    public void reiniciarContadores() {
        registrosLeidos = 0;
    }

    public long tamano() throws IOException {
        return archivo.length();
    }

    public void close() throws IOException {
        archivo.close();
    }
}
