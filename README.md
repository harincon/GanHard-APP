# GanHard — proyecto Android

Java y XML/Android Views, con organización MVC introductoria. El proyecto creado inicialmente con Compose se adaptó al enfoque acordado para el curso. Los archivos originales se respaldaron en `../tmp/respaldo_android_inicial`.

## Cómo están organizadas las carpetas

```text
app/src/main/
├── java/com/example/ganhard/
│   ├── GanHardApplication.java                 Repositorio compartido y tema inicial
│   ├── controlador/
│   │   ├── BaseActivity.java                   Márgenes de las barras Android y acceso al Modelo
│   │   ├── InicioActivity.java                 Solicita datos y actualiza Inicio
│   │   └── ConfiguracionActivity.java          Coordina el formulario y su guardado
│   └── modelo/
│       ├── ConfiguracionLocal.java             Perfil y finca recuperados juntos
│       ├── entidad/
│       │   ├── Usuario.java                    Tabla usuario
│       │   └── Finca.java                      Tabla finca
│       ├── datos/
│       │   ├── ConfiguracionDao.java           Consultas y transacción de guardado
│       │   └── GanHardDatabase.java            Base Room y versión del esquema
│       ├── repositorio/
│       │   └── ConfiguracionRepository.java    Carga/guardado fuera del hilo de pantalla
│       └── reglas/
│           ├── EntradaConfiguracion.java       Datos escritos en el formulario
│           └── ValidadorConfiguracion.java     Obligatorios, correo, área y longitudes
├── res/
│   ├── layout/
│   │   ├── activity_configuracion.xml          Vista del formulario
│   │   └── activity_inicio.xml                 Vista del inicio
│   ├── values/ganhard.xml                      Textos y colores claros
│   ├── values/themes.xml                       Tema Material de la aplicación
│   └── values-night/ganhard.xml                Colores oscuros
└── AndroidManifest.xml                        Pantallas y punto de entrada
```

**Modelo:** entidades, reglas, repositorio, DAO y base Room. **Vista:** archivos XML. **Controlador:** clases de `controlador`, que en Android heredan de Activity. DAO y repositorios son apoyos del Modelo; no son un cuarto componente de MVC.

Ejemplo: pulsar Guardar en la Vista → `ConfiguracionActivity` recoge y valida entradas → `ConfiguracionRepository` solicita la transacción al DAO → Room escribe en SQLite → el callback permite mostrar Inicio. Los controladores no contienen consultas SQL.

`Activity` es el nombre técnico de la clase Android que implementa un controlador. Los archivos `build.gradle.kts` usan Kotlin DSL para configurar Gradle; el código de las pantallas de la app está en Java.

## Etapa 1

- Formulario de nombre y correo del administrador, nombre de finca, ubicación y área opcional.
- Validación de obligatorios y correo; área positiva con hasta dos decimales. Acepta coma o punto, sin separadores de miles.
- Guardado conjunto de perfil y finca en Room; edición conservando sus UUID y fecha de creación.
- Inicio que muestra los datos guardados y permite volver al formulario.
- Tema claro, oscuro o del sistema, con preferencia persistente.
- Formulario desplazable, conservación de entradas al rotar y confirmación antes de descartar cambios.
- Consultas fuera del hilo principal; estados de carga y errores con opción de reintento.

El perfil local todavía no es una cuenta autenticada. Esta etapa no envía datos a servicios externos ni registra contraseñas. Las invitaciones, roles compartidos e historial de modificaciones se desarrollarán en las etapas correspondientes.

La base `ganhard.db` comienza con **usuario y finca**, versión 1. Se conservan los nombres de columnas y UUID del modelo general. SQLite almacena los instantes UTC como texto y el área como número; los tipos del diccionario son tipos lógicos. Las demás tablas se incorporarán mediante migraciones que preserven los datos. No se configura borrado automático de la base al cambiar su versión. El contador `version` de cada registro queda en 0 hasta implementar la integración remota; no debe confundirse con la versión 1 del esquema Room.

## Etapas siguientes

| Etapa | Resultado |
|---|---|
| 2 | Registro, consulta y ficha de animales; catálogo de razas y migración de Room. |
| 3 | Registro e historial de pesajes, últimos pesos y resumen real del inventario. |
| 4 | Correcciones, anulaciones e historial básico de modificaciones. |
| 5 | Sanidad, tareas y gastos, incorporados por módulos. |
| 6 | Cuenta Supabase, integrantes y permisos. |
| 7 | Sincronización y resolución de conflictos entre dispositivos. |

El primer avance requerido por el docente se completará cuando funcionen registro, consulta y ficha de animales con sus evidencias. Esta etapa inicial tiene dos interfaces y prepara ese recorrido.

## Abrir y ejecutar

1. Abre esta carpeta `GanHard` en Android Studio y espera a que termine la sincronización de Gradle. Usa el JDK incorporado en Android Studio.
2. Para ver la estructura completa, cambia el panel izquierdo a **Project**. Las vistas se abren desde `app/src/main/res/layout` y permiten ver Code, Split o Design.
3. En **Tools → Device Manager**, crea un teléfono virtual e instala su imagen de Android si aún no tienes uno. API 24 es la versión mínima; puedes probar con una imagen más reciente.
4. Selecciona el dispositivo y pulsa **Run** sobre `app`.
5. Completa el formulario, guarda y revisa Inicio. Usa tus datos de prueba; no hay registros de ejemplo precargados.
6. Vuelve a editar, cambia el nombre de la finca y verifica que se actualiza. Cierra y abre la app y comprueba que conserva la configuración.
7. Prueba un correo incorrecto, el área vacía y un área con decimales; comprueba que los errores conservan los otros datos escritos.
8. Cambia el tema y verifica que la preferencia continúa después de volver a abrir la aplicación.

## Verificación

Desde la terminal de Android Studio, dentro de esta carpeta:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
```

Con un emulador o dispositivo conectado:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Las pruebas locales cubren campos obligatorios, correo, límites y área. Una prueba con Robolectric comprueba el formulario durante rotación antes de la carga, después de editar y durante el guardado; también verifica conservación de UUID y ausencia de navegación desde la pantalla destruida. Las instrumentadas comprueban persistencia al cerrar/reabrir el archivo Room, edición sin cambiar identificadores y rechazo de una finca sin propietario con reversión de toda la transacción.

**Resultado del 12 de octubre de 2026:** compilación de la app y APK de pruebas completada; 6 pruebas locales aprobadas; lint sin errores, con 22 advertencias principalmente sobre actualización de dependencias, recursos y convenciones. No se ejecutaron las pruebas instrumentadas ni se verificó visualmente en Android: `adb devices` no encontró dispositivos conectados y no hay un AVD configurado. La prueba de Robolectric simula Android en el equipo y no sustituye las capturas ni la ejecución en el emulador exigidas por el curso.

APK generado: `app/build/outputs/apk/debug/app-debug.apk`.

Referencias técnicas utilizadas: [Room para proyectos Java](https://developer.android.com/jetpack/androidx/releases/room) y [pruebas locales con Robolectric](https://robolectric.org/getting-started/).
