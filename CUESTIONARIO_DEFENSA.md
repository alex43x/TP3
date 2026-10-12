# Guia practica para la defensa del TP3

Esta guia esta pensada para entender el proyecto, no para memorizar 80 respuestas sueltas.
Estudien en este orden:

1. Lean el mapa general y aprendan las cinco ideas esenciales.
2. Dibujen el ejemplo de `P = 60` en papel.
3. Expliquen cada bloque en voz alta sin mirar el codigo.
4. Ejecuten la demostracion practica.
5. Hagan el simulacro de 15 minutos entre los integrantes.

En la defensa conviene responder siempre con esta estructura:

1. **Idea:** que hace y por que.
2. **Ejemplo:** aplicarla al arbol de `P = 60`.
3. **Costo:** paginas leidas o escritas.
4. **Codigo:** metodo que la implementa.

## 1. Mapa general del proyecto

El programa transforma un archivo real de IMDb en dos archivos persistentes:

```text
title.basics.tsv.gz
        |
        | BMasApp construir
        v
+------------------+       +------------------+
| datos.dat        |       | indice.dat       |
| registros IMDb   |       | arbol B+         |
| de largo variable|       | clave -> posicion|
+------------------+       +------------------+
          ^                         |
          | posicion                | buscar(clave)
          +-------------------------+
```

Ejemplo: para buscar `tt0111161`, el arbol usa la clave `111161`, encuentra una posicion de
`datos.dat` y `ArchivoDatos.leer(posicion)` recupera la linea completa.

### Los archivos importantes

| Archivo | Responsabilidad |
|---|---|
| `ArbolBMas.java` | Arbol B+ persistente, busqueda, insercion, rango y validacion |
| `ArchivoDatos.java` | Escritura y lectura de registros variables en `datos.dat` |
| `BMasApp.java` | Comandos para construir y consultar los archivos |
| `ExperimentoBMas.java` | Genera las tablas A, B, C y D |
| `TestArbolBMas.java` | Ejecuta los 12 casos obligatorios |

## 2. Las cinco ideas que no pueden faltar

### Idea 1: el arbol vive en disco

`indice.dat` se divide en paginas de `P` bytes. La pagina 0 es la cabecera y las paginas siguientes
son nodos. La pagina `k` comienza en:

```text
posicion = k * P
```

El numero de pagina es una direccion persistente. Una referencia Java no sirve porque desaparece
al cerrar el programa.

**Respuesta corta para la defensa:**

> El arbol no se serializa al final ni se carga completo en RAM. Cada nodo es una pagina de
> `indice.dat`; se localiza con `k * P` y las paginas modificadas se escriben antes de terminar
> cada insercion.

### Idea 2: los datos estan solamente en las hojas

Los internos sirven para elegir un camino. Las hojas contienen los pares `clave -> posicion` y
estan enlazadas para resolver rangos.

```text
              interno: separadores
                    [50]
                   /    \
                  /      \
hojas: [10,20,30]  ->  [50,60,80]  ->  -1
```

### Idea 3: una busqueda lee una pagina por nivel

En un interno se elige el hijo segun los separadores. En la hoja se busca la clave. Si el arbol
tiene tres niveles, una busqueda lee tres paginas aunque la clave no exista.

### Idea 4: copiar en hoja, mover en interno

- Al dividir una **hoja**, la primera clave de la hoja derecha se **copia** al padre. Sigue en la
  hoja porque los datos deben permanecer en las hojas.
- Al dividir un **interno**, la clave central se **mueve** al padre. Desaparece de los dos internos
  resultantes porque ahora separa ambos subarboles desde arriba.

### Idea 5: un rango baja una vez y despues sigue las hojas

`buscarRango` hace un solo camino raiz-hoja. Luego usa el campo `siguiente` de cada hoja. No hace
una busqueda completa por cada resultado.

## 3. Como esta guardado `indice.dat`

### 3.1 Cabecera: pagina 0

La pagina 0 ocupa `P` bytes. Los primeros 40 contienen:

