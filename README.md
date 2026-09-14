# Cart

Aplicación educativa en Spring Boot que funciona como un microservicio REST de consulta administrativa para carritos. Consume directamente la base PostgreSQL `angelow_cart` del entorno Angelow, expone una consulta paginada y mantiene la vista MVC original como apoyo didáctico. No crea ni modifica el esquema compartido: `ddl-auto=none`.

> Estado documentado: 14 de septiembre de 2026. La documentación describe el código actual; no añade autenticación ni otras funcionalidades al proyecto.

## Tecnologías

Las dependencias se tomaron de pom.xml:

- Java 21 como versión objetivo del proyecto.
- Spring Boot 4.1.1.
- Spring MVC y Thymeleaf para la aplicación web.
- Spring Data JPA, Hibernate y PostgreSQL JDBC para persistencia.
- Jakarta Bean Validation para validar los formularios.
- Spring Security para CSRF y configuración HTTP.
- Docker Compose y la integración de Docker Compose de Spring Boot para el entorno local.
- JUnit, Spring Boot Test, MockMvc y Testcontainers para pruebas.
- Maven Wrapper (mvnw y mvnw.cmd) para ejecutar Maven sin instalarlo globalmente.

## Características

- CRUD MVC de carritos en /cart.
- Persistencia de consulta sobre la tabla `carts` de PostgreSQL.
- DTOs separados para creación y actualización.
- Allowlist de campos para evitar over-posting.
- Validación de userId hasta 50 caracteres y sessionId hasta 255 caracteres.
- Búsqueda paginada por dos campos con `AND` y búsqueda global por `id`, `userId` y `sessionId` con `OR`.
- Paginación con tamaño predeterminado 20, máximo 100 y página máxima 10.000.
- Protección CSRF en formularios que cambian el estado.
- Headers X-Content-Type-Options, Referrer-Policy y Content Security Policy.
- Rate limiting por dirección remota para rutas de carrito.
- Respuestas controladas para 400, 404, 429 y 500.
- Logs en consola y en logs/cart.log, con rotación y compresión.
- API REST dockerizada y CORS configurable para el frontend de Angelow.
- Conexión PostgreSQL directa a `cart-db` sin duplicar la base de datos.
- Pruebas unitarias, MVC, de seguridad, de rate limiting y de contexto con Testcontainers.

## Inicio rápido

Desde la carpeta raíz del proyecto, con `cart-db` de Angelow activo en el puerto publicado 5435:

~~~powershell
docker compose up -d --build
docker compose ps
.\mvnw.cmd spring-boot:run
~~~

El endpoint queda disponible en `http://localhost:8081/api/admin/carts` y acepta `page`, `size` y `search`. Ejemplo: `http://localhost:8081/api/admin/carts?page=1&size=20`.

La guía completa está en [docs/01-instalacion.md](docs/01-instalacion.md). Para aprender a usar la interfaz, continúa con [docs/02-uso.md](docs/02-uso.md).

## Documentación

| Documento | Contenido |
| --- | --- |
| [01 — Instalación](docs/01-instalacion.md) | Requisitos, preparación desde cero y ejecución local. |
| [02 — Uso](docs/02-uso.md) | Manual de la interfaz para usuarios. |
| [03 — Arquitectura](docs/03-arquitectura.md) | Capas, estructura, configuración y flujo general. |
| [04 — CRUD](docs/04-crud.md) | Mappings, DTOs, validaciones y flujo técnico de cada operación. |
| [05 — Docker y PostgreSQL](docs/05-docker-mysql.md) | Contenedor, Compose, conexión compartida y logs Docker. |
| [06 — Logging](docs/06-logging.md) | Niveles, mensajes reales, consulta y rotación de logs. |
| [07 — Seguridad](docs/07-seguridad.md) | Validación, DTOs, SQL Injection, CSRF, XSS y headers. |
| [08 — Rate limiting](docs/08-rate-limiting.md) | Límites, buckets, HTTP 429 y limitaciones distribuidas. |
| [09 — Pruebas](docs/09-pruebas.md) | Comandos, Testcontainers y las 27 pruebas actuales. |
| [10 — Troubleshooting](docs/10-troubleshooting.md) | Diagnóstico de puertos, PostgreSQL compartido, Spring, errores y logs. |
| [11 — Búsquedas y paginación](docs/11-busquedas.md) | Consultas AND/OR, límites de entrada y conservación de filtros en la vista. |
| [LOGGING.md](LOGGING.md) | Referencia breve existente sobre logs y persistencia. |

