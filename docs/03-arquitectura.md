# 03 — Arquitectura

Cart es un microservicio Spring Boot independiente. El panel Angelow envía consultas HTTP al endpoint REST, el controlador valida paginación y búsqueda, el repositorio delega la lectura en JPA/Hibernate y PostgreSQL (`cart-db`) almacena los registros.

## Flujo general

~~~mermaid
flowchart TD
    A[Navegador] --> B[SecurityHeadersFilter]
    B --> C[RateLimitFilter]
    C --> D[Spring Security / CSRF]
    D --> E[CartController]
    E --> F[DTO + Bean Validation]
    F --> G[CartRepository]
    G --> H[JPA / Hibernate]
    H --> I[(PostgreSQL Angelow: carts)]
~~~

Los filtros se ejecutan antes de la lógica del controlador. Un límite excedido devuelve 429 sin llegar al controlador; un POST sin token CSRF válido es rechazado por Spring Security. Las páginas HTML las renderiza Thymeleaf.

## Estructura real de código

~~~text
src/
├── main/
│   ├── java/com/example/cart/
│   │   ├── CartApplication.java
│   │   ├── config/
│   │   │   └── CartProperties.java
│   │   ├── controller/
│   │   │   └── CartController.java
│   │   ├── dto/
│   │   │   ├── CartCreateRequest.java
│   │   │   └── CartUpdateRequest.java
│   │   ├── entity/
│   │   │   └── Cart.java
│   │   ├── exception/
│   │   │   ├── CartNotFoundException.java
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   └── InvalidCartRequestException.java
│   │   ├── repository/
│   │   │   └── CartRepository.java
│   │   └── security/
│   │       ├── RateLimitFilter.java
│   │       ├── RateLimitProperties.java
│   │       ├── RateLimitService.java
│   │       ├── SecurityConfig.java
│   │       └── SecurityHeadersFilter.java
│   └── resources/
│       ├── application.properties
│       └── templates/
│           ├── cart/
│           │   ├── create.html
│           │   ├── detail.html
│           │   ├── edit.html
│           │   └── list.html
│           ├── error/
│           │   ├── 400.html
│           │   ├── 404.html
│           │   └── 500.html
│           └── fragments/
│               ├── footer.html
│               └── header.html
└── test/java/com/example/cart/
    ├── CartApplicationTests.java
    ├── TestCartApplication.java
    ├── TestcontainersConfiguration.java
    ├── controller/CartControllerSecurityTest.java
    └── security/
        ├── CsrfProtectionTest.java
        ├── RateLimitFilterTest.java
        ├── RateLimitServiceTest.java
        └── SecurityHeadersFilterTest.java
~~~

## Responsabilidad de cada capa

### Aplicación y configuración

CartApplication es el punto de entrada con SpringBootApplication y activa la ejecución programada mediante EnableScheduling, necesaria para limpiar buckets expirados del rate limiter. CartProperties enlaza app.cart.* y valida los límites de paginación.

### Controlador

CartController está bajo /cart. Recibe los parámetros HTTP, crea los DTOs para los formularios, valida IDs y paginación, llama al repositorio y devuelve nombres de vistas Thymeleaf o redirecciones.

El InitBinder de cart permite enlazar únicamente userId y sessionId. Por eso un cliente no puede establecer desde el formulario el ID ni las fechas de la entidad.

### DTOs

CartCreateRequest y CartUpdateRequest son objetos distintos de Cart. Ambos validan longitudes; el DTO de actualización contiene un id para representar el formulario, pero el binder no lo acepta como dato modificable.

### Entidad

Cart se mapea a la tabla `carts`. Su id es autogenerado por PostgreSQL. El microservicio no modifica el esquema compartido.

### Repositorio

CartRepository extiende JpaRepository<Cart, Integer>. Esto proporciona findAll(Pageable), findById, save y delete sin consultas SQL concatenadas manualmente.

### Excepciones

GlobalExceptionHandler transforma excepciones de recurso inexistente en 404, entradas inválidas y errores de binding en 400, y cualquier excepción no controlada en una vista 500. Los detalles internos se registran en el servidor, no se envían al navegador.

### Seguridad

SecurityConfig habilita CSRF, desactiva login de formulario y HTTP Basic, y permite las solicitudes (permitAll). SecurityHeadersFilter añade los headers HTTP. RateLimitFilter y RateLimitService aplican los límites por dirección remota.

## Configuración de application.properties

La configuración actual se agrupa así:

| Grupo | Propiedades verificadas | Propósito |
| --- | --- | --- |
| Aplicación y puerto | spring.application.name=cart, server.port=8081 | Nombre y puerto HTTP. |
| Datasource | URL JDBC parametrizada hacia localhost:5435/angelow_cart | Conexión directa con la base de carritos de Angelow. |
| JPA | ddl-auto=none, show-sql=true, SQL formateado | Consulta el esquema existente sin intentar migrarlo. |
| Rate limiting | app.rate-limit.* | Límites, capacidad, expiración y limpieza. |
| Paginación | app.cart.default-page-size=20, max-page-size=100, max-page-number=10000 | Acota las consultas de listado. |
| Formularios | max-http-form-post-size=128KB, max-swallow-size=128KB, max-parameter-count=100 | Evita formularios desproporcionados. |
| Errores | spring.web.error.include-* en never o false | Evita exponer stack traces, mensajes, excepciones o rutas. |
| Seguridad | Exclusión de UserDetailsServiceAutoConfiguration | Evita generar una contraseña temporal que no se utiliza; no crea autenticación. |
| Logging | logging.file.name=logs/cart.log y propiedades rollingpolicy.* | Consola, archivo y rotación de Logback. |

La contraseña real no se reproduce en esta documentación. En un despliegue seguro, las credenciales no deberían vivir en texto plano dentro del repositorio.

## Dependencias principales de pom.xml

| Dependencia | Función |
| --- | --- |
| spring-boot-starter-data-jpa | Repositorios Spring Data, JPA y Hibernate. |
| spring-boot-starter-webmvc | Controladores MVC, Spring Web y servidor web embebido. |
| spring-boot-starter-thymeleaf | Renderizado de vistas HTML. |
| spring-boot-starter-validation | Jakarta Bean Validation y @Valid. |
| spring-boot-starter-security | Cadena de filtros y protección CSRF. |
| postgresql | Driver JDBC de PostgreSQL en runtime. |
| spring-boot-docker-compose | Integración opcional de Spring Boot con Compose en runtime. |
| Dependencias *-test y Testcontainers | Pruebas MVC, JPA, contexto y PostgreSQL temporal. |

## Estado de autenticación

La presencia de Spring Security no significa que existan usuarios autenticados:

~~~text
Spring Security instalado
        ≠
login, usuarios, roles o permisos
~~~

En este proyecto anyRequest().permitAll() permite el CRUD sin iniciar sesión. La seguridad implementada protege la integridad de las peticiones (CSRF), añade headers y limita solicitudes, pero no controla quién puede ver o modificar un carrito.