| Offset | Tipo | Campo |
|---:|:---:|---|
| 0 | `int` | `MAGIC = 0x41454433` |
| 4 | `int` | version 1 |
| 8 | `int` | `P`, tamano de pagina |
| 12 | `int` | `M`, maximo de hijos de un interno |
| 16 | `int` | `L`, maximo de pares de una hoja |
| 20 | `int` | numero de pagina de la raiz |
| 24 | `int` | cantidad total de paginas |
| 28 | `int` | cantidad de niveles |
| 32 | `long` | cantidad de claves |

`MAGIC` y la version permiten detectar que un archivo no tiene el formato esperado.

### 3.2 Pagina hoja

```text
offset 0       4       8       12
       +-------+-------+-------+-------------------------------+
       |tipo=0 |   n   | sig   | clave0,pos0 | clave1,pos1 ... |
       +-------+-------+-------+-------------------------------+
          int     int     int        16 bytes por par
```

- `n`: cantidad de pares usados.
- `sig`: pagina de la hoja siguiente; `-1` en la ultima.
- Cada par usa 16 bytes: `long clave` y `long posicion`.

Por eso:

```text
12 + 16L <= P
L = floor((P - 12) / 16)
```

### 3.3 Pagina interna

```text
+-------+-------+-------+----------------+--------------------+
|tipo=1 |   n   |  -1   | M hijos int   | M-1 claves long   |
+-------+-------+-------+----------------+--------------------+
  4 B      4 B     4 B       4M B             8(M-1) B
```

Con `n` claves hay `n + 1` hijos. El bloque reserva siempre lugar para `M` hijos, incluso cuando
el nodo no esta lleno.

```text
12 + 4M + 8(M - 1) <= P
4 + 12M <= P
M = floor((P - 4) / 12)
```

### 3.4 Valores que deben saber calcular

| P | L | Libre en hoja | M | Libre en interno |
|---:|---:|---:|---:|---:|
| 60 | 3 | 0 bytes | 4 | 8 bytes |
| 4096 | 255 | 4 bytes | 341 | 0 bytes |

`M` y `L` son distintos porque una hoja usa dos `long` por entrada, mientras que el crecimiento
de un interno combina aproximadamente un hijo `int` y una clave `long`.

### Practica rapida

Sin mirar la respuesta, expliquen:

1. ¿Donde comienza la pagina 7 si `P = 60`? En el byte `7 * 60 = 420`.
2. ¿Por que la hoja necesita 12 bytes antes de los pares? Por `tipo`, `n` y `siguiente`.
3. ¿Por que los arreglos internos tienen una posicion extra? Para alojar temporalmente el elemento
   que provoca overflow antes del split.

## 4. Busqueda paso a paso

Usaremos este arbol final de la guia:

```text
Nivel 1:                         #8(50)
                               /       \
Nivel 2:             #3(20,30,40)      #7(80)
                     /   |   |   \       /   \
Nivel 3: #1[10,15] #9[20,25] #5[30,35] #6[40,45] #2[50,60,70] #4[80,90]
            ->          ->          ->          ->          ->          -> -1
```

### Regla para elegir un hijo

El hijo elegido es la cantidad de separadores `<= x`. La igualdad va a la derecha porque el
separador representa la menor clave del subarbol derecho.

### Ejemplo: `buscar(35)`

1. Lee `#8`: `35 < 50`, baja a `#3`.
2. Lee `#3`: `30 <= 35 < 40`, baja al hijo 2, `#5`.
3. Lee `#5`: encuentra `35` con posicion `350`.

```text
camino = #8 -> #3 -> #5
costo = 3 paginas = niveles()
```

Si se busca `33`, recorre el mismo camino y retorna `-1`. Una ausencia tambien cuesta un camino
completo.

**Pregunta frecuente:** ¿por que se usa busqueda binaria dentro de cada pagina?

**Respuesta:** porque las claves estan ordenadas. Reduce las comparaciones dentro del nodo, aunque
la medida principal del TP siga siendo la cantidad de paginas transferidas.

## 5. Insercion y divisiones

### 5.1 Insercion sin division

1. Baja hasta una hoja y guarda el camino.
2. Si la clave existe, retorna `false` y no escribe nada.
3. Si hay lugar, inserta ordenadamente y escribe solo esa hoja.

