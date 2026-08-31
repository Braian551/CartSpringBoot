# 07 — Seguridad

La seguridad del proyecto está compuesta por varias barreras pequeñas: validación de entradas, DTOs con allowlist, consultas gestionadas por JPA, CSRF, headers HTTP, límites de paginación, rate limiting y respuestas de error controladas.

No debe confundirse la presencia de Spring Security con autenticación:

~~~text
Spring Security instalado
        ≠
login, usuarios, roles o permisos
~~~

SecurityConfig permite cualquier solicitud con permitAll, desactiva formLogin y httpBasic, y mantiene CSRF habilitado. La aplicación protege la forma en que llegan las peticiones, pero no identifica al usuario que puede operar el CRUD.

## Bean Validation

CartCreateRequest y CartUpdateRequest aplican @Size:

| Campo | Máximo |
| --- | ---: |
| userId | 50 caracteres |
| sessionId | 255 caracteres |

Los campos son opcionales porque no tienen @NotNull ni @NotBlank. Si se supera un máximo, @Valid llena BindingResult; el controlador registra un WARN, vuelve al formulario y no persiste la entidad.

También se validan los parámetros de paginación:

- page entre 0 y 10.000, inclusive.
- size entre 1 y 100, inclusive.
- page y size deben poder convertirse al tipo numérico esperado.

El servidor añade además límites Tomcat de 128 KB para el formulario y 100 parámetros.

## DTOs y over-posting

La entidad Cart contiene más campos que los que un cliente debe controlar:

~~~text
Cart
├── id
├── userId
├── sessionId
├── createdAt
└── updatedAt
~~~

Las solicitudes usan objetos separados:

~~~text
Cliente
   ↓
CartCreateRequest o CartUpdateRequest
   ↓
allowlist: userId, sessionId
   ↓
Cart
~~~

CartController configura un WebDataBinder para aceptar únicamente userId y sessionId. En actualización, además, el controlador copia explícitamente solo esos dos valores sobre la entidad ya encontrada. El ID se valida desde la ruta, y las fechas permanecen bajo control de la entidad y sus callbacks JPA.

Este diseño evita que un formulario pueda sobrescribir campos internos aunque alguien añada parámetros como id, createdAt o updatedAt. Es una protección contra over-posting o mass assignment.

## SQL Injection

El repositorio solo extiende JpaRepository. El CRUD utiliza findAll(Pageable), findById, save y delete; no hay consultas SQL manuales que concatenen texto recibido del usuario.

Una entrada como:

~~~text
' OR '1'='1
~~~

es un valor de texto del campo userId. JPA/Hibernate construye y ejecuta la operación de persistencia con valores tratados como parámetros, por lo que ese texto no se convierte automáticamente en una condición SQL.

El mecanismo correcto es:

~~~mermaid
flowchart LR
    A[Entrada del usuario] --> B[Campo del DTO]
    B --> C[Entidad Cart]
    C --> D[JPA / Hibernate]
    D --> E[Valor parametrizado]
    E --> F[(MySQL)]
~~~

Esto no significa que se deba concatenar SQL en el futuro. Si se añaden consultas personalizadas, deben usar parámetros de JPA o del driver, y deben probarse de forma específica. La defensa no consiste en borrar comillas, punto y coma, SELECT o DROP: esos caracteres pueden ser datos legítimos y ese filtrado sería incompleto.

La prueba treatsSqlLookingValueAsData verifica que el texto con apariencia de SQL se conserva como dato al guardar mediante el repositorio simulado.

## CSRF

CSRF intenta provocar una operación que cambia estado desde un sitio distinto, aprovechando las credenciales implícitas del navegador. Spring Security mantiene un token que el formulario legítimo debe enviar.

Los formularios de crear, editar y eliminar incluyen un campo oculto con el token CSRF:

~~~text
Sitio atacante
     ↓
POST sin token válido
     ↓
Spring Security
     ↓
