# Cuestionario para la defensa del TP3

Guia basada en la rubrica de defensa y en el codigo entregado. Para practicar, leer primero solo la pregunta, responder en voz alta sin mirar el codigo y luego comparar con la respuesta esperada.

## Como responder en la defensa

Una respuesta completa deberia seguir este orden:

1. Decir la idea o invariante.
2. Aplicarla al ejemplo concreto.
3. Indicar su costo en paginas leidas o escritas.
4. Relacionarla con el metodo del codigo que la implementa.

No alcanza con decir que un metodo "busca" o "divide". Hay que explicar por que se eligio ese diseno y que propiedad preserva.

## 1. Representacion fisica, M y L - 5 puntos

### 1. ¿Que representa una pagina de `indice.dat`?

Es la unidad fija de almacenamiento y de E/S del arbol. La pagina 0 es la cabecera y las paginas 1 en adelante son nodos hoja o internos. Cada pagina ocupa exactamente `P` bytes.

### 2. ¿En que byte comienza la pagina numero `k`?

En el byte `k * P`. El codigo hace `archivo.seek((long) pagina * tamPagina)` tanto para leer como para escribir (`ArbolBMas.java`, metodos `leerNodo` y `escribirNodo`). El cast a `long` evita overflow en la multiplicacion.

### 3. ¿Que contiene la cabecera y cuanto ocupa?

Ocupa toda la pagina 0. Sus primeros 40 bytes contienen:

| Offset | Tipo | Campo |
|---:|:---:|:---|
| 0 | `int` | `MAGIC = 0x41454433` |
| 4 | `int` | version 1 |
| 8 | `int` | `P` |
| 12 | `int` | `M` |
| 16 | `int` | `L` |
| 20 | `int` | pagina raiz |
| 24 | `int` | cantidad de paginas, incluida la cabecera |
| 28 | `int` | niveles |
| 32 | `long` | cantidad de claves |

Los bytes restantes se escriben en cero porque `escribirCabecera` crea un arreglo nuevo de `P` bytes.

### 4. ¿Como se representa una hoja?

Tiene 12 bytes comunes: `tipo = 0`, `n` y `siguiente`. Desde el offset 12 guarda pares de 16 bytes: una clave `long` en `12 + 16i` y una posicion `long` en `20 + 16i`. `siguiente` enlaza la proxima hoja o vale `-1`.

### 5. ¿Como se representa un nodo interno?

Tiene `tipo = 1`, `n` claves y `siguiente = -1`. Reserva primero espacio para los `M` hijos `int`, desde `12 + 4i`. Las claves `long` empiezan siempre en `12 + 4M + 8i`, aunque el nodo no este lleno. Con `n` claves usa `n + 1` hijos.

### 6. Deduzca `L` sin leer el codigo.

Una hoja necesita `12 + 16L <= P`. Por lo tanto:

```text
L = floor((P - 12) / 16)
```

Se toma el mayor entero que cabe.

### 7. Deduzca `M` sin leer el codigo.

Un interno reserva `M` hijos y `M - 1` claves:

```text
12 + 4M + 8(M - 1) <= P
4 + 12M <= P
M = floor((P - 4) / 12)
```

### 8. Calcule `M`, `L` y los bytes libres para `P = 60`.

`L = floor(48/16) = 3`. La hoja usa `12 + 3*16 = 60`, sin bytes libres.

`M = floor(56/12) = 4`. El interno usa `4 + 12*4 = 52`, por lo que quedan 8 bytes libres.

### 9. Calcule `M`, `L` y los bytes libres para `P = 4096`.

`L = floor(4084/16) = 255`. La hoja usa 4092 bytes y deja 4.

`M = floor(4092/12) = 341`. El interno usa exactamente 4096 bytes y no deja espacio.

### 10. ¿Por que `M` no es igual a `L`?

Porque las entradas tienen formatos distintos. Una hoja usa 16 bytes por par, dos `long`. En un interno, cada aumento del fan-out cuesta aproximadamente 12 bytes: un hijo `int` y una clave `long`, con una clave menos que hijos.

### 11. ¿Por que los bytes se interpretan en big-endian?

