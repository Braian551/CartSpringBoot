# 02 — Manual de uso

Este documento explica la interfaz web como la usaría una persona que entra por primera vez al sistema.

## Abrir la aplicación

1. Asegúrate de que MySQL esté activo con docker compose ps.
2. Asegúrate de que Spring Boot esté ejecutándose.
3. Abre [http://localhost:8081/cart](http://localhost:8081/cart).

La pantalla principal se titula **Carritos de compra**. Muestra el panel de administración y el botón **Crear carrito**. Si todavía no hay registros, aparece el mensaje **No hay carritos registrados**.

## Ver el listado

Cada fila muestra:

- **ID**: identificador generado por MySQL.
- **ID de usuario**: puede aparecer como Sin usuario.
- **ID de sesión**: puede aparecer como Sin sesión.
- **Creado** y **Actualizado**: fechas automáticas.
- **Acciones**: **Ver**, **Editar** y **Eliminar**.

El listado se ordena por ID ascendente. La primera página se presenta como **Página 1** aunque internamente el parámetro page comienza en 0.

## Crear un carrito

1. Pulsa **Crear carrito** o **Nuevo carrito**.
2. En **ID de usuario**, escribe el identificador del usuario si el carrito está asociado a una cuenta. Es opcional.
3. En **ID de sesión**, escribe el identificador de la sesión si se trata de un visitante o una sesión anónima. También es opcional.
4. Pulsa **Guardar carrito**.

El servidor valida los datos antes de guardarlos. userId admite como máximo 50 caracteres y sessionId como máximo 255. Si una validación falla, la página conserva el formulario y muestra el mensaje debajo del campo correspondiente.

Al guardar correctamente, la aplicación vuelve al listado y el nuevo carrito aparece con un ID generado y sus fechas automáticas.

## Consultar el detalle

En el listado, pulsa **Ver**. La página **Detalle del carrito** muestra el ID, los identificadores y las fechas **Creado** y **Última actualización**. Los valores opcionales se presentan como **Sin usuario asociado** o **Sin sesión asociada** cuando están vacíos.

Pulsa **Volver** para regresar al listado o **Editar** para cambiar los identificadores permitidos.

## Editar un carrito

1. En el listado pulsa **Editar**, o abre primero el detalle y luego pulsa **Editar**.
2. Cambia **ID de usuario** o **ID de sesión**.
3. Pulsa **Guardar cambios**.

La aplicación solo permite actualizar esos dos campos. El ID y las fechas no se pueden modificar desde este formulario. Después de guardar, se muestra el detalle del carrito actualizado.

## Eliminar un carrito

1. En la fila correspondiente, pulsa **Eliminar**.
2. Confirma el cuadro **¿Seguro que deseas eliminar este carrito?**.

La eliminación se envía mediante un formulario POST protegido con CSRF. Si confirmas, el registro se elimina y vuelves al listado. No uses un enlace GET para eliminar: consultar una URL no debería ejecutar una operación destructiva.

## Navegar entre páginas

Cuando hay más registros que caben en una página, aparece la navegación **Anterior** y **Siguiente**. También puedes consultar una página directamente, por ejemplo:

~~~text
http://localhost:8081/cart?page=1&size=20
~~~

El tamaño predeterminado es 20. El tamaño solicitado debe estar entre 1 y 100, y page debe estar entre 0 y 10.000. Valores fuera de esos límites producen una solicitud inválida (400).

## Mensajes de error que puede ver el usuario

| Código | Significado en la interfaz | Qué hacer |
| ---: | --- | --- |
| **400** | **Solicitud inválida**: datos demasiado largos, ID no válido o parámetros de página fuera de rango. | Corrige los datos y vuelve a enviar. |
| **403** | La solicitud protegida no tiene un token CSRF válido. | Usa los formularios de la aplicación y recarga la página si el formulario quedó abierto mucho tiempo. |
| **404** | **Carrito no encontrado**. | Vuelve al listado; el ID puede no existir o haber sido eliminado. |
| **429** | Se superó temporalmente el límite de solicitudes. | Espera el tiempo indicado por Retry-After y vuelve a intentarlo. |
| **500** | **No se pudo completar la solicitud** por un error inesperado. | Inténtalo más tarde y revisa los logs si administras el entorno. |

La página 500 no muestra el stack trace ni detalles internos del servidor.

<!-- Captura sugerida: listado de carritos con las acciones Ver, Editar y Eliminar. -->