HTTP 403
     ↓
el controlador no procesa la operación
~~~

La prueba rejectsStateChangingRequestWithoutCsrfToken confirma el rechazo de un POST sin token. La prueba exposesCsrfTokenToThymeleafForms confirma que una vista Thymeleaf expone el campo. Las solicitudes GET de lectura no necesitan token CSRF porque no cambian el estado.

## XSS y Thymeleaf

La inspección de las plantillas actuales muestra que los valores dinámicos se imprimen con th:text o th:errors. No se encontró th:utext. th:text escapa el contenido antes de insertarlo en HTML, por lo que un valor de userId o sessionId no se interpreta directamente como marcado HTML.

Esto reduce el riesgo de XSS reflejado o almacenado en estos campos, pero no reemplaza una revisión al añadir nuevas plantillas o scripts. El JavaScript actual solo contiene una confirmación fija para la eliminación.

## Security headers

SecurityHeadersFilter se ejecuta con máxima prioridad y establece:

| Header | Valor actual | Protección |
| --- | --- | --- |
| X-Content-Type-Options | nosniff | Evita que el navegador intente adivinar un tipo MIME distinto. |
| Referrer-Policy | strict-origin-when-cross-origin | Limita la información enviada como Referer al cambiar de origen. |
| Content-Security-Policy | Ver detalle inferior | Restringe orígenes de scripts, estilos, imágenes, conexiones y formularios. |

La CSP actual es equivalente a:

~~~text
default-src 'self';
script-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com;
style-src 'self' 'unsafe-inline';
connect-src 'self' https://cdn.tailwindcss.com;
img-src 'self' data:;
base-uri 'self';
form-action 'self';
frame-ancestors 'none'
~~~

Se permite el CDN de Tailwind porque el header lo carga desde https://cdn.tailwindcss.com. También se permite unsafe-inline para scripts y estilos, una concesión de compatibilidad que conviene eliminar o reducir en una futura mejora. frame-ancestors none impide que la aplicación sea embebida en un frame por otro sitio.

La prueba addsBasicSecurityHeaders verifica los tres headers configurados por el filtro.

## Manejo seguro de errores

GlobalExceptionHandler traduce errores a vistas controladas:

- CartNotFoundException → 404 y error/404.
- InvalidCartRequestException, errores de binding, tipos, parámetros faltantes y contenido ilegible → 400 y error/400.
- Cualquier Exception no controlada → 500 y error/500.

La configuración de Spring desactiva la inclusión pública de stack trace, mensaje, excepción, errores de binding y path. El servidor registra el detalle técnico con ERROR cuando corresponde, pero la página 500 solo dice que no se pudo completar la solicitud.

## Paginación y límites

PageRequest limita el tamaño de las consultas de listado. El máximo size es 100 y el máximo page es 10.000. Esto evita aceptar una página desproporcionada que pueda consumir memoria o tiempo innecesarios.

El rate limiting añade un límite temporal por cliente; su funcionamiento se explica en [08 — Rate limiting](08-rate-limiting.md).

## Limitaciones y mejoras futuras

1. **Autenticación y autorización:** no existen login, usuarios, roles ni permisos. Cualquier cliente que llegue a la aplicación puede operar el CRUD, sujeto a los controles técnicos anteriores.
2. **Credenciales de MySQL:** el entorno local todavía tiene credenciales en Compose y en la configuración de Spring. Para producción deben inyectarse con variables de entorno, Docker Secrets o un gestor de secretos.
3. **Rate limiter local:** los buckets están en memoria de una instancia. Redis u otro almacén compartido sería necesario para varias instancias.
4. **CSP:** unsafe-inline sigue habilitado por compatibilidad con el frontend y es una oportunidad de endurecimiento.
5. **HTTPS y operación real:** el código documentado no configura terminación TLS, gestión de secretos ni observabilidad centralizada; deben resolverse en la infraestructura antes de exponerlo públicamente.

