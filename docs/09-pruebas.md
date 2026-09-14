# 09 — Pruebas

## Ejecutar la suite

Desde la raíz del proyecto:

~~~powershell
.\mvnw.cmd test
~~~

Este ciclo compila lo necesario y ejecuta las pruebas de Surefire. Los reportes quedan en target/surefire-reports, que es un directorio generado y no forma parte del código fuente.

Para ejecutar el ciclo de verificación completo:

~~~powershell
.\mvnw.cmd verify
~~~

verify incluye la fase de pruebas y continúa con las validaciones del ciclo Maven. Para generar el artefacto ejecutable:

~~~powershell
.\mvnw.cmd clean package
~~~

clean elimina el directorio target generado por compilaciones anteriores; package recompila, prueba y empaqueta el JAR. El nombre verificado es:

~~~text
target/cart-0.0.1-SNAPSHOT.jar
~~~

Puede iniciarse con:

~~~powershell
java -jar target/cart-0.0.1-SNAPSHOT.jar
~~~

## Resultado verificado

El 14 de septiembre de 2026 se ejecutó .\mvnw.cmd test con este resultado:

~~~text
Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
~~~

Este resultado describe esa ejecución concreta; no es una garantía permanente para cambios futuros.

## Distribución de pruebas

| Clase | Pruebas | Qué cubre |
| --- | ---: | --- |
| CartApplicationTests | 1 | Carga del contexto Spring. |
| CartControllerSecurityTest | 11 | CRUD MVC, validaciones, IDs, paginación, over-posting, SQL-looking input y 500 seguro. |
| CsrfProtectionTest | 2 | Rechazo de POST sin CSRF y exposición del token en formularios. |
| RateLimitFilterTest | 1 | 429, Retry-After y separación entre clientes. |
| RateLimitServiceTest | 2 | Límite por cliente, expiración y capacidad máxima. |
| SecurityHeadersFilterTest | 1 | Headers de seguridad. |
| AdminCartRestControllerTest | 3 | Consulta REST paginada, estadísticas, filtros y errores controlados. |
| CartSearchTest | 3 | Búsquedas AND/OR y conservación de criterios. |
| CartValidationTest | 3 | Validación de fechas, userId y sessionId. |
| **Total** | **27** | **27 exitosas en la ejecución verificada.** |

## Inventario de pruebas reales

### CartApplicationTests

| Método | Verifica |
| --- | --- |
| contextLoads | Que el contexto completo de Spring pueda iniciar. |

### CartControllerSecurityTest

| Método | Verifica |
| --- | --- |
| acceptsValidCreateAndDoesNotBindInternalFields | Creación válida y rechazo de campos internos durante el binding. |
| rejectsOversizedInputBeforePersistence | Que un userId de más de 50 caracteres se rechace antes de persistir. |
| treatsSqlLookingValueAsData | Que una entrada con apariencia de SQL se trate como texto. |
| rejectsNegativeIdWithBadRequest | Que un ID negativo produzca 400 sin consultar el repositorio. |
| rejectsNonNumericIdWithBadRequest | Que un ID no numérico produzca 400. |
| returnsNotFoundWithoutExposingInternalException | Que un carrito inexistente produzca 404 y una vista controlada. |
| rejectsExcessivePageSize | Que un size abusivo produzca 400 sin consultar datos. |
| updateOnlyChangesAllowedFields | Que update conserve ID y fechas y solo cambie campos permitidos. |
| deletesThroughPostAndRequiresAnExistingCart | Que delete use POST y exija una entidad existente. |
| hidesUnexpectedPersistenceErrorsBehindGeneric500Page | Que un fallo inesperado produzca una página 500 genérica. |
| usesBoundedPageQueryForListing | Que el listado use una consulta paginada. |

### CsrfProtectionTest

| Método | Verifica |
| --- | --- |
| rejectsStateChangingRequestWithoutCsrfToken | Que un POST sin token sea rechazado con 403. |
| exposesCsrfTokenToThymeleafForms | Que el formulario de Thymeleaf contenga el campo CSRF. |

### RateLimitFilterTest

| Método | Verifica |
| --- | --- |
| returns429WithRetryAfterAndDoesNotBlockAnotherClient | 429, Retry-After y que otra dirección pueda continuar. |

### RateLimitServiceTest

| Método | Verifica |
| --- | --- |
| limitsOneClientButKeepsAnotherClientIndependent | Contadores independientes por cliente. |
| expiresInactiveBucketsAndNeverExceedsConfiguredCapacity | Expiración y capacidad máxima de buckets. |

### SecurityHeadersFilterTest

| Método | Verifica |
| --- | --- |
| addsBasicSecurityHeaders | X-Content-Type-Options, Referrer-Policy y Content-Security-Policy. |

## Testcontainers y PostgreSQL temporal

La prueba de contexto importa TestcontainersConfiguration, que crea un PostgreSQLContainer con una imagen PostgreSQL y conexión administrada por Spring Boot:

~~~mermaid
flowchart TD
    A[JUnit] --> B[Testcontainers]
    B --> C[PostgreSQL temporal]
    C --> D[Contexto Spring + JPA]
    D --> E[Prueba]
    E --> F[Recurso eliminado al terminar]
~~~

Este PostgreSQL usa un puerto dinámico y no es la base `cart-db` compartida de Angelow. Por eso las pruebas de contexto necesitan Docker Desktop funcionando aunque la base compartida no esté iniciada.

Las pruebas del controlador usan MockMvc y Mockito para aislar CartRepository. En consecuencia, la suite combina una prueba de contexto con base temporal y pruebas MVC enfocadas en comportamiento, seguridad y límites.

## Si falla Testcontainers

Comprueba:

~~~powershell
docker --version
docker info
~~~

Docker Engine debe estar disponible. Si el problema es una imagen no descargada, revisa la conexión de Docker Desktop y vuelve a ejecutar la suite. No elimines volúmenes del proyecto para solucionar un fallo de Testcontainers.
