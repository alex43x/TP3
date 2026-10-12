# Progreso del TP3

Ultima actualizacion: 11/10/2026.

Este archivo registra el estado real del trabajo para poder continuar en futuras sesiones. Debe actualizarse despues de cada bloque implementado o verificado. El `README.md` contiene el informe academico solicitado por el enunciado; esta bitacora no lo reemplaza.

## Estado actual

- [x] Formato fisico de cabecera, hojas e internos.
- [x] Calculo de `M` y `L` desde el tamano de pagina.
- [x] `crear`, `abrir`, `close` y persistencia de la cabecera.
- [x] Lectura y escritura explicita de paginas completas.
- [x] `buscar` y `caminoBusqueda` con una lectura por nivel.
- [x] Insercion sin division y rechazo de duplicados.
- [x] Split de hoja y reenlace de la cadena de hojas.
- [x] Split de interno, propagacion y creacion de una raiz nueva.
- [x] Contrato de E/S de insercion (`niveles()` lecturas y `2s + 1` escrituras).
- [x] `buscarRango` mediante un unico descenso y la cadena de hojas.
- [x] `volcarPagina`, `volcarArbol`, `estadisticas` y `verificarInvariantes`.
- [x] `TestArbolBMas` con los 12 casos y la traza obligatoria de `P = 60`.
- [x] `ArchivoDatos`.
- [x] `BMasApp` y sus siete comandos.
- [x] `ExperimentoBMas` con generacion de las tablas A, B, C y D.
- [x] Ejecutar y guardar las cuatro tablas con los parametros definitivos.
- [x] Completar el `README.md` academico con tablas, analisis y uso de IA.
- [x] Agregar la Declaracion de Honor con el texto oficial de la primera clase.
- [ ] Preparar y repasar la defensa oral.
- [x] Preparar cuestionario de defensa alineado con la rubrica (`CUESTIONARIO_DEFENSA.md`).
- [ ] Limpiar artefactos que no deben entrar en el ZIP (`.class`, `.dat`, TSV y PDF).

## Verificaciones realizadas

En JDK 21.0.11:

```text
javac *.java
java TestArbolBMas
```

Resultado: compilacion correcta y `12/12 casos OK`.

Tambien se verifico el indice persistente actual:

```text
java BMasApp verificar
java BMasApp info
```

Resultado de `verificar`: `OK`.

Estado observado del indice:

```text
P=4096
M=341
L=255
raiz=345
niveles=3
paginas=2358
claves=300000
hojas=2343
internos=14
ocupacion hojas=0.502121
ocupacion internos=0.493506
```

## Archivos implementados

- `ArbolBMas.java`
- `ConsumidorEntrada.java`
- `EstadisticasBMas.java`
- `FormatoInvalidoException.java`
- `RangoInvalidoException.java`
- `TamPaginaInvalidoException.java`
- `TestArbolBMas.java`
- `ArchivoDatos.java`
- `BMasApp.java`
- `ExperimentoBMas.java`

## Proximo paso

Revisar el paquete de entrega, ensayar la defensa y excluir `.class`, `.dat`, TSV y PDF del ZIP.

## Resultados experimentales guardados

- Tabla A completa con `N = 1_000_000`: terminada y copiada a `README.md`.
- Tabla B con 300.000 registros IMDb: terminada y copiada a `README.md`.
- Tabla C con 1000 busquedas B+ y 50 secuenciales por grupo: terminada y copiada a `README.md`.
- Tabla D con 100 rangos por ancho: terminada y copiada a `README.md`.

Los tiempos son propios de la ejecucion del 11/10/2026 y pueden cambiar por el cache y la maquina. No deben reemplazarse a mano: si se vuelven a medir, se actualizan las tablas completas con la salida del programa.

## Decisiones y advertencias

- El arbol vive en `indice.dat`; los objetos `Nodo` solo representan paginas temporales.
- La pagina 0 es la cabecera y no se incluye en los contadores de E/S.
- Los diagnosticos no alteran los contadores.
- No se usan colecciones de `java.util`, salvo `Random`, ni `Arrays.*`, `Collections.*` o `System.arraycopy`.
- El dataset y los archivos `.dat` no deben incluirse en la entrega.
- El enunciado exige que el `README.md` termine con una declaracion detallada del uso de IA. El contenido generado o revisado en estas sesiones debe declararse como tal.
- El 11/10/2026 se recompilo con `javac *.java`, se ejecuto `java TestArbolBMas` con resultado `12/12 casos OK`, y se verifico la reapertura mediante `java BMasApp verificar`, `java BMasApp info` y `java BMasApp buscar 111161`.
