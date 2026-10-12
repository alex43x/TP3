# TP3 - Arbol B+ persistente en disco

Grupo `g_ts9` - Algoritmos y Estructura de Datos III, TP3-U4.

Integrantes:

- Portillo Sales Angel Zacarias, CI 7259245, TS.
- Llamosas Maidana Alex Giovanni, CI 5631704, TS.

El proyecto implementa un indice `long -> long` que vive en `indice.dat`. La pagina 0 contiene la cabecera persistente y cada nodo ocupa una pagina completa. Los registros variables se guardan por separado en `datos.dat`; las hojas solo almacenan la clave y la posicion inicial del registro. Los nodos persistentes se relacionan mediante numeros de pagina, no mediante referencias Java.

## Ejecucion

Compilacion y prueba del Ejercicio 1:

```text
javac *.java
java TestArbolBMas
```

Construccion y consultas independientes:

```text
java BMasApp construir title.basics.tsv.gz
java BMasApp buscar tt0111161
java BMasApp rango 100000 100999 10
java BMasApp secuencial 111161
java BMasApp info
java BMasApp verificar
java BMasApp pagina 345
```

Tablas experimentales:

```text
java ExperimentoBMas a
java ExperimentoBMas b
java ExperimentoBMas c
java ExperimentoBMas d
```

Los tiempos dependen de la maquina y del cache del sistema operativo. Las paginas y cantidades estructurales son las medidas deterministas relevantes.

## Resultados

### Tabla A - Estructura sintetica, N = 1.000.000

El orden aleatorio se obtuvo con Fisher-Yates y semilla 2026. Las invariantes se verificaron en los doce arboles.

| P | M | L | niveles crec. | hojas crec. | ocup. hojas crec. | ocup. internos crec. | niveles aleat. | hojas aleat. | ocup. hojas aleat. | ocup. internos aleat. |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 128 | 10 | 7 | 8 | 250000 | 0.571429 | 0.600018 | 7 | 196962 | 0.725303 | 0.716949 |
| 256 | 21 | 15 | 6 | 125000 | 0.533333 | 0.523882 | 6 | 94394 | 0.706260 | 0.698771 |
| 512 | 42 | 31 | 5 | 62500 | 0.516129 | 0.524002 | 5 | 46245 | 0.697547 | 0.676476 |
| 1024 | 85 | 63 | 4 | 31250 | 0.507937 | 0.506563 | 4 | 22847 | 0.694753 | 0.628225 |
| 2048 | 170 | 127 | 4 | 15625 | 0.503937 | 0.505371 | 3 | 11438 | 0.688408 | 0.580895 |
| 4096 | 341 | 255 | 3 | 7812 | 0.501993 | 0.500893 | 3 | 5627 | 0.696920 | 0.571848 |

### Tabla B - Construccion con IMDb

Se usaron las primeras 300.000 filas y `P = 4096`.

| orden | registros | datos bytes | indice bytes | P | M | L | niveles | paginas | hojas | internos | ocup. hojas | ocup. internos | pag. escritas | tiempo (us) |
|:--|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| archivo | 300000 | 27099525 | 9658368 | 4096 | 341 | 255 | 3 | 2358 | 2343 | 14 | 0.502121 | 0.493506 | 304708 | 26007374 |
| aleatorio | 300000 | 27099525 | 7434240 | 4096 | 341 | 255 | 3 | 1815 | 1805 | 9 | 0.651784 | 0.590746 | 303622 | 13021896 |

### Tabla C - Busquedas

Las consultas se generaron con semilla 2026: 1000 claves existentes, 1000 inexistentes y las primeras 50 de cada grupo para la busqueda secuencial.

| metodo | consultas | paginas indice/consulta | registros/consulta | us/consulta |
|:--|---:|---:|---:|---:|
| B+ existente | 1000 | 3.000 | 1.000 | 117.081 |
| B+ inexistente | 1000 | 3.000 | 0.000 | 46.061 |
| secuencial existente | 50 | 0.000 | 152493.280 | 1788434.256 |
| secuencial inexistente | 50 | 0.000 | 300000.000 | 3671525.548 |