## Pruebas y compilación

~~~powershell
.\mvnw.cmd test
.\mvnw.cmd verify
.\mvnw.cmd clean package
~~~

El artefacto ejecutable verificado por el proyecto es:

~~~text
target/cart-0.0.1-SNAPSHOT.jar
~~~

Para ejecutarlo después de compilar:

~~~powershell
java -jar target/cart-0.0.1-SNAPSHOT.jar
~~~

Consulta [docs/09-pruebas.md](docs/09-pruebas.md) para conocer la diferencia entre test, verify y package.

## Estado técnico

La API consulta PostgreSQL por `localhost:5435` en ejecución local o por `host.docker.internal:5435` dentro de su Compose independiente. El entorno de validación puede usar Java 26.0.2, mientras que pom.xml declara Java 21 como versión objetivo.

Spring Security está instalado, pero el endpoint educativo de consulta está abierto y protegido por CORS configurable, límites de consulta, headers HTTP y manejo seguro de errores. La autorización administrativa real permanece en el panel Angelow; este servicio no duplica autenticación ni lógica de negocio.

## Estructura resumida

~~~text
cart/
├── .gitattributes
├── .gitignore
├── .mvn/wrapper/maven-wrapper.properties
├── HELP.md
├── src/
│   ├── main/
│   │   ├── java/com/example/cart/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── exception/
│   │   │   ├── repository/
│   │   │   └── security/
│   │   └── resources/
│   │       ├── application.properties
│   │       └── templates/
│   └── test/java/com/example/cart/
├── docs/
├── logs/                    # generado en runtime; ignorado por Git
├── compose.yaml
├── LOGGING.md
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
~~~

target/, los logs históricos, los entornos de IDE y otros archivos generados se excluyen de este árbol.

## Glosario breve

- **Spring Boot:** plataforma que configura y ejecuta la aplicación Java con convenciones razonables.
- **MVC:** separación entre modelo/datos, vistas HTML y controladores HTTP.
- **DTO:** objeto de transporte que define los campos que una solicitud puede enviar.
- **Entity:** clase Cart que representa una fila de la tabla carts.
- **Repository:** interfaz de acceso a datos; CartRepository extiende JpaRepository.
- **ORM:** mapeo entre objetos Java y tablas relacionales.
- **JPA:** API estándar de Java para persistencia ORM.
- **Hibernate:** implementación de JPA utilizada por Spring Boot.
- **CSRF:** ataque que intenta enviar una acción desde otro sitio usando una sesión del navegador.
- **XSS:** inyección de contenido ejecutable en una página web.
- **Rate limiting:** límite de solicitudes para controlar abuso o sobrecarga.
- **SLF4J:** API de logging usada por el código Java.
- **Logback:** implementación que escribe y rota los logs.
- **Container:** proceso aislado que ejecuta una imagen, como la API Java.
- **Image:** plantilla inmutable desde la que se crea un contenedor.
- **Volume:** almacenamiento administrado por Docker que sobrevive al contenedor.
- **Testcontainers:** biblioteca que levanta dependencias reales en contenedores temporales durante las pruebas.

## Limitaciones conocidas

- No hay autenticación ni autorización real.
- Las credenciales de PostgreSQL se inyectan mediante variables de entorno; para producción deben mantenerse en Docker Secrets o un gestor de secretos.
- El rate limiter vive en memoria local: se reinicia al reiniciar Spring y no comparte estado entre instancias.
- La CSP usa unsafe-inline para mantener la compatibilidad actual del frontend.

Estas limitaciones se explican con contexto en [docs/07-seguridad.md](docs/07-seguridad.md) y [docs/08-rate-limiting.md](docs/08-rate-limiting.md).
