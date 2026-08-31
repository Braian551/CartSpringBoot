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

## 3. Iniciar MySQL

Desde la raíz del proyecto:

~~~powershell
docker compose up -d
~~~

El comando crea o inicia el servicio mysql en segundo plano. Comprueba el estado:

~~~powershell
docker compose ps
~~~

Deberías ver un contenedor del servicio mysql en estado Up y el puerto publicado 3309 hacia el puerto 3306 del contenedor. Si acaba de arrancar y todavía no acepta conexiones, espera unos segundos y revisa:

~~~powershell
docker compose logs --tail 100 mysql
~~~

## 4. Iniciar Spring Boot

En otra ventana de PowerShell, vuelve a la raíz del proyecto y ejecuta el wrapper de Maven para Windows:

~~~powershell
.\mvnw.cmd spring-boot:run
~~~

La aplicación lee la configuración de src/main/resources/application.properties, escucha en el puerto 8081 y conecta con MySQL publicado en localhost:3309.

## 5. Abrir la aplicación

Abre:

~~~text
http://localhost:8081/cart
~~~

La ruta http://localhost:8081/cart/ también existe y redirige a /cart.

## 6. Detener el entorno

Para detener Spring, pulsa Ctrl+C en la ventana donde se está ejecutando.

Para detener el contenedor MySQL sin eliminar sus datos:

~~~powershell
docker compose down
~~~

Para iniciarlo de nuevo:

~~~powershell
docker compose up -d
~~~

> **Advertencia:** no ejecutes docker compose down -v si deseas conservar los datos de MySQL. La opción -v elimina el volumen mysql_data y puede destruir los registros persistidos.

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