Es el formato exigido. `RandomAccessFile` y `ByteBuffer` usan big-endian por defecto, por eso no se configura otro orden.

### 12. ¿Por que los nodos apuntan con numeros de pagina y no con referencias Java?

Porque las referencias Java no sobreviven al cierre del proceso ni identifican una posicion estable en disco. El numero permite recuperar un nodo con `seek(pagina * P)` en una ejecucion posterior.

### 13. ¿Para que los arreglos de `Nodo` tienen una posicion adicional?

Una hoja reserva `L + 1` claves y posiciones, y un interno puede alojar temporalmente `M` claves y `M + 1` hijos. Eso permite insertar primero el elemento que causa overflow y luego dividir el nodo en memoria.

### 14. ¿Que tamanos de pagina rechaza la implementacion?

Rechaza `P < 40`, `L < 2` o `M < 3`. Con las formulas actuales, por ejemplo `P = 40` todavia da `L = 1`, por lo que tambien es invalido. La validacion esta en `crear` y se vuelve a comprobar al abrir.

## 2. Busquedas, inserciones y trazas - 7 puntos

### 15. En un interno, ¿como se elige el hijo para buscar `x`?

Se elige el hijo cuyo indice es la cantidad de separadores menores o iguales que `x`. Es una busqueda binaria tipo limite superior: avanza a la derecha cuando `clave[medio] <= x`. Esto implementa los intervalos `x < k0`, `k0 <= x < k1`, etc.

### 16. ¿Por que la igualdad baja por el hijo derecho del separador?

Porque en un B+ el separador es la menor clave del subarbol derecho. Por I2, esa clave pertenece al intervalo derecho. En el split de hoja el separador se copia al padre pero el dato permanece en la hoja derecha.

### 17. ¿Cuantas paginas lee `buscar`?

Exactamente una por nivel, exista o no la clave: `niveles()` lecturas. No relee nodos ni carga el archivo completo.

### 18. Trace `buscar(35)` sobre el arbol final de la guia.

Lee `#8`, compara con 50 y baja al hijo izquierdo `#3`. En `#3`, como `30 <= 35 < 40`, toma el hijo 2, `#5`. En `#5` encuentra `35:350`. Camino: `#8 -> #3 -> #5`; costo: 3 lecturas.

### 19. ¿Que ocurre al buscar una clave inexistente como 33?

Recorre igualmente un camino completo hasta una hoja. En el arbol final llega a `#5`, no encuentra 33 y retorna `-1`. Cuesta 3 lecturas.

### 20. ¿Que hace `insertar` antes de modificar una hoja?

Baja como una busqueda, guarda en arreglos los nodos, numeros de pagina e indices de hijo del camino. En la hoja busca la primera clave mayor o igual. Si encuentra un duplicado retorna `false` sin escribir; si no, inserta ordenadamente.

### 21. ¿Cuanta memoria de paginas usa una insercion?

Guarda como maximo las paginas del camino raiz-hoja, por lo que usa `O(h)` paginas temporales. El arbol completo nunca se representa como objetos enlazados en RAM.

### 22. Explique exactamente un split de hoja.

Con `L + 1` pares, la hoja original conserva los primeros `ceil((L + 1)/2)` y el resto va a una hoja nueva. La nueva hereda el antiguo `siguiente`; luego la original apunta a la nueva. La primera clave de la hoja nueva se copia al padre y permanece en la hoja.

### 23. ¿Por que el separador de una hoja se copia y no se mueve?

Porque I1 exige que todos los datos esten en hojas. El interno solo guia la busqueda; la clave y su posicion deben seguir en una hoja.

### 24. ¿Como se reenlaza una hoja intermedia?

Primero `nueva.siguiente = original.siguiente` y despues `original.siguiente = paginaNueva`. El orden conceptual evita perder la referencia al resto de la cadena. Por ejemplo, al dividir `#1` entre `#1` y `#2`, la nueva `#5` deja la cadena `#1 -> #5 -> #2`.

### 25. Explique exactamente un split de interno.

