# 10 — Troubleshooting

Esta guía propone comprobaciones de lectura y diagnóstico. No mates procesos automáticamente ni elimines volúmenes para resolver un error sin identificar antes la causa.

## Spring no inicia porque el puerto está ocupado

Comprueba quién utiliza el puerto 8081:

~~~powershell
netstat -ano | findstr :8081
~~~

La última columna es el PID del proceso. Identifica el proceso con las herramientas del sistema y decide de forma consciente si debes detenerlo. No finalices procesos automáticamente desde esta guía.

Si necesitas ejecutar temporalmente la aplicación en otro puerto, esa decisión cambia la configuración de ejecución; recuerda que las instrucciones y la URL documentadas usan 8081.

Con el Compose puedes conservar el puerto interno 8081 y cambiar solo el puerto del host:

~~~powershell
$env:CART_API_PORT = "8082"
docker compose up -d --build
~~~

En ese caso, configura también `VITE_CART_ADMIN_API_URL=http://localhost:8082/api` en el frontend.

## PostgreSQL compartido no inicia porque el puerto está ocupado

Comprueba el puerto publicado:

~~~powershell
netstat -ano | findstr :5435
~~~

El host debe poder publicar 5435 para dirigirlo al 5432 de `cart-db`. Revisa qué proceso usa el puerto antes de cambiar el Compose de Angelow. Si modificas el puerto del host, también debes actualizar `CART_DB_PORT` y las instrucciones de conexión.

## PostgreSQL compartido no aparece activo

~~~powershell
docker compose ps
docker compose logs --tail 100 cart-db
~~~

Revisa:

- Docker Desktop está iniciado.
- El servicio `cart-db` aparece en estado Up.
- El contenedor usa la imagen PostgreSQL declarada en el Compose de Angelow.
- No hay errores de inicialización en los logs.
- PostgreSQL ha terminado su arranque antes de iniciar la API Java.

Compose no define un healthcheck; Up significa que el contenedor está ejecutándose, no necesariamente que MySQL ya acepte conexiones. Espera unos segundos y vuelve a consultar.

## Spring no puede conectarse a PostgreSQL

Comprueba en este orden:

1. Docker Engine está disponible.
2. El servicio `cart-db` está Up.
3. El puerto publicado sigue siendo 5435.
4. La base de datos configurada es `angelow_cart`.
5. El usuario configurado en Spring coincide con el de Compose.
6. La credencial configurada en Spring coincide con la de Compose.
7. El contenedor no muestra errores en `docker compose logs cart-db` desde Angelow.

No pegues credenciales en issues, capturas ni documentación. La conexión local apunta a `localhost:5435`; dentro del contenedor usa `host.docker.internal:5435`.

## Error 400 — Solicitud inválida

Causas previstas:

- ID negativo o igual a cero.
- ID no numérico.
- page menor que 0 o mayor que 10.000.
- size menor que 1 o mayor que 100.
- userId de más de 50 caracteres.
- sessionId de más de 255 caracteres.
- demasiados parámetros o un formulario mayor que los límites Tomcat.
- error de binding o parámetro faltante.

La interfaz muestra la página Solicitud inválida y permite volver a carritos. Corrige los datos en lugar de repetir la solicitud sin cambios.

## Error 403 — CSRF

El proyecto permite lectura sin autenticación, pero exige CSRF para POST y otras operaciones que cambian estado. Si envías un formulario manual sin el token o usas una página antigua, Spring Security puede devolver 403.

Solución:

1. Abre de nuevo /cart/new o el formulario de edición.
2. Envía el formulario desde la interfaz, que incluye el token oculto.
3. No desactives CSRF para ocultar el problema.
4. Si persiste, revisa que la solicitud llegue al mismo host y que el navegador no haya bloqueado sus cookies.

## Error 404 — Carrito no encontrado

La ruta tiene formato válido, pero el ID no corresponde a un registro existente. Vuelve a /cart y utiliza un ID visible en el listado. El mensaje de la aplicación es Carrito no encontrado; no muestra la excepción interna.

## Error 429 — Too Many Requests

El rate limiter agotó el límite para la combinación de dirección y tipo de operación. Lee el header Retry-After, espera el número de segundos indicado y realiza una nueva solicitud normal.

Los límites actuales son 120 GET/HEAD por minuto, 30 POST de creación/actualización y 20 POST de eliminación o DELETE. No intentes evadir el límite; si el volumen es legítimo, revisa el diseño del cliente o la configuración operativa.

## Error 500 — Solicitud no completada

La página 500 oculta los detalles técnicos al usuario. Para diagnosticar:

~~~powershell
Get-Content .\logs\cart.log -Tail 100
docker compose logs --tail 100 mysql
docker compose ps
~~~

Busca mensajes ERROR como Failed to list carts, Failed to create cart o Unexpected error while processing cart request. El stack trace debe permanecer en el destino del servidor y no copiarse a respuestas públicas.

## Los logs parecen no existir

Spring escribe en logs/cart.log relativo al directorio desde el que se inicia. Comprueba que ejecutaste spring-boot:run desde la raíz del proyecto y revisa la consola.

~~~powershell
Get-ChildItem .\logs
Get-Content .\logs\cart.log -Tail 100
~~~

Los logs Docker se consultan por separado:

~~~powershell
docker compose logs mysql
~~~

## Los datos parecen haber desaparecido

Comprueba el volumen y el servicio sin borrar nada:

~~~powershell
docker volume ls
docker compose ps
~~~

docker compose restart mysql conserva el volumen. docker compose down elimina contenedor y red, pero conserva el volumen nombrado; después docker compose up -d puede reutilizarlo.

> **Advertencia:** docker compose down -v elimina los volúmenes asociados. En este proyecto puede destruir mysql_data y los registros persistidos. No ejecutes ese comando para hacer troubleshooting si deseas conservar los datos.

## Validar Compose sin levantar ni destruir

~~~powershell
docker compose config --quiet
~~~

Si termina sin error, la sintaxis y la configuración de Compose son válidas. Para revisar el estado actual sin modificarlo:

~~~powershell
docker compose ps
~~~

## Diagnóstico recomendado

~~~text
1. Leer el mensaje de la interfaz o la salida de Maven.
2. Revisar logs/cart.log y la consola de Spring.
3. Revisar docker compose logs mysql.
4. Revisar docker compose ps.
5. Confirmar puertos, base de datos y configuración sin exponer credenciales.
6. Reproducir con una prueba o comando de lectura.
~~~
