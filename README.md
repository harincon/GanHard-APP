# GanHard

App Android para llevar el inventario de ganado de una finca: animales, pesajes, sanidad, gastos y tareas. Funciona sin conexión; los datos se guardan en el teléfono.

Java y XML (Android Views), arquitectura MVC, base de datos local con Room.

## Qué hace hoy

- Perfil del administrador y datos de la finca.
- Registro, búsqueda, ficha y edición de animales, con estado activo, vendido o fallecido. El código se asigna solo (GH-001, GH-002…) y cada animal lleva su hierro, y si quieres aretes y otros números.
- Catálogo de hierros de la finca (crear, editar, desactivar).
- Registro e historial de pesajes por animal, con corrección y anulación con motivo.
- Sanidad: actividades realizadas por animal (vacunas, desparasitaciones, revisiones, tratamientos), con corrección y anulación. Se pueden aplicar a todos los animales activos de una vez, y corregir o anular todo el grupo.
- Gastos: egresos de la finca por mes con su total, con corrección y anulación.
- Tareas: actividades programadas, generales o de un animal; al realizar un pesaje o una actividad sanitaria programados, la tarea queda completada y enlazada. El tipo se elige o se escribe (un tipo propio, como «Herrar», funciona como una tarea general), y se pueden programar para todos los animales activos de una vez: la lista las junta en una tarjeta, se cancelan juntas y una de sanidad se cumple para todos con una sola actividad.
- Historial de cambios de la finca y de cada animal: quién, cuándo, qué cambió y por qué.
- Inicio con estadísticas mes por mes: animales (activos, hembras y machos, razas, ingresos, ventas y fallecimientos), pesos (promedio y animales sin pesar) y gastos (total y categorías), con barras simples.
- Cuenta opcional: crear la cuenta o iniciar sesión (el correo se confirma con un código), vincular la finca, invitar a un encargado por correo y retirar accesos con motivo.
- «Cerrar sesión y borrar datos» en el menú: cierra la sesión y deja el teléfono como recién instalado, previa advertencia.
- Tema claro, oscuro o el del sistema.

Sincronización aparece en el menú como "Próximamente".

## Estructura

```text
app/src/main/java/com/example/ganhard/
├── controlador/   Pantallas (Activity) y apoyos de la vista
└── modelo/
    ├── entidad/      Usuario, Finca, Raza, Animal, Pesaje, EventoSanitario, Gasto, Tarea, HistorialCambio
    ├── datos/        Base Room, DAO y migraciones
    ├── repositorio/  Operaciones de guardado y consulta
    ├── remoto/       Cliente de Supabase (cuentas, invitaciones)
    └── reglas/       Validaciones
app/src/main/res/layout/   Una vista XML por pantalla
```

## Ejecutar

1. Abrir la carpeta en Android Studio y esperar la sincronización de Gradle.
2. Iniciar un emulador (Android 7.0 / API 24 o superior) y pulsar **Run**.

## Cuenta (opcional)

Sin cuenta la app funciona igual. Para activar las cuentas, crea un proyecto gratuito en Supabase siguiendo [`supabase/LEEME.md`](supabase/LEEME.md) y agrega `supabase.url` y `supabase.anon.key` a `local.properties`.
