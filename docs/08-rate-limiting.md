# 08 — Rate limiting

El rate limiter controla la cantidad de solicitudes que una dirección cliente puede hacer a las rutas de carrito dentro de una ventana de un minuto. Su objetivo es reducir abuso accidental o intencional y evitar que el controlador y la base de datos reciban una cantidad ilimitada de trabajo.

No es autenticación, autorización ni una defensa única contra ataques distribuidos.

## Límites verificados

| Operación | Límite |
| --- | ---: |
| GET y HEAD | 120 solicitudes por minuto |
| POST de creación o actualización | 30 solicitudes por minuto |
| POST de eliminación | 20 solicitudes por minuto |
| DELETE | 20 solicitudes por minuto |

El filtro aplica a /cart y a cualquier ruta que empiece por /cart/. Clasifica GET y HEAD como GET; DELETE y un POST cuya ruta termina en /delete como DELETE; los demás métodos se clasifican como POST.

## Flujo

~~~mermaid
flowchart TD
    A[Solicitud HTTP] --> B[RateLimitFilter]
    B --> C[request.getRemoteAddr]
    C --> D[RateLimitService]
    D --> E[Bucket cliente + tipo]
    E --> F{¿Queda capacidad?}
    F -- Sí --> G[Continúa hacia Spring Security y Controller]
    F -- No --> H[HTTP 429 + Retry-After]
~~~

La identificación usa request.getRemoteAddr(). No se confía automáticamente en X-Forwarded-For. Si la dirección está vacía, el filtro utiliza unknown.

La clave interna combina la dirección y el tipo de operación. Por eso un cliente tiene contadores independientes para GET, POST y DELETE, y dos clientes distintos no comparten sus contadores.

## Ventana y decisión

RateLimitService usa una ventana fija de un minuto por bucket. Al llegar una solicitud:

1. Obtiene la hora monotónica.
2. Elige el límite según GET, POST o DELETE.
3. Localiza el bucket de la dirección y el tipo.
4. Si la ventana terminó, reinicia su contador.
5. Si el contador ya alcanzó el límite, rechaza.
6. Si todavía hay capacidad, incrementa el contador y deja continuar la cadena.

El RateLimitFilter se ejecuta antes del controlador. Una solicitud rechazada no realiza la operación CRUD.

## HTTP 429

Cuando se agota el límite, la respuesta tiene:

~~~text
HTTP 429 Too Many Requests
Retry-After: <segundos>
Content-Type: application/json
{"status":429,"error":"Too Many Requests","message":"Rate limit exceeded"}
~~~

Retry-After indica cuánto esperar aproximadamente antes de intentar de nuevo. Un cliente debe respetar ese valor; no se documenta ni se recomienda intentar evadir el límite.

## Memoria, expiración y concurrencia

Los valores actuales son:

| Configuración | Valor | Función |
| --- | ---: | --- |
| cache-max-size | 10.000 buckets | Capacidad máxima de buckets rastreados. |
| expiration-minutes | 10 minutos | Inactividad tras la que un bucket puede eliminarse. |
| cleanup-interval-ms | 60.000 ms | Frecuencia de limpieza programada. |

La aplicación activa scheduling en CartApplication. Cada 60 segundos, la tarea programada elimina buckets cuya última actividad tiene al menos 10 minutos. Si se alcanza la capacidad, se expulsa el bucket más antiguo según el mapa de acceso.

~~~text
IP sin actividad
      ↓
10 minutos
      ↓
bucket eliminado
      ↓
memoria acotada
~~~

RateLimitService protege el mapa con ReentrantLock, por lo que la actualización de contadores y la expulsión son thread-safe.

## Separación entre clientes

La prueba limitsOneClientButKeepsAnotherClientIndependent comprueba que un cliente puede agotar su propio límite sin bloquear a otro. La prueba returns429WithRetryAfterAndDoesNotBlockAnotherClient verifica además la respuesta 429, Retry-After y el paso de otra dirección.

## Limitación distribuida

El estado vive en la memoria de una sola instancia:

~~~text
Rate limiter
     ↓
memoria local de Spring
~~~

Por tanto:

- al reiniciar Spring, los buckets se pierden y comienzan de nuevo;
- si existen varias instancias, cada una mantiene contadores distintos;
- un proxy puede hacer que varias solicitudes parezcan venir de una dirección, o que la dirección observada no represente al cliente final.

Para una arquitectura distribuida, una mejora futura sería guardar los contadores en Redis u otro almacenamiento compartido y definir cuidadosamente la confianza en proxies.