Costo sin split:

```text
lecturas = niveles()
escrituras = 1
```

### 5.2 Split de hoja

Cuando hay `L + 1` pares en memoria:

1. La original conserva `ceil((L + 1)/2)` pares.
2. El resto pasa a una hoja nueva.
3. `nueva.siguiente` recibe el antiguo `original.siguiente`.
4. `original.siguiente` pasa a ser la pagina nueva.
5. La primera clave de la nueva se copia al padre.

Ejemplo:

```text
Antes:       #5[30,35,40] -> #2
Insertar 45: [30,35,40,45]

Despues:     #5[30,35] -> #6[40,45] -> #2
                              |
                              +-- se copia 40 al padre
```

### 5.3 Split de interno

Un interno desborda con `M` claves y `M + 1` hijos temporales:

1. El original conserva los primeros `ceil((M + 1)/2)` hijos.
2. La clave central sube al padre y desaparece de ambos internos.
3. Lo restante pasa a un interno nuevo.

### 5.4 La insercion de 45 que deben saber dibujar

Antes de insertar, la raiz es `#3(30,50,80)` y la hoja destino es `#5[30,35,40]`.

1. Se leen `#3` y `#5`: 2 lecturas.
2. `#5` se divide en `#5[30,35]` y la nueva `#6[40,45]`.
3. Se copia 40 al padre.
4. `#3` queda temporalmente con claves `30,40,50,80` y cinco hijos.
5. `#3` se divide: queda `#3(30,40)` y nace `#7(80)`.
6. La clave 50 se mueve hacia arriba.
7. Como ya no hay padre, nace la raiz `#8(50)`.

```text
Paginas nuevas, en orden: #6 hoja, #7 interno, #8 raiz
Splits: s = 2
Escrituras: 2s + 1 = 5
```

### Contrato general de E/S

```text
lecturas = h = niveles()
escrituras = 2s + 1
```

Cada split escribe el nodo original y el nuevo. La escritura restante corresponde al ultimo padre
que absorbe el separador o a la nueva raiz. La cabecera no cuenta como pagina de nodo.

### Diferencias que suelen preguntar

| Caso | Que pasa |
|---|---|
| Duplicado | `h` lecturas, 0 escrituras, no cambia la cantidad de claves |
| Sin split | `h` lecturas, 1 escritura |
| Split de hoja | El separador se copia |
| Split de interno | La clave central se mueve |
| Split de raiz | Se crea una raiz y aumenta un nivel |

## 6. Busqueda por rango

### Algoritmo

1. Baja una vez hasta la hoja donde estaria `desde`.
2. Entrega las claves del intervalo `[desde, hasta]`.
3. Continua por la cadena de hojas.
4. Se detiene al ver una clave `> hasta`, llegar a `-1` o terminar una hoja cuya ultima clave
   sea `>= hasta`.

Solo pasa a la hoja siguiente cuando la ultima clave de la actual es `< hasta`.

### Ejemplo: `buscarRango(22, 62)`

```text
Descenso: #8 -> #3 -> #9
Cadena:                   #9 -> #5 -> #6 -> #2
Entrega:                  25   30 35  40 45  50 60
```

En `#2` ve 70, que es mayor que 62, y termina. Lee 6 paginas: 3 del descenso y 3 hojas
adicionales.

### Ejemplo de parada exacta: `buscarRango(20, 45)`

Termina en `#6`, porque su ultima clave es 45. No lee `#2`: todas sus claves son posteriores y no
pueden pertenecer al rango.

**Respuesta corta para la defensa:**

> El costo es `h` paginas para el descenso, incluida la primera hoja, mas las hojas adicionales
> necesarias. Es mejor que hacer una busqueda independiente por cada resultado.

## 7. Persistencia y validacion

### Crear

Un arbol nuevo tiene:

```text
#0 = cabecera
#1 = raiz hoja vacia
paginas = 2
niveles = 1
claves = 0
```

### Abrir

`abrir` lee solo la cabecera. Recupera `P`, `M`, `L`, raiz, paginas, niveles y claves. Luego puede
localizar cualquier nodo por su numero de pagina, sin reconstruir el indice desde el TSV.

