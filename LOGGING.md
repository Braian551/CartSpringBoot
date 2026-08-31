# Logging y persistencia

## Spring Boot

La aplicación se ejecuta actualmente fuera de Docker. Spring Boot conserva los
logs en consola y los escribe en `logs/cart.log`, relativo al directorio desde
el que se inicia la aplicación. Logback rota el archivo al alcanzar 10 MB,
comprime los históricos, conserva hasta 7 archivos y aplica un límite total de
100 MB para los archivos archivados. El archivo activo puede ocupar hasta otros
10 MB, por lo que el uso máximo aproximado es de 110 MB más metadatos del
filesystem.

Los logs generados en runtime están excluidos de Git mediante `.gitignore`.
Si Spring Boot se containeriza posteriormente, montar un directorio del host
en `/app/logs`; así los logs no dependerán del filesystem efímero del
contenedor. Este Compose todavía administra solamente MySQL.

## MySQL

`mysql_data` persiste `/var/lib/mysql` y está separado de los logs. La salida
stdout/stderr del contenedor usa el driver `local` de Docker con rotación de
10 MB y un máximo de 3 archivos.

Comandos útiles:

```text
docker compose logs mysql
docker compose logs -f mysql
```

Reiniciar Spring conserva `logs/cart.log` y sus históricos. Reiniciar Docker
conserva el volumen `mysql_data` y los logs del contenedor. Si el contenedor
MySQL se elimina y recrea, los datos permanecen en `mysql_data`; los logs Docker
asociados al contenedor eliminado no se recuperan y el nuevo contenedor inicia
un historial nuevo sujeto a la misma rotación.