Cuando hay `M` claves y `M + 1` hijos temporales, el original conserva los primeros `ceil((M + 1)/2)` hijos y las claves entre ellos. La clave siguiente sube al padre y desaparece de ambos nodos divididos. Los hijos y claves restantes van al nuevo interno.

### 26. ¿Por que la clave central del interno se mueve y no se copia?

En un interno esa clave solo es un separador entre dos subarboles. Despues de la division pasa a separar los dos internos desde el padre. Dejarla tambien en uno de ellos alteraria la relacion entre claves e hijos y las cotas de I2.

### 27. ¿Como se asignan paginas nuevas?

Siempre al final: el numero nuevo es `cantidadPaginas` y luego se incrementa. En una propagacion se asignan de abajo hacia arriba: nueva hoja, nuevos internos y finalmente nueva raiz.

### 28. ¿Que ocurre si el separador llega por encima de la raiz?

Se crea una nueva raiz interna con una clave y dos hijos: la raiz anterior y la pagina derecha generada por su split. La pagina nueva se vuelve `raiz` y `niveles` aumenta en uno.

### 29. Trace la insercion de 45 del ejemplo.

La busqueda lee `#3` y `#5`: 2 lecturas. La hoja `#5`, con `[30,35,40]`, recibe 45 y se divide en `#5[30,35]` y la nueva `#6[40,45]`; sube una copia de 40. El padre `#3` queda temporalmente con 4 claves y 5 hijos, se divide: `#3(30,40)`, nueva `#7(80)`, y 50 sube, desapareciendo de esos internos. Se crea la raiz nueva `#8(50)`. Se asignan `#6`, `#7`, `#8`, en ese orden. Hay 2 splits y se escriben `2*2 + 1 = 5` paginas.

### 30. ¿Cual es el contrato de E/S de una insercion?

Lee `h = niveles()` paginas. Si hay `s` divisiones, escribe `2s + 1`: dos por cada nodo dividido y una por el ultimo padre que absorbe el separador o por la nueva raiz. La cabecera puede escribirse, pero no cuenta como pagina de nodo.

### 31. ¿Por que una insercion sin split escribe una sola pagina?

Solo cambia la hoja destino. El camino interno ya leido no se modifica ni debe reescribirse.

### 32. ¿Que costo tiene insertar un duplicado?

Lee un camino completo, retorna `false`, no cambia `cantidadClaves`, no reserva paginas y escribe cero nodos.

### 33. ¿Como funciona `buscarRango`?

Hace un unico descenso desde la raiz hasta la hoja donde estaria `desde`. En esa hoja empieza en la primera clave mayor o igual a `desde`, entrega pares hasta `hasta` y continua por `siguiente` sin volver a la raiz.

### 34. ¿Cual es la regla exacta de parada del rango?

Se detiene si encuentra una clave mayor que `hasta`, si `siguiente == -1`, o si la ultima clave de la hoja actual es mayor o igual a `hasta`. Solo lee la hoja siguiente cuando la ultima clave actual es menor que `hasta`.

### 35. Trace `buscarRango(22, 62)`.

Desciende `#8 -> #3 -> #9`; entrega 25 y sigue `#5 -> #6 -> #2`, donde entrega 50 y 60 y se detiene al ver 70. Resultado: `25 30 35 40 45 50 60`. Lee 6 paginas: 3 del descenso y 3 hojas adicionales.

### 36. ¿Por que `buscarRango(20, 45)` no lee la hoja `#2`?

En `#6` entrega 40 y 45. Como su ultima clave es 45, que ya es `>= hasta`, sabe que ninguna hoja posterior puede aportar una clave dentro del rango. Termina con 5 lecturas.

### 37. Enumere las invariantes I1-I5.

I1: datos solo en hojas, claves ordenadas y unicas, hojas al mismo nivel. I2: separadores y cotas correctas entre hijos. I3: ocupacion minima y maxima, con reglas especiales para la raiz. I4: hojas enlazadas en orden y terminadas en `-1`. I5: referencias fisicas mediante numeros de pagina.

### 38. ¿Que comprueba `verificarInvariantes` en esta implementacion?