Valida, entre otras cosas:

- `MAGIC` y version.
- Capacidades `M` y `L` coherentes con `P`.
- Raiz, paginas, niveles y claves validos.
- Largo exacto `cantidadPaginas * P`.

`FormatoInvalidoException` extiende `IOException` porque el problema proviene de un archivo
externo posiblemente corrupto o incompatible. Un rango invertido y un tamano de pagina invalido
son errores de precondicion y usan excepciones no chequeadas.

### Las cinco invariantes

| Invariante | Explicacion simple |
|---|---|
| I1 | Datos solo en hojas; claves ordenadas, sin repetidos y hojas al mismo nivel |
| I2 | Para los hijos: `x < k0`, `k0 <= x < k1`, ..., y `x >=` el ultimo separador |
| I3 | Los nodos respetan ocupacion minima y maxima; la raiz tiene reglas especiales |
| I4 | Las hojas forman una cadena ordenada terminada en `-1` |
| I5 | Todos los enlaces son numeros de pagina |

`verificarInvariantes` tambien comprueba el largo del archivo, paginas validas y alcanzables una
sola vez, cantidad total de claves y correspondencia de la cadena de hojas.

Los diagnosticos no alteran los contadores porque leen con `contar = false`. Asi una inspeccion no
contamina la medicion de una operacion.

## 8. Altura y costo

### Por que el B+ tiene poca altura

El fan-out `M` es la cantidad maxima de hijos de un interno. Para `P = 4096`, `M = 341`: cada
nivel permite elegir entre cientos de subarboles.

Con paginas llenas, la capacidad de un arbol de `h` niveles es:

```text
L * M^(h - 1)
```

Para `N > L`, la altura minima es:

```text
h_min = 1 + ceil(log_M(N/L))
```

Para la altura maxima, con `lmin = ceil(L/2)` y `mmin = ceil(M/2)`, un arbol de `h >= 2` tiene
como minimo:

```text
2 * lmin * mmin^(h - 2) claves
```

### Caso IMDb: 300.000 claves

Con `P = 4096`, `L = 255` y `M = 341`:

```text
2 niveles llenos: 255 * 341   = 86.955       no alcanza
3 niveles llenos: 255 * 341^2 = 29.651.655   alcanza
```

El indice medido tiene 3 niveles. Un ABB balanceado con un nodo por pagina necesitara unas
`ceil(log2(300001)) = 19` paginas.

```text
B+  : Theta(log_M N) accesos a disco
ABB : Theta(log_2 N) accesos si cada nodo ocupa una pagina
```

## 9. Como explicar los experimentos

### Tabla A: estructura sintetica

Compara seis tamanos de pagina y dos ordenes de insercion sobre un millon de claves. Muestra que
un `P` mayor aumenta `M` y `L`, y normalmente reduce altura y cantidad de paginas.

### Tabla B: construccion IMDb

Compara insercion creciente y aleatoria. IMDb llega ordenado por `tconst`.

- En creciente, todas las claves nuevas llegan a la ultima hoja. Cada split deja atras una hoja
  cerca del 50 %.
- En aleatorio, las inserciones se distribuyen entre hojas parcialmente llenas. En la Tabla A
  sintetica la ocupacion ronda 69-72 %; en IMDb fue 65,1784 %.

Resultados del proyecto:

| Orden | Hojas | Tamano del indice |
|---|---:|---:|
| Creciente | 2343 | 9.658.368 bytes |
| Aleatorio | 1805 | 7.434.240 bytes |

Para insercion creciente y `N > L`:

```text
hojas = 1 + ceil((N - L) / ceil((L + 1)/2))
```

Con `L = 255`, predice 2343 hojas para 300.000 claves.

### Tabla C: busqueda indexada contra secuencial

- B+ existente: 3 paginas de indice y 1 registro de datos.
- B+ inexistente: 3 paginas de indice y 0 registros de datos.
- Secuencial: en promedio recorre gran parte del archivo; una inexistente lee los 300.000 registros.

La busqueda secuencial es `Theta(N)` y la indexada es `Theta(log_M N)`.

### Tabla D: rangos