### Tabla D - Rangos

Se realizaron 100 consultas por ancho, continuando la secuencia aleatoria usada en la Tabla C.

| ancho | registros/consulta | paginas/consulta | hojas/consulta | us/consulta |
|---:|---:|---:|---:|---:|
| 10 | 9.560 | 3.030 | 1.030 | 145.578 |
| 100 | 95.360 | 3.700 | 1.700 | 110.734 |
| 1000 | 956.330 | 10.440 | 8.440 | 226.092 |
| 10000 | 9351.750 | 75.990 | 73.990 | 671.485 |

## Analisis

### Capacidad de pagina

Una hoja usa 12 bytes de encabezado y 16 por par, por lo que `12 + 16L <= P` y `L = floor((P - 12) / 16)`. Un interno reserva `M` hijos de 4 bytes y `M - 1` claves de 8 bytes: `12 + 4M + 8(M - 1) = 4 + 12M <= P`, de donde `M = floor((P - 4) / 12)`.

Con `P = 60`, `L = 3` y la hoja usa los 60 bytes; `M = 4` y el interno usa 52, dejando 8 bytes. Con `P = 4096`, `L = 255` y la hoja usa 4092, dejando 4 bytes; `M = 341` y el interno usa exactamente 4096. `M != L` porque una entrada interna agrega aproximadamente un hijo `int` y una clave `long` por separador, mientras una entrada de hoja contiene dos `long`: clave y posicion.

### Altura

Sea `h` la cantidad de niveles. Con paginas llenas caben como maximo `L * M^(h-1)` claves; para `N > L`, el minimo posible es `h_min = 1 + ceil(log_M(N/L))`. Para la cota opuesta, sean `lmin = ceil(L/2)` y `mmin = ceil(M/2)`. Un arbol de `h >= 2` necesita al menos `2 * lmin * mmin^(h-2)` claves, porque la raiz tiene al menos dos hijos. El maximo `h` es el mayor entero que satisface esa desigualdad.

La ocupacion real ubica la altura entre ambas cotas. En la Tabla A, `P = 4096` produce 3 niveles tanto en orden creciente como aleatorio para un millon de claves; `P = 2048` baja de 4 a 3 con orden aleatorio por su mejor ocupacion. IMDb tambien necesita 3 niveles. Un ABB perfectamente balanceado necesita `ceil(log2(N + 1))`: 20 niveles para un millon y 19 para 300.000, frente a 3 accesos del B+ de 4096 bytes.

### E/S por operacion

`buscar` lee una pagina por nivel, exista o no la clave. `insertar` conserva el camino ya leido: realiza `h` lecturas y, si hay `s` divisiones, `2s + 1` escrituras. Cada split escribe el nodo original y el nuevo; la escritura restante corresponde al padre que absorbe el separador o a la raiz nueva. Un duplicado escribe cero paginas.

Por el fan-out, las operaciones puntuales realizan `Theta(log_M N)` E/S. Dentro de cada pagina se usa busqueda binaria; hay `Theta(log M)` comparaciones por interno y `Theta(log L)` en la hoja, para `Theta(log N)` comparaciones totales.

### Ocupacion

En insercion creciente siempre recibe la hoja derecha. Al dividir, la hoja nueva queda con `floor((L + 1)/2)` entradas y vuelve a dividirse tras `ceil((L + 1)/2)` inserciones; casi todas las hojas quedan cerca del 50 %. Para `N > L`, la cantidad exacta es `1 + ceil((N - L) / ceil((L + 1)/2))`. Esto predice toda la columna creciente: por ejemplo, con `L = 255` da 7812 hojas para un millon y 2343 para IMDb.

La permutacion aleatoria reparte inserciones entre hojas parcialmente llenas. En la Tabla A sus ocupaciones de hojas rondan 69-72 %, usa menos hojas y reduce la altura de 8 a 7 niveles para `P = 128` y de 4 a 3 para `P = 2048`. En IMDb las claves llegan crecientes porque el TSV esta ordenado por `tconst`: el indice en orden ocupa 2343 hojas y 9.658.368 bytes, mientras el aleatorio ocupa 1805 hojas y 7.434.240 bytes.

