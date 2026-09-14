# 01 — Instalación y ejecución

Esta guía prepara el proyecto desde cero en Windows. Los comandos están escritos para PowerShell, pero también pueden adaptarse a otra terminal.

## Requisitos

- JDK 21 instalado y disponible en PATH. El proyecto declara Java 21 en pom.xml; compruébalo con java -version.
- Docker Desktop iniciado, con Docker Engine disponible.
- PowerShell o una terminal equivalente.
- No es obligatorio instalar Maven globalmente: el repositorio incluye mvnw y mvnw.cmd.

El entorno de validación de esta documentación tenía Java 26.0.2, que pudo ejecutar la suite, pero la versión objetivo declarada por el proyecto sigue siendo Java 21.

## 1. Obtener el proyecto

Clona o descomprime el proyecto en una carpeta local. Después abre PowerShell y entra en la carpeta que contiene pom.xml y compose.yaml:

~~~powershell
cd C:\ruta\al\proyecto\cart
~~~

Confirma que estás en la carpeta correcta:

~~~powershell
Get-ChildItem pom.xml, compose.yaml, mvnw.cmd
~~~

## 2. Comprobar Java y Docker

~~~powershell
java -version
docker --version
docker compose version
~~~

Si Docker Desktop está apagado, inicia Docker Desktop y repite los comandos antes de continuar.

## 3. Iniciar Angelow y su base de carritos

Desde `C:\laragon\www\Angelow_microservices`, inicia `cart-db` para que el servicio Java consuma la base compartida:

~~~powershell
docker compose up -d cart-db
~~~

Comprueba el estado:

~~~powershell
docker compose ps
~~~

Debes ver `angelow_cart_db` en estado `Up` y el puerto publicado 5435 hacia el puerto 5432 del contenedor. Si acaba de arrancar y todavía no acepta conexiones, revisa:

~~~powershell
docker compose logs --tail 100 cart-db
~~~

## 4. Iniciar el microservicio Java

En otra ventana de PowerShell, vuelve a la raíz del proyecto y ejecuta el wrapper de Maven para Windows:

~~~powershell
.\mvnw.cmd spring-boot:run
~~~

La aplicación lee la configuración de src/main/resources/application.properties, escucha en el puerto 8081 y conecta con PostgreSQL publicado en localhost:5435.

## 5. Abrir la aplicación

Abre:

~~~text
http://localhost:8081/api/admin/carts
~~~

La vista didáctica MVC original continúa disponible en `http://localhost:8081/cart`.

## 6. Detener el entorno

Para detener Spring, pulsa Ctrl+C en la ventana donde se está ejecutando.

Para detener el contenedor Java sin eliminar los datos de Angelow:

~~~powershell
docker compose down
~~~

Para iniciar de nuevo la API:

~~~powershell
docker compose up -d
~~~

> **Advertencia:** no elimines volúmenes desde Angelow si deseas conservar `cart_db_data`; contiene los registros compartidos.

## Comprobación rápida

El flujo mínimo completo es:

~~~powershell
java -version
docker --version
docker compose up -d
docker compose ps
.\mvnw.cmd spring-boot:run
~~~

Si la aplicación no inicia, consulta [10 — Troubleshooting](10-troubleshooting.md).