Con altura `h`, `s` resultados, ocupacion media `rho` y capacidad `L`, el costo esperado es:

```text
h - 1 + Theta(1 + s/(rho * L)) paginas
```

Se pagan los internos una sola vez y luego se recorren hojas. Para ancho 10.000 se entregaron en
promedio 9351,750 registros y se leyeron 75,990 paginas, de las cuales 73,990 fueron hojas.

### Por que medir paginas y tiempo

El tiempo cambia por hardware y cache del sistema operativo. La cantidad de paginas depende del
algoritmo y representa mejor el costo de un disco frio. Por eso se informan ambos, pero el analisis
se fundamenta en paginas.

## 10. `datos.dat`, comandos y pruebas

### Formato de `datos.dat`

```text
long clave | int largo | largo bytes UTF-8 de la linea TSV
```

`agregar` devuelve el byte donde empieza la clave. Esa posicion es el valor almacenado en la hoja
del B+.

- `leer(pos)` hace acceso directo con `seek`.
- `busquedaSecuencial(clave)` recorre desde el primer registro.

### Comandos para una demostracion

```powershell
javac -encoding UTF-8 *.java
java TestArbolBMas
java BMasApp verificar
java BMasApp info
java BMasApp buscar 111161
java BMasApp rango 100000 100999 10
java BMasApp secuencial 111161
java BMasApp pagina 345
```

Para construir desde IMDb:

```powershell
java BMasApp construir title.basics.tsv.gz
```

El TSV se usa solamente para construir. Los demas comandos abren en ejecuciones separadas los
archivos persistentes que necesitan: `buscar` y `rango` usan ambos, `secuencial` usa `datos.dat`,
y `info`, `verificar` y `pagina` usan `indice.dat`. Ninguno vuelve a leer el TSV.

### Que cubren los 12 casos

1. Arbol vacio.
2. Inserciones sin split.
3. Split de raiz hoja y de hoja intermedia.
4. Split interno.
5. Nueva raiz.
6. Tres niveles.
7. Busquedas extremas y camino.
8. Clave inexistente y duplicado.
9. Rango dentro de una hoja.
10. Rangos entre hojas y regla de parada.
11. Cierre, reapertura y archivo invalido.
12. Busquedas despues de reabrir.

## 11. Cambios que puede pedir el docente

No memoricen codigo. Expliquen primero que contrato y formato cambia.

### Rango `[desde, hasta)`

Se deja de entregar cuando `clave >= hasta`. Hay que ajustar la condicion, las pruebas y la
documentacion.

### Duplicado que actualiza la posicion

Se reemplaza la posicion en la hoja y se escribe esa unica pagina. No aumenta la cantidad de
claves ni provoca split: `h` lecturas y 1 escritura.

### Posicion de `long` a `int`

Cambia el formato de hoja, los offsets, la formula de `L`, los arreglos y la version del archivo.
No se puede interpretar silenciosamente un indice viejo con el nuevo formato.

### Cache LRU de paginas

Los contadores deben aumentar solo ante E/S real. Hace falta un modo sin cache para los
experimentos y una politica de escritura que preserve la persistencia.

### Eliminacion

Aparecen redistribucion, fusion, actualizacion de separadores, reparacion de la cadena de hojas y
reduccion de altura cuando la raiz queda con un solo hijo.

### Cambiar `P` de 4096 a 128

Las formulas dan `M = 10` y `L = 7`. Al bajar el fan-out, aumentan la cantidad de paginas y la
altura.

### Validar todo durante `abrir`

Se podria llamar a una validacion equivalente a `verificarInvariantes`, pero `abrir` dejaria de leer
solo la cabecera y pasaria a tener costo lineal en paginas.

## 12. Preguntas esenciales para practicar

Respondan cada una en menos de un minuto:

