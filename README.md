# Cart

Aplicación educativa en Spring Boot para administrar carritos de compra mediante una interfaz web MVC. Permite listar, crear, consultar, editar y eliminar registros de carrito persistidos en MySQL. El proyecto incluye validación de entrada, paginación acotada, protección CSRF, headers HTTP de seguridad, rate limiting, logging con rotación y pruebas automatizadas.

> Estado documentado: 31 de agosto de 2026. La documentación describe el código actual; no añade autenticación ni otras funcionalidades al proyecto.

## Tecnologías

Las dependencias se tomaron de pom.xml:

- Java 21 como versión objetivo del proyecto.
- Spring Boot 4.1.1.
- Spring MVC y Thymeleaf para la aplicación web.
- Spring Data JPA, Hibernate y MySQL Connector/J para persistencia.
- Jakarta Bean Validation para validar los formularios.
- Spring Security para CSRF y configuración HTTP.
- Docker Compose y la integración de Docker Compose de Spring Boot para el entorno local.
- JUnit, Spring Boot Test, MockMvc y Testcontainers para pruebas.
- Maven Wrapper (mvnw y mvnw.cmd) para ejecutar Maven sin instalarlo globalmente.

## Características

- CRUD MVC de carritos en /cart.
- Persistencia en la tabla carts de MySQL.
- DTOs separados para creación y actualización.
- Allowlist de campos para evitar over-posting.
- Validación de userId hasta 50 caracteres y sessionId hasta 255 caracteres.
- Paginación con tamaño predeterminado 20, máximo 100 y página máxima 10.000.
- Protección CSRF en formularios que cambian el estado.
- Headers X-Content-Type-Options, Referrer-Policy y Content Security Policy.
- Rate limiting por dirección remota para rutas de carrito.
- Respuestas controladas para 400, 404, 429 y 500.
- Logs en consola y en logs/cart.log, con rotación y compresión.
- MySQL ejecutado en Docker Compose con volumen persistente.
- Pruebas unitarias, MVC, de seguridad, de rate limiting y de contexto con Testcontainers.

## Inicio rápido

Desde la carpeta raíz del proyecto:

~~~powershell
docker compose up -d
docker compose ps
.\mvnw.cmd spring-boot:run
~~~

Después abre http://localhost:8081/cart.

La guía completa está en [docs/01-instalacion.md](docs/01-instalacion.md). Para aprender a usar la interfaz, continúa con [docs/02-uso.md](docs/02-uso.md).

## Documentación

| Documento | Contenido |
| --- | --- |
| [01 — Instalación](docs/01-instalacion.md) | Requisitos, preparación desde cero y ejecución local. |
| [02 — Uso](docs/02-uso.md) | Manual de la interfaz para usuarios. |
| [03 — Arquitectura](docs/03-arquitectura.md) | Capas, estructura, configuración y flujo general. |
| [04 — CRUD](docs/04-crud.md) | Mappings, DTOs, validaciones y flujo técnico de cada operación. |
| [05 — Docker y MySQL](docs/05-docker-mysql.md) | Contenedor, Compose, volumen y logs Docker. |
| [06 — Logging](docs/06-logging.md) | Niveles, mensajes reales, consulta y rotación de logs. |
| [07 — Seguridad](docs/07-seguridad.md) | Validación, DTOs, SQL Injection, CSRF, XSS y headers. |
| [08 — Rate limiting](docs/08-rate-limiting.md) | Límites, buckets, HTTP 429 y limitaciones distribuidas. |
| [09 — Pruebas](docs/09-pruebas.md) | Comandos, Testcontainers y las 18 pruebas actuales. |
| [10 — Troubleshooting](docs/10-troubleshooting.md) | Diagnóstico de puertos, MySQL, Spring, errores y logs. |
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

En la validación del 31 de agosto de 2026, .\mvnw.cmd test terminó con 18 pruebas exitosas. El entorno utilizado tenía Java 26.0.2, mientras que pom.xml declara Java 21 como versión objetivo. Docker Compose validó el archivo y el servicio MySQL estaba activo en el puerto publicado 3309.

Spring Security está instalado, pero todas las solicitudes están configuradas con permitAll; no existe login, usuario, rol ni autorización de negocio. La protección efectiva actualmente se concentra en CSRF, headers HTTP, validación, límites y manejo seguro de errores.

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
- **Container:** proceso aislado que ejecuta una imagen, como MySQL.
- **Image:** plantilla inmutable desde la que se crea un contenedor.
- **Volume:** almacenamiento administrado por Docker que sobrevive al contenedor.
- **Testcontainers:** biblioteca que levanta dependencias reales en contenedores temporales durante las pruebas.

## Limitaciones conocidas

- No hay autenticación ni autorización real.
- Las credenciales de MySQL del entorno local siguen estando en la configuración del proyecto; para producción deben migrarse a variables de entorno, Docker Secrets o un gestor de secretos.
- El rate limiter vive en memoria local: se reinicia al reiniciar Spring y no comparte estado entre instancias.
- La CSP usa unsafe-inline para mantener la compatibilidad actual del frontend.

Estas limitaciones se explican con contexto en [docs/07-seguridad.md](docs/07-seguridad.md) y [docs/08-rate-limiting.md](docs/08-rate-limiting.md).
