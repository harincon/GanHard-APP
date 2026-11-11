# GanHard

App Android para llevar el inventario de ganado de una finca: animales, pesajes y, más adelante, sanidad, tareas y gastos. Funciona sin conexión; los datos se guardan en el teléfono.

Java y XML (Android Views), arquitectura MVC, base de datos local con Room.

## Qué hace hoy

- Perfil del administrador y datos de la finca.
- Registro, búsqueda, ficha y edición de animales, con estado activo, vendido o fallecido.
- Registro e historial de pesajes por animal.
- Inicio con el total de animales activos.
- Tema claro, oscuro o el del sistema.

Sanidad, Tareas, Gastos, Historial de cambios y Sincronización aparecen en el menú como "Próximamente".

## Estructura

```text
app/src/main/java/com/example/ganhard/
├── controlador/   Pantallas (Activity) y apoyos de la vista
└── modelo/
    ├── entidad/      Usuario, Finca, Raza, Animal, Pesaje
    ├── datos/        Base Room, DAO y migraciones
    ├── repositorio/  Operaciones de guardado y consulta
    └── reglas/       Validaciones
app/src/main/res/layout/   Una vista XML por pantalla
```

## Ejecutar

1. Abrir la carpeta en Android Studio y esperar la sincronización de Gradle.
2. Iniciar un emulador (Android 7.0 / API 24 o superior) y pulsar **Run**.
