# 06 — Logging

La aplicación usa SLF4J desde el código y Logback como implementación. Cada evento puede aparecer en la consola y en el archivo de aplicación:

~~~text
logs/cart.log
~~~

La ruta es relativa al directorio desde el que se inicia Spring Boot. Los archivos generados en runtime están excluidos por .gitignore. El archivo LOGGING.md de la raíz contiene una referencia breve; este documento reúne la explicación operativa completa.

## Tres cosas distintas

No deben confundirse:

~~~mermaid
flowchart TD
    A[Spring Boot] --> B[Consola]
    A --> C[logs/cart.log]
    C --> D[Logback + rotación + .gz]

    E[Docker] --> F[Contenedor MySQL]
    F --> G[stdout / stderr]
    G --> H[Docker logging]
    F --> I[/var/lib/mysql]
    I --> J[Volumen mysql_data]
~~~

1. **Datos MySQL:** tablas y registros de negocio dentro de mysql_data.
2. **Logs Docker/MySQL:** salida stdout/stderr del contenedor, consultable con docker compose logs mysql.
3. **Logs Spring:** eventos de la aplicación en consola y logs/cart.log.

## Niveles usados

- **INFO:** operación normal relevante, como crear, actualizar o eliminar un carrito.
- **WARN:** solicitud inválida o carrito inexistente; merece atención, pero no implica necesariamente un fallo del servidor.
- **ERROR:** fallo de persistencia o excepción inesperada. El evento incluye la excepción para diagnóstico del servidor.

## Mensajes reales del código

Los siguientes textos se extraen de CartController y GlobalExceptionHandler. {} son placeholders de SLF4J que se sustituyen durante la ejecución:

### INFO

~~~text
Creating cart userId={}
Cart created successfully cartId={} userId={}
Cart updated cartId={} userId={}
Cart deleted cartId={}
~~~

El código registra userId en las operaciones de creación y actualización, y no registra sessionId en esos mensajes. Revisa la política de datos de tu entorno antes de considerar userId un dato inocuo.

### WARN

~~~text
Invalid cart request operation=create errorCount={}
Invalid cart request operation=update cartId={} errorCount={}
Cart not found cartId={}
Invalid cart request
~~~

### ERROR

~~~text
Failed to list carts
Failed to create cart userId={}
Failed to update cart cartId={}
Failed to delete cart cartId={}
Failed to find cart cartId={}
Unexpected error while processing cart request
~~~

Cuando el código pasa una excepción al logger, Logback conserva el stack trace en el destino de logging del servidor. El navegador recibe una vista genérica 500 y no ese stack trace.

## Consultar el archivo de Spring

~~~powershell
Get-Content .\logs\cart.log
Get-Content .\logs\cart.log -Wait
Get-Content .\logs\cart.log -Tail 100
~~~

## Rotación de Logback

La configuración actual es:

| Propiedad | Valor | Efecto |
| --- | --- | --- |
| Archivo activo | logs/cart.log | Destino principal del archivo. |
| Patrón histórico | logs/cart-%d{yyyy-MM-dd}.%i.log.gz | Fecha, índice y compresión gzip. |
| Tamaño máximo | 10 MB | Al alcanzar el tamaño, el archivo rota. |
| max-history | 7 | Conserva hasta 7 archivos históricos. |
| total-size-cap | 100 MB | Tope acumulado de archivos archivados. |

El archivo activo puede ocupar aproximadamente otros 10 MB. Por eso el consumo aproximado máximo es 110 MB más metadatos del sistema de archivos.

~~~mermaid
flowchart TD
    A[logs/cart.log] --> B{¿Llega a 10 MB?}
    B -- No --> A
    B -- Sí --> C[Rotación]
    C --> D[Archivo histórico]
    D --> E[Compresión .gz]
    E --> F[Retención: hasta 7 y 100 MB]
    F --> G[Eliminación de históricos antiguos]
~~~

Reiniciar Spring no elimina el archivo ni sus históricos. Si en el futuro Spring se ejecuta dentro de un contenedor, debe montarse un directorio del host en /app/logs para conservarlos.

## Contraseña temporal de Spring Security

La aplicación excluye UserDetailsServiceAutoConfiguration porque el CRUD actual no utiliza login. Esto evita que Spring cree y registre una contraseña temporal que no tiene utilidad para este proyecto.

## Datos que no deben registrarse

La implementación no añade deliberadamente a sus mensajes:

- sessionId;
- contraseñas;
- tokens;
- cookies;
- credenciales;
- cabeceras Authorization.

El usuario y los IDs de carrito sí pueden formar parte de algunos mensajes funcionales, tal como muestran los ejemplos reales. Además, show-sql está activado para desarrollo, por lo que Hibernate puede mostrar sentencias SQL en la consola.

## Logs de MySQL en Docker

~~~powershell
docker compose logs mysql
docker compose logs --tail 100 mysql
docker compose logs -f mysql
~~~

Estos comandos muestran el historial o el flujo en tiempo real del servicio MySQL administrado por Docker. No leen logs/cart.log.

El Compose usa el driver local con max-size 10m y max-file 3: como aproximación, hasta tres archivos de unos 10 MB para la salida del contenedor. Si se elimina y recrea el contenedor, sus logs Docker anteriores no se recuperan; el volumen mysql_data sí puede conservar los datos.