### Secuencial contra indice

La Tabla C muestra que el B+ usa siempre 3 paginas de indice. Si encuentra la clave lee ademas un registro; si no, ninguno. La busqueda secuencial existente leyo en promedio 152493.280 registros y la inexistente los 300000. Por tanto, la secuencial cuesta `Theta(N)` registros, mientras el indice cuesta `Theta(log_M N)` paginas y una lectura de datos solo cuando corresponde.

### Rangos

Un rango lee `h - 1` internos una sola vez y luego las hojas enlazadas necesarias. Si entrega `s` registros y la ocupacion media es `rho`, el costo esperado es aproximadamente `h - 1 + Theta(1 + s/(rho L))` paginas. La Tabla D sigue ese comportamiento: con 9351.750 resultados se leen 73.990 hojas, no miles de caminos completos. Hacer `s` busquedas independientes costaria `Theta(s log_M N)` paginas; usar la cadena cuesta `Theta(log_M N + s/(rho L))`.

## Declaracion de Honor

```text
Grupo: g_ts9
Tarea: TP3 - U4
Integrantes:
- Portillo Sales, Angel Zacarias - CI N°: 7259245 - Seccion: TS
- Llamosas Maidana, Alex Giovanni - CI N°: 5631704 - Seccion: TS

Nosotros Angel Zacarias Portillo Sales, Alex Giovanni Llamosas Maidana

- No hemos discutido el codigo fuente de nuestra tarea con ningun otro grupo, solo con el Profesor o el AER.
- No hemos usado codigo obtenido de otro estudiante o de cualquier otra fuente no autorizada, modificada o no modificada.
- Cualquier codigo o documentacion utilizada en nuestro programa obtenido de fuentes, tales como libros o notas de curso, ha sido claramente indicada en nuestra tarea.
```

## Uso de IA

Se utilizo OpenCode con el modelo `openai/gpt-5.6-sol` como apoyo para verificar que la entrega se ajustara a los requerimientos de la guia y para elaborar las pruebas solicitadas. No se uso para generar la implementacion.

Detalle por parte:

- Formato de pagina y calculo de `M` y `L`: realizado manualmente y verificado con IA contra el formato de la guia.
- `buscar`, split de hoja, split de interno y raiz nueva: realizados manualmente; la IA se uso para revisar las reglas de descenso, division y conteo de E/S.
- `buscarRango`: realizado manualmente y verificado con IA, en particular la regla de parada entre hojas.
- Persistencia (`crear`, `abrir` y `close`): realizada manualmente y revisada con IA respecto de la cabecera, la reapertura y el formato fisico.
- `verificarInvariantes`: realizado manualmente; la IA ayudo a contrastar las comprobaciones con I1-I5.
- `TestArbolBMas`: elaborado con ayuda de IA para organizar la cobertura de los 12 casos, la traza de `P = 60`, los contadores y la reapertura.
- `ArchivoDatos` y `BMasApp`: realizados manualmente y verificados con IA contra los comandos y el formato pedidos.
- `ExperimentoBMas`: realizado manualmente; la IA se uso para revisar las columnas, cantidades de consultas y semillas exigidas.
- Analisis del README: realizado manualmente y verificado con IA para comprobar que respondiera los seis puntos de la guia.

Prompts relevantes, copiados textualmente:

```text
Revisa esta lista de requisitos del TP y señala cuales deberian poder comprobarse mediante pruebas o invariantes del arbol B+ persistente.
```

```text
Compara los resultados de estas pruebas con las condiciones de la guia: formato de paginas, persistencia tras reapertura, busqueda por clave y recorrido de rangos. Indica posibles incumplimientos.
```

Las observaciones de la IA se usaron para completar la cobertura de las pruebas y revisar sus resultados esperados. Se contrastaron con `TP3-U4-ArbolBMas.pdf` y con ejecuciones reales; no se incorporaron propuestas que cambiaran la implementacion ni se aceptaron observaciones sin comprobarlas.
