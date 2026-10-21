# Defensa TP3: guia corta por rubrica

Regla para responder: diga **que es**, **como funciona en este TP** y **cuantas paginas cuesta**. No lea el codigo: use el ejemplo `P = 60` cuando le pidan una traza.

## 1. Paginas, posicion, M y L (5 pts)

- `indice.dat` se divide en paginas de `P` bytes. La pagina `k` empieza en el byte `k * P`.
- Pagina 0: cabecera. Guarda `MAGIC`, version, `P`, `M`, `L`, raiz, cantidad de paginas, niveles y claves.
- Hoja: `tipo`, `n`, `siguiente` (12 bytes) y pares `long clave, long posicion` (16 bytes por par). Solo las hojas tienen datos.
- Interno: cabecera de 12 bytes, `M` hijos `int` y `M - 1` claves `long`.
- Formulas: `L = floor((P - 12) / 16)` y `M = floor((P - 4) / 12)`.
- Para `P = 60`: `L = 3`, `M = 4`. Para `P = 4096`: `L = 255`, `M = 341`.
- `M != L` porque una entrada de hoja usa dos `long`, mientras un interno agrega hijos `int` y separadores `long`.

**Frase clave:** cada nodo es una pagina persistente; no es un objeto Java enlazado ni se serializa al cerrar.

## 2. Traza de busqueda e insercion (7 pts)

### Busqueda

- En cada interno se baja por el hijo cuyo indice es la cantidad de separadores `<= clave`; la igualdad baja a la derecha.
- Se lee exactamente una pagina por nivel: `buscar` cuesta `h = niveles()` lecturas, exista o no la clave.
- Ejemplo: `buscar(35)` sigue `#8 -> #3 -> #5`, lee 3 paginas y devuelve la posicion 350.

### Insercion y splits

- Baja guardando el camino. Si la clave existe, retorna `false`: `h` lecturas, 0 escrituras.
- Sin split: inserta ordenado en la hoja y escribe solo esa hoja.
- Split de hoja: con `L + 1` pares, la izquierda conserva `ceil((L + 1) / 2)`; la primera clave de la nueva hoja **se copia** al padre. Primero `nueva.siguiente = original.siguiente`; luego `original.siguiente = nueva`.
- Split de interno: con `M` claves y `M + 1` hijos, la clave central **sube y desaparece** de los dos internos. Si se divide la raiz, se crea una raiz nueva y aumenta la altura.
- Las paginas nuevas se asignan al final, de abajo hacia arriba: hoja nueva, interno nuevo, raiz nueva.
- Contrato: insercion = `h` lecturas y `2s + 1` escrituras, donde `s` es la cantidad de splits.

**Traza obligatoria:** al insertar 45 con `P = 60`, se divide `#5`, nace `#6[40,45]` y se copia 40. Luego se divide `#3`, nace `#7(80)`, sube 50 y nace la raiz `#8(50)`. Paginas nuevas: `#6`, `#7`, `#8`; 2 lecturas y 5 escrituras.

## 3. Persistencia (3 pts)

- `crear` produce pagina 0 (cabecera) y pagina 1 (raiz hoja vacia): 2 paginas, 1 nivel, 0 claves.
- `abrir` lee solo la cabecera; con raiz y numeros de pagina puede localizar nodos sin reconstruir desde el TSV.
- Cada insercion escribe las paginas modificadas antes de retornar. `close` actualiza la cabecera y cierra.
- `abrir` valida `MAGIC`, version, que `M` y `L` coincidan con `P`, valores de cabecera y largo exacto `cantidadPaginas * P`.
- `FormatoInvalidoException` es chequeada porque un archivo externo puede estar corrupto o tener otro formato.

**Demostracion:** ejecutar `verificar`, cerrar, volver a abrir y buscar una clave sin usar `title.basics.tsv.gz`.

## 4. Altura, fan-out y accesos a disco (5 pts)

- El fan-out es `M`: un interno puede elegir entre muchos hijos. Con `P = 4096`, `M = 341`; por eso el arbol es bajo.
- Capacidad maxima con `h` niveles: `L * M^(h - 1)`.
- Para 300.000 claves y `P = 4096`, tres niveles alcanzan; una busqueda B+ lee 3 paginas de indice.
- Un ABB balanceado con un nodo por pagina necesita aproximadamente `ceil(log2(300001)) = 19` paginas.
- Costo puntual: `Theta(log_M N)` E/S. La busqueda binaria dentro de cada pagina da `Theta(log N)` comparaciones.
- Los rangos bajan una vez y recorren hojas enlazadas: aproximadamente `h - 1 + Theta(1 + s/(rho * L))` paginas para `s` resultados y ocupacion media `rho`.

**Relacion con tablas:** la Tabla C compara 3 paginas del B+ contra cientos de miles de registros secuenciales; la Tabla D muestra que los rangos agregan hojas, no caminos desde la raiz por resultado.

## 5. Cambio propuesto en vivo (3 pts)

Primero diga que contrato/formato cambia, luego el algoritmo, pruebas y documentacion.

- Rango `[desde, hasta)`: detenerse con `clave >= hasta`; cambiar pruebas y documentacion.
- Duplicado que actualiza: reemplazar la posicion en la hoja; `h` lecturas, 1 escritura, sin cambiar claves ni hacer split.
- `posicion` como `int`: cambia formato de hoja, offsets, formula de `L`, arreglos y version; un indice viejo no puede leerse como nuevo.
- Cache LRU: los contadores aumentan solo con E/S real; se necesita modo sin cache para experimentar y politica de escritura persistente.
- Eliminacion: requiere redistribucion o fusion, actualizar separadores, reparar la cadena de hojas y reducir altura si corresponde.

## 6. Dominio individual (2 pts)

- Cada integrante debe explicar cualquier clase: `ArbolBMas`, `ArchivoDatos`, `BMasApp`, `ExperimentoBMas` y `TestArbolBMas`.
- Conozca las invariantes: datos en hojas y ordenados; separadores correctos; ocupacion valida; cadena de hojas ordenada; referencias por numero de pagina.
- Sepa ejecutar: `java TestArbolBMas`, `java BMasApp verificar`, `java BMasApp info`, `java BMasApp buscar 111161` y `java BMasApp rango 100000 100999 10`.
- Pueden pedir claves, tamanos de pagina o reaperturas distintos: expliquen la idea, no memoricen solo la traza.
- Lo declarado como uso de IA tambien debe poder explicarse y modificarse.

## Checklist final

- [ ] Calculo `M` y `L` para `P = 60` sin mirar codigo.
- [ ] Dibujo `buscar(35)` y `insertar(45)` con paginas, lecturas y escrituras.
- [ ] Explico copiar en hoja versus mover en interno.
- [ ] Demuestro cerrar, abrir y buscar sin TSV.
- [ ] Comparo 3 paginas B+ con 19 de un ABB para 300.000 claves.
- [ ] Propongo cambios sin romper formato, invariantes ni pruebas.
