# 05 — Docker y MySQL

## Conceptos básicos

- **Imagen:** plantilla de solo lectura desde la que Docker crea un contenedor.
- **Contenedor:** proceso aislado que ejecuta una imagen.
- **Volumen:** almacenamiento administrado por Docker que vive fuera de la capa efímera del contenedor.
- **Compose:** formato y herramienta para definir servicios, puertos, volúmenes y opciones de ejecución en un archivo.

En este proyecto Compose administra MySQL. Spring Boot se ejecuta normalmente desde el host con Maven Wrapper o como JAR.

## Compose actual

El archivo compose.yaml define un único servicio:

| Bloque | Configuración verificada | Función |
| --- | --- | --- |
| image | mysql:8.4.7 | Imagen de MySQL utilizada. |
| environment | Base de datos, usuario y credenciales de desarrollo | Inicializa el servidor MySQL. Los valores sensibles no se copian aquí. |
| ports | 3309:3306 | Publica el puerto 3306 del contenedor como 3309 en el host. |
| volumes | mysql_data:/var/lib/mysql | Conserva los datos fuera del ciclo de vida del contenedor. |
| logging | driver local, max-size 10m, max-file 3 | Rota la salida stdout/stderr de Docker. |

La aplicación usa la base de datos angelow y la URL JDBC apunta a localhost:3309. El nombre de usuario y la credencial deben coincidir con la configuración local de Compose y application.properties; no se reproducen en esta documentación.

~~~text
HOST
localhost:3309
      │
      ▼
CONTENEDOR MYSQL
3306
~~~

La expresión 3309:3306 siempre se interpreta como puerto del host a la izquierda y puerto del contenedor a la derecha.

## Persistencia de datos

El volumen nombrado mysql_data está montado en /var/lib/mysql, que es la ubicación de datos de MySQL dentro del contenedor:

~~~mermaid
flowchart TD
    A[Spring Boot en el host] -->|JDBC localhost:3309| B[Contenedor MySQL]
    B --> C[( /var/lib/mysql )]
    C --> D[(volumen mysql_data)]
    B --> E[stdout / stderr]
    E --> F[Docker logging]
~~~

Esto separa tres conceptos:

1. **Datos MySQL:** tablas y registros almacenados en mysql_data.
2. **Logs MySQL/Docker:** salida del contenedor consultable con docker compose logs mysql.
3. **Logs Spring:** archivo logs/cart.log gestionado por Logback.

## Comandos principales

### Iniciar o crear el servicio

~~~powershell
docker compose up -d
~~~

Crea el contenedor si no existe, reutiliza el volumen si ya existe y lo deja ejecutándose en segundo plano.

### Consultar el estado

~~~powershell
docker compose ps
~~~

Muestra si el servicio está Up y qué puertos publica. Up indica que el contenedor está en ejecución; puede ser necesario esperar a que MySQL termine de inicializarse.

### Consultar logs de MySQL

~~~powershell
docker compose logs mysql
docker compose logs --tail 100 mysql
docker compose logs -f mysql
~~~

El primer comando muestra los logs disponibles, el segundo limita la salida a las últimas 100 líneas y el tercero sigue el flujo en tiempo real. Estos no son los logs de Spring.

### Reiniciar solo MySQL

~~~powershell
docker compose restart mysql
~~~

Detiene y vuelve a iniciar el mismo contenedor. El volumen mysql_data permanece y los datos continúan disponibles.

### Detener el entorno

~~~powershell
docker compose down
~~~

Elimina el contenedor y la red creada por Compose, pero conserva el volumen nombrado. Si luego ejecutas docker compose up -d, MySQL puede reutilizar los datos existentes.

> **Advertencia:** docker compose down -v elimina los volúmenes asociados. En este proyecto puede destruir los datos persistidos de MySQL en mysql_data. No ejecutar si se desea conservar la base de datos.

## Ciclos habituales

~~~text
docker compose restart
  → reinicia el contenedor
  → conserva volumen y datos

docker compose down
  → elimina contenedor y red
  → conserva volumen y datos

docker compose up -d
  → crea o inicia el contenedor
  → reutiliza mysql_data

docker compose down -v
  → elimina contenedor, red y volumen
  → los datos persistidos pueden perderse
~~~

## Credenciales y producción

El Compose actual contiene credenciales de desarrollo en su configuración y application.properties contiene la conexión correspondiente. Esta documentación las omite deliberadamente. En producción deben inyectarse mediante variables de entorno, Docker Secrets o un gestor de secretos, y nunca publicarse en el repositorio.

## Diferencia frente a Testcontainers

Las pruebas de contexto usan Testcontainers para levantar un MySQL temporal con mysql:latest y un puerto dinámico. Ese contenedor de pruebas no es necesariamente el servicio mysql de compose.yaml.