Comprueba largo del archivo, cabecera basica, referencias validas, paginas alcanzadas una sola vez, claves estrictamente crecientes, cotas de separadores, separador igual a la primera clave del hijo derecho, ocupacion, nivel de hojas, cadena de hojas, cantidad total de claves y que toda pagina de nodos sea alcanzable.

### 39. ¿Por que los diagnosticos no cambian los contadores?

`volcarPagina`, `volcarArbol`, `estadisticas` y `verificarInvariantes` llaman a `leerNodo` con `contar = false`. Asi observar o validar el arbol no contamina la operacion que se esta midiendo.

## 3. Persistencia - 3 puntos

### 40. ¿Como se crea un arbol vacio?

Se trunca el archivo, se configura raiz `#1`, 2 paginas totales, un nivel y cero claves. Se escribe la cabecera en `#0` y una hoja vacia en `#1`. La escritura inicial se excluye de los contadores dejandolos en cero.

### 41. ¿Como se reabre sin reconstruir?

`abrir` lee solo los primeros 40 bytes de la cabecera, recupera `P`, `M`, `L`, raiz, cantidad de paginas, niveles y claves, y conserva abierto el `RandomAccessFile`. Las operaciones posteriores llegan a los nodos mediante numeros de pagina.

### 42. ¿Que valida `abrir`?

Valida longitud minima, `MAGIC`, version, `P`, que `M` y `L` coincidan con las formulas, capacidades minimas, raiz y cantidad de paginas validas, niveles y claves, y que el largo sea exactamente `cantidadPaginas * P`. Los nodos se validan cuando se leen o mediante `verificarInvariantes`.

### 43. ¿Por que `FormatoInvalidoException` es chequeada?

Extiende `IOException` porque un archivo externo puede estar truncado, ser de otra version o no ser un indice. Es una condicion recuperable que el llamador debe manejar. En cambio, un rango invertido o un tamano solicitado invalido son errores de precondicion y usan excepciones no chequeadas.

### 44. ¿Que estado se espera despues de cerrar y reabrir el ejemplo completo?

Raiz 8, 3 niveles, 10 paginas, 13 claves y 600 bytes para `P = 60`. `buscar(45)` retorna 450, `buscar(15)` retorna 150 y `verificarInvariantes()` retorna `OK`.

### 45. ¿Por que no se serializa el arbol completo al cerrar?

Porque el objetivo es que el arbol viva siempre en paginas de disco. Cada insercion escribe sus paginas modificadas antes de retornar. Serializar una estructura en RAM al final incumpliria persistencia, memoria `O(h)` y el modelo de costo por pagina.

## 4. Altura, fan-out y accesos a disco - 5 puntos

### 46. ¿Que es el fan-out y por que importa?

Es la cantidad maxima `M` de hijos de un interno. Un `M` grande hace que cada nivel discrimine entre muchos subarboles, reduciendo la altura y por tanto las paginas leidas.

### 47. De una cota para el minimo numero de niveles con `N` claves.

Con paginas llenas, un arbol de `h` niveles guarda como maximo `L * M^(h-1)` claves. Para `N > L`:

```text
h_min = 1 + ceil(log_M(N/L))
```

Para `N <= L`, basta un nivel.

### 48. ¿Como se obtiene la cota de altura maxima?

Sean `lmin = ceil(L/2)` y `mmin = ceil(M/2)`. Para `h >= 2`, la raiz tiene al menos dos hijos y el minimo de claves es:

```text
2 * lmin * mmin^(h-2)
```

La altura maxima es el mayor `h` para el que ese minimo no supera `N`.

### 49. ¿Por que el B+ de `P = 4096` tiene 3 niveles con 300.000 claves?

Tiene `M = 341` y `L = 255`. Dos niveles llenos solo admiten `255*341 = 86.955` claves, insuficientes. Tres niveles admiten hasta `255*341^2 = 29.650.455`, por lo que 3 niveles alcanzan.

### 50. Compare el B+ con un ABB balanceado para 300.000 claves.

El ABB de un nodo por pagina necesita aproximadamente `ceil(log2(300001)) = 19` lecturas. El B+ medido necesita 3 gracias a que cada pagina contiene cientos de claves e hijos.

