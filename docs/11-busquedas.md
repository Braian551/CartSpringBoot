# 11 — Búsquedas y paginación

El listado MVC (`GET /cart`) y la API administrativa (`GET /api/admin/carts`)
usan `Pageable` de Spring Data JPA. Los tamaños se limitan a la configuración
de `app.cart`, evitando consultas sin límite.

## Búsqueda por dos campos con `AND`

En la vista, completa simultáneamente `userId` y `sessionId` y selecciona
**Buscar con Y**:

```text
GET /cart?userId=user-1&sessionId=session-1&page=0&size=20
```

El repositorio ejecuta el método derivado
`findByUserIdContainingIgnoreCaseAndSessionIdContainingIgnoreCase`, por lo que
ambos criterios deben coincidir en el mismo carrito.

La API ofrece la misma operación:

```text
GET /api/admin/carts?userId=user-1&sessionId=session-1&page=1&size=20
```

## Búsqueda por tres campos con `OR`

La búsqueda global consulta los tres campos `id`, `userId` y `sessionId`.
Escribe un valor y selecciona **Buscar con OR**:

```text
GET /cart?orSearch=42&page=0&size=20
GET /api/admin/carts?search=42&page=1&size=20
```

El valor se envía como parámetro a
`findByUserIdContainingIgnoreCaseOrSessionIdContainingIgnoreCaseOrId`; no se
concatena en SQL. Si el valor no es numérico, el criterio de `id` queda vacío
y se conservan los criterios de usuario y sesión.

No se permite combinar una búsqueda global OR con filtros AND en la misma
petición. Las entradas superan un límite de longitud o contienen caracteres
de control se responden con 400.

## Vista paginada

La plantilla `templates/cart/list.html` conserva el filtro activo en los
enlaces **Anterior** y **Siguiente**, muestra la página visible y el total de
páginas, y permite volver al listado completo mediante **Limpiar**.

## Validación en la entidad

`Cart` declara cinco restricciones Bean Validation: tamaño y ausencia de
caracteres de control para cada identificador, más una validación de fechas
que impide valores futuros. Los DTOs repiten la validación de entrada para
que los formularios fallen antes de persistir.

