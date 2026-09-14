# 05 — Docker y PostgreSQL compartido

El Compose de este proyecto administra únicamente la API Java. PostgreSQL pertenece al entorno Angelow y se consume por el puerto publicado 5435; no se crea una segunda base de datos.

## Inicio

Primero inicia la base compartida desde `C:\laragon\www\Angelow_microservices`:

~~~powershell
docker compose up -d cart-db
~~~

Después, desde este proyecto:

~~~powershell
docker compose up -d --build
docker compose ps
docker compose logs --tail 100 cart-api
~~~

La API queda disponible en `http://localhost:8081/api/admin/carts`. Dentro del contenedor, la conexión usa `host.docker.internal:5435` y `ddl-auto=none`, por lo que Spring no crea, altera ni elimina tablas.

## Configuración

| Variable | Valor local | Función |
| --- | --- | --- |
| `CART_DB_HOST` | `host.docker.internal` | Host publicado de `cart-db`. |
| `CART_DB_PORT` | `5435` | Puerto PostgreSQL publicado por Angelow. |
| `CART_DB_NAME` | `angelow_cart` | Base compartida consultada. |
| `CART_DB_USERNAME` | `postgres` | Usuario de la base. |
| `CART_DB_PASSWORD` | `root` | Credencial de desarrollo; inyectar en producción. |
| `CART_API_PORT` | `8081` | Puerto del host; permite usar otro, por ejemplo `8082`, si el predeterminado está ocupado. |
| `CART_CORS_ALLOWED_ORIGINS` | `*` | Orígenes permitidos para la consulta HTTP. |

El Compose monta `./logs` en `/app/logs` y limita la rotación de salida del contenedor a tres archivos de 10 MB.

## Separación de responsabilidades

~~~mermaid
flowchart LR
    A[Panel Angelow] -->|HTTP GET| B[Cart API Java]
    B -->|JDBC de solo esquema existente| C[(cart-db de Angelow)]
    D[Carrito público] --> E[cart-service Laravel]
    E --> C
~~~

El panel consume el endpoint Java, mientras que el flujo público conserva su servicio actual. Para retirar la integración educativa basta con detener este Compose y eliminar el cliente/ruta de carritos del frontend; no hay código Java dentro de Angelow.

## Detener

~~~powershell
docker compose down
~~~

Este comando elimina el contenedor Java y su red local, pero no toca el volumen `cart_db_data` porque pertenece al Compose de Angelow.

Las pruebas de contexto usan Testcontainers con PostgreSQL temporal; ese contenedor no es la base real compartida.