### 51. ¿Cual es la complejidad de una busqueda?

Realiza `Theta(log_M N)` accesos a disco. Dentro de cada pagina usa busqueda binaria; sumando niveles, realiza `Theta(log N)` comparaciones.

### 52. ¿Por que la insercion creciente deja hojas cerca del 50 %?

Todas las claves nuevas llegan a la hoja derecha. Cada split deja dos mitades, pero solo la ultima vuelve a llenarse; las hojas anteriores quedan cerca de su ocupacion minima.

### 53. Prediga la cantidad de hojas para insercion creciente.

Para `N > L`, una hoja nueva aparece cada `ceil((L + 1)/2)` inserciones despues de llenar la primera:

```text
hojas = 1 + ceil((N - L) / ceil((L + 1)/2))
```

Con `N = 1.000.000` y `L = 255` da 7812 hojas. Con 300.000 da 2343.

### 54. ¿Por que la insercion aleatoria ocupa mejor las hojas?

Las inserciones se distribuyen entre muchas hojas parcialmente llenas en lugar de concentrarse siempre en la ultima. En la Tabla A la ocupacion ronda 69-72 %, frente a aproximadamente 50 % en creciente.

### 55. Relacione ocupacion, tamano del indice y altura usando sus tablas.

Con IMDb creciente se obtuvieron 2343 hojas y 9.658.368 bytes; con orden aleatorio, 1805 hojas y 7.434.240 bytes. La mejor ocupacion reduce paginas. Tambien puede reducir niveles: en la Tabla A, para `P = 2048`, creciente usa 4 niveles y aleatorio 3.

### 56. ¿En que orden llegan originalmente las claves de IMDb?

En orden creciente de `tconst`. Por eso la construccion normal reproduce el caso de ocupacion cercana al 50 %. `--aleatorio` mantiene `datos.dat` en el orden original, pero baraja con Fisher-Yates los arreglos paralelos de claves y posiciones antes de insertar el indice.

### 57. ¿Por que se cuentan paginas y no solamente tiempo?

El tiempo depende del hardware, la carga y el cache del sistema operativo. Las paginas transferidas dependen del algoritmo y representan el costo de un disco frio. Los tiempos se reportan, pero la conclusion se fundamenta en E/S.

### 58. Compare la busqueda secuencial y el indice usando la Tabla C.

El B+ lee siempre 3 paginas de indice y, si encuentra la clave, un registro de datos. La secuencial leyo en promedio 152.493,280 registros para existentes y 300.000 para inexistentes. La secuencial es `Theta(N)` y el indice `Theta(log_M N)`.

### 59. Exprese el costo de una consulta por rango.

Con altura `h`, `s` resultados, ocupacion media `rho` y capacidad `L`, el costo esperado es:

```text
h - 1 + Theta(1 + s/(rho*L)) paginas
```

Se leen los internos una vez y luego las hojas necesarias. Hacer `s` busquedas independientes costaria `Theta(s log_M N)` paginas.

### 60. Interprete la fila de rango de ancho 10.000.

Entrega en promedio 9351,750 registros y lee 75,990 paginas, de las cuales 73,990 son hojas. No recorre un camino por resultado: paga 2 internos una vez y despues avanza por hojas enlazadas.

## 5. Modificaciones propuestas en vivo - 3 puntos

### 61. Si el rango debiera ser `[desde, hasta)`, ¿que cambiaria?

La entrega debe detenerse cuando `clave >= hasta`, no cuando `clave > hasta`. La regla para pasar a la siguiente hoja cambia de `ultima < hasta` a la misma condicion estricta, que ya expresa que todavia puede haber claves menores que `hasta`. Tambien deben ajustarse pruebas y documentacion.

### 62. Si un duplicado debiera actualizar la posicion, ¿que cambiaria?

Al encontrar la clave en la hoja se reemplaza su posicion y se escribe esa unica hoja. Se retorna segun el nuevo contrato, no aumenta `cantidadClaves`, no hay split y el costo es `h` lecturas y 1 escritura.

### 63. Si la posicion del registro pasara de `long` a `int`, ¿que debe revisarse?

