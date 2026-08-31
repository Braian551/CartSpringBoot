# 04 — CRUD técnico

El CRUD es MVC y devuelve vistas HTML. No hay un controlador REST JSON para estas operaciones; la excepción es la respuesta JSON del rate limiter cuando devuelve 429.

## Mappings reales

| Operación | Método | Ruta | Vista o resultado |
| --- | --- | --- | --- |
| Redirección inicial | GET | /cart/ | Redirige a /cart. |
| Read — listado | GET | /cart | cart/list, con page y size opcionales. |
| Formulario de creación | GET | /cart/new | cart/create. |
| Create | POST | /cart | Guarda y redirige a /cart; si es inválido, conserva cart/create. |
| Read — detalle | GET | /cart/{id} | cart/detail; 404 si no existe. |
| Formulario de actualización | GET | /cart/{id}/edit | cart/edit. |
| Update | POST | /cart/{id} | Guarda y redirige al detalle /cart/{id}. |
| Delete | POST | /cart/{id}/delete | Elimina y redirige a /cart. |

Todas las rutas están bajo RequestMapping("/cart").

## Datos permitidos

| Campo | Creación | Actualización | Límite |
| --- | --- | --- | ---: |
| userId | CartCreateRequest | CartUpdateRequest | 50 caracteres |
| sessionId | CartCreateRequest | CartUpdateRequest | 255 caracteres |
| id | Generado por la base de datos | Se toma de la ruta; no es editable | Entero positivo |
| createdAt | Automático | Conservado | No editable desde el formulario |
| updatedAt | Automático | Actualizado por la entidad | No editable desde el formulario |

Los dos campos de texto son opcionales: se usa Size(max=...), no NotNull ni NotBlank.

## CREATE

### Desde la interfaz

1. GET /cart/new crea un CartCreateRequest vacío y muestra cart/create.
2. El formulario envía POST /cart con userId, sessionId y el token CSRF oculto.
3. Valid ejecuta Bean Validation.
4. Si hay errores, el controlador registra un WARN, vuelve a cart/create y no llama al repositorio.
5. Si es válido, el controlador copia únicamente los dos campos a una nueva entidad Cart.
6. CartRepository.save(cart) delega en JPA/Hibernate, que inserta el registro en MySQL.
7. PrePersist establece createdAt y updatedAt.
8. Se registra el éxito y se redirige a /cart.

### Flujo

~~~mermaid
flowchart TD
    A[Usuario completa formulario] --> B[POST /cart]
    B --> C[Headers y RateLimitFilter]
    C --> D[CSRF de Spring Security]
    D --> E[CartCreateRequest + Valid]
    E --> F{¿Binding válido?}
    F -- No --> G[WARN y vista cart/create]
    F -- Sí --> H[Crear entidad Cart con campos permitidos]
    H --> I[CartRepository.save]
    I --> J[Hibernate: INSERT parametrizado]
    J --> K[(MySQL: carts)]
    K --> L[Redirect /cart]
~~~

## READ

### Listado

GET /cart acepta:

~~~text
page: entero opcional, predeterminado 0
size: entero opcional, predeterminado 20
~~~

Antes de consultar, el controlador exige 0 ≤ page ≤ 10000 y 1 ≤ size ≤ 100. Después usa PageRequest.of(page, size, orden ascendente por id).

El repositorio ejecuta findAll(Pageable) y la plantilla muestra el contenido, el número de página visible, el total y los enlaces Anterior/Siguiente.

### Detalle

GET /cart/{id} valida que el ID sea positivo y llama a findById. Si el Optional está vacío, registra Cart not found cartId={} y GlobalExceptionHandler devuelve error/404 con HTTP 404.

## UPDATE

1. GET /cart/{id}/edit busca la entidad existente.
2. El controlador copia sus campos permitidos a CartUpdateRequest y muestra cart/edit.
3. El formulario envía POST /cart/{id} con los valores y el token CSRF.
4. Se vuelve a buscar el carrito por el ID de la ruta.
5. Valid valida las longitudes.
6. Si es válido, solo se ejecutan setUserId y setSessionId sobre la entidad existente.
7. save(existingCart) actualiza el registro; PreUpdate renueva updatedAt.
8. Se redirige al detalle.

~~~mermaid
flowchart TD
    A[Formulario editar] --> B[CartUpdateRequest]
    B --> C[CSRF + Bean Validation]
    C --> D[Buscar Cart existente por ID]
    D --> E[Copiar solo userId y sessionId]
    E --> F[save(existingCart)]
    F --> G[Hibernate / UPDATE parametrizado]
    G --> H[Redirección al detalle]
~~~

El ID, createdAt y updatedAt no forman parte del conjunto actualizable. Aunque un cliente intente enviarlos, el binder tiene una allowlist y el controlador no los copia.

## DELETE

POST /cart/{id}/delete busca primero el carrito y solo después llama a cartRepository.delete(existingCart). En caso de éxito registra Cart deleted cartId={} y redirige al listado.

La interfaz usa POST porque eliminar cambia el estado y no debe ocurrir al seguir un enlace GET. El formulario contiene un token CSRF y una confirmación en el navegador.

## Respuestas de error

- 400 Bad Request: ID negativo o no numérico, parámetros de paginación fuera de rango, binding inválido o campo demasiado largo.
- 404 Not Found: el ID tiene formato válido, pero no existe un carrito.
- 429 Too Many Requests: el rate limiter se agotó antes de llegar al controlador.
- 500 Internal Server Error: excepción inesperada; el servidor registra el stack trace y la interfaz muestra una página genérica.