1. ¿Por que el arbol es persistente y no una estructura serializada?
2. ¿Como se localiza fisicamente la pagina `k`?
3. ¿Que informacion minima necesita la cabecera para reabrir el arbol?
4. Deduzca `L` y `M` sin mirar el codigo.
5. ¿Por que la igualdad con un separador baja a la derecha?
6. ¿Por que buscar una clave inexistente cuesta la misma cantidad de paginas?
7. ¿Que se copia en un split de hoja y que se mueve en un split interno?
8. Trace `insertar(45)` e indique paginas nuevas, lecturas y escrituras.
9. ¿Por que una hoja intermedia debe reenlazarse en un orden preciso?
10. ¿Por que un duplicado escribe cero paginas?
11. ¿Por que un rango no vuelve a la raiz por cada resultado?
12. Enumere I1-I5 con sus propias palabras.
13. ¿Como se abre el indice sin leer el TSV?
14. ¿Por que 300.000 claves caben en tres niveles con `P = 4096`?
15. ¿Por que IMDb creciente ocupa cerca del 50 %?
16. ¿Que diferencia hay entre `datos.dat` e `indice.dat`?
17. ¿Que demuestra cada tabla experimental?
18. ¿Por que se cuentan paginas y no solo microsegundos?
19. ¿Que estructuras de Java estan prohibidas y que usa el proyecto en su lugar?
20. ¿Que parte declarada con ayuda de IA pueden explicar y modificar en vivo?

## 13. Simulacro de defensa de 15 minutos

Un integrante actua como docente. Elijan al azar quien responde cada bloque.

### Minutos 0-3: representacion

1. Dibuje cabecera, hoja e interno para `P = 60`.
2. Deduzca `L = 3` y `M = 4`.
3. Explique como se lee la pagina 7.

### Minutos 3-8: traza

1. Dibuje el arbol anterior a `insertar(45)`.
2. Marque `#3` y `#5` como paginas leidas.
3. Divida la hoja y copie 40.
4. Divida el interno y mueva 50.
5. Asigne `#6`, `#7`, `#8` y justifique 5 escrituras.

### Minutos 8-11: persistencia

1. Ejecute `java BMasApp verificar`.
2. Explique que campos permiten encontrar la raiz.
3. Explique por que no se reconstruye desde el TSV.

### Minutos 11-13: resultados

1. Explique la ocupacion de IMDb creciente.
2. Compare 3 paginas del B+ con una busqueda secuencial.
3. Interprete una fila de la Tabla D.

### Minutos 13-15: cambio en vivo

El docente elige uno:

- Limite superior exclusivo.
- Actualizar un duplicado.
- Cambiar `long posicion` por `int`.
- Agregar un cache desactivable.
- Agregar eliminacion.

## 14. Lista de control individual

Cada integrante debe poder hacer esto sin ayuda:

- [ ] Explicar el mapa `TSV -> datos.dat + indice.dat`.
- [ ] Deducir `L` y `M` para cualquier `P`.
- [ ] Dibujar cabecera, hoja e interno con sus offsets.
- [ ] Explicar la eleccion de hijo y el caso de igualdad.
- [ ] Trazar `buscar(35)` y `insertar(45)`.
- [ ] Explicar la diferencia entre copiar y mover una clave.
- [ ] Trazar `buscarRango(22, 62)` y su regla de parada.
- [ ] Enumerar I1-I5 y explicar como se verifican.
- [ ] Cerrar y reabrir sin usar el TSV.
- [ ] Explicar altura, fan-out y costo de E/S.
- [ ] Interpretar una fila de cada tabla.
- [ ] Explicar `ArchivoDatos`, `BMasApp`, `ExperimentoBMas` y las pruebas.
- [ ] Resolver verbalmente una modificacion no ensayada.

## 15. Frases que conviene evitar

| Respuesta debil | Respuesta mejor |
|---|---|
| "Busca recursivamente" | "Lee una pagina por nivel y elige el hijo con limite superior" |
| "Divide por la mitad" | "La hoja izquierda conserva `ceil((L+1)/2)` y copia el separador" |
| "Se guarda cuando cierro" | "Cada insercion escribe las paginas modificadas antes de retornar" |
| "El rango busca todas" | "Baja una vez y continua por la cadena de hojas" |
| "Es mas rapido" | "Reduce de `Theta(N)` registros a `Theta(log_M N)` paginas" |

La meta no es repetir estas frases de memoria. Hay que poder dibujarlas, justificarlas y relacionarlas
con una ejecucion real del programa.