Cambian el formato de hoja, sus offsets, la formula de `L`, lectura, escritura, `Nodo.posiciones`, interfaces y compatibilidad de version. Cada par pasaria de 16 a 12 bytes. Los indices existentes no se pueden abrir como si tuvieran el formato nuevo; habria que cambiar la version o migrarlos.

### 64. Si se agregara un cache LRU de paginas, ¿que debe preservar?

Las busquedas deben pedir paginas al cache; los contadores solo aumentan cuando hay E/S real. Debe existir modo sin cache para las mediciones. Las paginas modificadas necesitan una politica clara de escritura y `close` debe vaciar cambios pendientes sin romper persistencia.

### 65. Si se implementara eliminacion, ¿que casos aparecen?

Eliminar de hoja, actualizar separadores si cambia la primera clave de un hijo, redistribuir con hermanos, fusionar cuando no se alcanza ocupacion minima, reparar la cadena de hojas y reducir altura si la raiz interna queda con un solo hijo. Tambien cambian cantidad de claves, cabecera, E/S y pruebas.

### 66. Si se cambia `P` de 4096 a 128, ¿que ocurre?

Las formulas producen `M = 10` y `L = 7`. Disminuye el fan-out, aumentan las paginas y la altura. En la Tabla A creciente con un millon de claves llega a 8 niveles y aleatorio a 7.

### 67. Si se quisiera usar little-endian, ¿basta cambiar `ByteBuffer`?

No. Tambien deben cambiarse todas las lecturas y escrituras de cabecera realizadas directamente con `RandomAccessFile`, definir una nueva version de formato y evitar abrir archivos antiguos con la interpretacion nueva.

### 68. Si se pide validar todo el archivo durante `abrir`, ¿que cambiaria?

Despues de validar la cabecera se ejecutaria una validacion equivalente a `verificarInvariantes`. Eso detectaria nodos corruptos al abrir, pero dejaria de cumplirse el contrato actual de leer solo la cabecera y tendria costo lineal en paginas.

### 69. Si `buscarRango` hiciera una busqueda desde la raiz por cada clave, ¿por que estaria mal?

Aunque pudiera devolver los mismos resultados, violaria el algoritmo pedido. Costaria `Theta(s log_M N)` paginas en lugar de un descenso mas un recorrido secuencial de hojas.

### 70. ¿Como agregaria una suma de verificacion por pagina?

Reservaria bytes en un formato nuevo, recalcularia `M` y `L`, escribiria el checksum al guardar y lo validaria al leer. Se debe incrementar la version porque cambian offsets o capacidad y decidir si el checksum incluye sus propios bytes como cero.

## 6. Dominio del resto del proyecto - 2 puntos y requisito individual

### 71. ¿Que guarda `datos.dat`?

Registros variables consecutivos: `long clave`, `int largo` y `largo` bytes UTF-8 de la linea TSV sin salto. La posicion retornada por `agregar` es el byte donde empieza la clave y es el valor almacenado en la hoja del indice.

### 72. ¿Que diferencia hay entre `leer` y `busquedaSecuencial`?

`leer(pos)` hace un `seek` directo a una posicion conocida y lee un registro. `busquedaSecuencial(clave)` empieza en cero y salta registro por registro hasta encontrar la clave o llegar al final.

### 73. ¿Por que `BMasApp buscar` funciona en una ejecucion separada de `construir`?

Porque abre `indice.dat` y `datos.dat`; no conserva objetos del proceso constructor ni vuelve a leer el TSV. El indice persistente devuelve la posicion y `ArchivoDatos.leer` recupera la linea.

### 74. ¿Por que se reinician los contadores despues de obtener `caminoBusqueda`?

`caminoBusqueda` realiza lecturas reales. En `BMasApp buscar` se usa para mostrar el camino, pero luego se reinicia el contador para que las paginas informadas correspondan solo a `buscar` y no se duplique la medicion.

### 75. ¿Como se construye el orden aleatorio de forma reproducible?

Se usa Fisher-Yates con `Random(2026)`. En `BMasApp` se intercambian en paralelo clave y posicion para no perder su asociacion. Una semilla fija permite reproducir tablas.

### 76. ¿Que mide cada tabla experimental?

Tabla A: estructura para seis `P`, en orden creciente y aleatorio. Tabla B: construccion IMDb en ambos ordenes. Tabla C: busquedas B+ existentes/inexistentes y secuenciales. Tabla D: consultas por rango para cuatro anchos.

### 77. ¿Por que primero se obtienen todas las claves para la Tabla C?

Permite seleccionar existentes uniformemente y generar inexistentes dentro del rango descartando por busqueda binaria. Se permite porque el experimento puede usar un arreglo de claves; no cambia que el arbol operativo viva en disco.

### 78. ¿Que estructuras prohibidas evita la solucion?

No usa colecciones de `java.util`, `Arrays.*`, `Collections.*` ni `System.arraycopy`. Usa arreglos nativos, `RandomAccessFile`, `ByteBuffer`, flujos y `Random`, todos permitidos por la guia.

### 79. ¿Que partes se declararon asistidas por IA?

La implementacion se declaro manual. La IA se uso para revisar formato, reglas de busqueda y splits, rango, persistencia, invariantes, comandos, experimentos y analisis; `TestArbolBMas` fue elaborado con ayuda de IA para organizar la cobertura. Hay que poder explicar cualquiera de esas partes, especialmente las declaradas con asistencia.

### 80. ¿Que demuestran los 12 casos de `TestArbolBMas`?

Cubren arbol vacio, inserciones sin split, splits de hoja raiz e intermedia, split interno, raiz nueva, tres niveles, busquedas extremas y camino, ausencia y duplicado, rangos de una y varias hojas, regla de parada, rango invalido, persistencia, archivo invalido y consultas despues de reabrir. Ademas comparan la traza, R/W e invariantes tras cada insercion.

## Simulacro practico de 15 minutos

Un companero actua como docente y elige al azar quien responde cada bloque.

### Minutos 0-3: representacion

1. Dibuje una hoja y un interno para `P = 60` con offsets.
2. Deduzca `L` y `M` sin mirar apuntes.
3. Explique como se localiza fisicamente la pagina 7.

### Minutos 3-8: traza

1. Dibuje el arbol anterior a `insertar(45)`.
2. Marque las dos paginas leidas.
3. Ejecute el split de hoja y diga que clave se copia.
4. Ejecute el split interno y diga que clave se mueve.
5. Asigne `#6`, `#7`, `#8` y justifique 5 escrituras.

### Minutos 8-11: persistencia

1. Cierre y reabra el indice.
2. Diga que campos permiten encontrar la raiz.
3. Explique por que no se reconstruye desde `datos.dat`.

### Minutos 11-13: resultados

1. Explique por que IMDb creciente ocupa cerca del 50 %.
2. Compare 3 paginas B+ con la busqueda secuencial.
3. Explique una fila de la Tabla D.

### Minutos 13-15: cambio en vivo

Elegir una de estas consignas:

- Hacer el limite superior del rango exclusivo.
- Actualizar la posicion al insertar un duplicado.
- Cambiar la posicion de `long` a `int`.
- Agregar un cache desactivable.
- Describir eliminacion con fusion.

## Lista de control individual

Antes de la defensa, cada integrante deberia poder hacer sin ayuda:

- [ ] Deducir `L` y `M` para cualquier `P`.
- [ ] Dibujar byte a byte cabecera, hoja e interno.
- [ ] Explicar `buscarHijo` y el caso de igualdad.
- [ ] Trazar `insertar(45)` con paginas, claves, R y W.
- [ ] Explicar la diferencia entre copiar en hoja y mover en interno.
- [ ] Trazar los dos rangos de la guia y su regla de parada.
- [ ] Enumerar I1-I5 y explicar como se verifican.
- [ ] Demostrar cierre y reapertura sin TSV.
- [ ] Derivar las cotas de altura y los costos asintoticos.
- [ ] Interpretar al menos una fila de cada tabla propia.
- [ ] Explicar `ArchivoDatos`, `BMasApp`, `ExperimentoBMas` y las pruebas.
- [ ] Resolver verbalmente una modificacion no ensayada.
