# GanHard — proyecto Android

Java y XML/Android Views, con organización MVC introductoria. El proyecto creado inicialmente con Compose se adaptó al enfoque acordado para el curso. Los archivos originales se respaldaron en `../tmp/respaldo_android_inicial`.

## Cómo están organizadas las carpetas

```text
app/src/main/
├── java/com/example/ganhard/
│   ├── GanHardApplication.java                 Base, ejecutor y repositorios compartidos; tema inicial
│   ├── controlador/                            Controladores (Activity) y apoyos de la Vista
│   │   ├── BaseActivity.java                   Barras del sistema, flecha de regreso y selector de fechas
│   │   ├── InicioActivity.java                 Inventario real; el icono ☰ abre el Menú
│   │   ├── MenuActivity.java                   Módulos; los que faltan aparecen como "Próximamente"
│   │   ├── AjustesActivity.java                Configuración: perfil, datos de la finca y apariencia
│   │   ├── ConfiguracionActivity.java          Formulario del perfil local y la finca
│   │   ├── AnimalesActivity.java (+ AnimalAdapter)        Lista con búsqueda y filtro Activos/Vendidos/Fallecidos
│   │   ├── AnimalFormActivity.java             Registro y edición de animales (con estado en la edición)
│   │   ├── DetalleAnimalActivity.java          Ficha, peso registrado y accesos
│   │   ├── PesajesActivity.java (+ PesajeAdapter)         Historial de peso
│   │   ├── PesajeFormActivity.java             Registro de un pesaje
│   │   └── FilaNavegacion, FormatoFecha, MascaraFecha, MensajesError, Presentacion   Apoyos de la Vista
│   └── modelo/
│       ├── nucleo/    EjecutorModelo (un solo hilo), Callback<T>, ErrorModelo (errores tipados), Tiempo
│       ├── entidad/   Usuario, Finca, Raza, Animal, Pesaje
│       ├── datos/     GanHardDatabase (v2), DAO, CatalogoRazas, Migraciones
│       ├── repositorio/  ConfiguracionRepository, AnimalRepository, PesajeRepository
│       ├── reglas/    Validadores en Java puro; devuelven códigos de error, no textos
│       └── PreferenciaTema.java, ConfiguracionLocal.java
├── res/
│   ├── layout/        Una vista XML por pantalla, más filas de lista
│   ├── values/        ganhard.xml (textos), themes.xml (estilos del Figma), plurals.xml, dimens.xml, attrs.xml
│   └── values-night/  Colores oscuros
└── AndroidManifest.xml
app/schemas/           Esquemas de Room exportados (1.json, 2.json); sirven para probar migraciones
```

**Modelo:** entidades, reglas, repositorio, DAO y base Room. **Vista:** archivos XML. **Controlador:** clases de `controlador`, que en Android heredan de Activity. DAO y repositorios son apoyos del Modelo; no son un cuarto componente de MVC.

Ejemplo: pulsar Guardar en `activity_animal_form.xml` → `AnimalFormActivity` lee los campos, convierte las fechas de `dd/mm/aaaa` a ISO y valida → `AnimalRepository.registrar` valida otra vez contra la finca, genera el UUID y pide a `AnimalDao` insertar → Room escribe en SQLite → el resultado vuelve al hilo principal como `Callback<Animal>` y la pantalla confirma o muestra el error junto al campo. Los controladores no contienen consultas SQL ni textos de error escritos en código: el Modelo devuelve códigos y `MensajesError` los traduce con `strings.xml`.

`Activity` es el nombre técnico de la clase Android que implementa un controlador. Los archivos `build.gradle.kts` usan Kotlin DSL para configurar Gradle; el código de las pantallas de la app está en Java.

## Qué hace hoy

**Etapa 1:** perfil local del administrador y finca (RF01, RF02) guardados en Room, edición conservando UUID, tema claro/oscuro/del sistema (RF18).

**Etapa 2:**

- **Registro de animales** (RF03): código, nombre, sexo y raza (listas desplegables), fechas de nacimiento e ingreso (se escriben solo con dígitos y la app pone las barras, o se elige con el calendario) y observaciones.
- **Consulta** (RF04): lista con búsqueda por código o nombre (ignora mayúsculas y tildes) y filtro por estado (Activos al abrir); contador "N activos en la finca"; tarjetas con raza y último pesaje.
- **Ficha** (RF05), **edición** (RF06) y **cambio de estado** activo/vendido/fallecido con su fecha (RF07), desde "Editar datos y estado".
- **Pesajes** (RF08, RF09): registro con fecha, hora y peso; historial del más reciente (VIGENTE) al más antiguo; peso registrado en la ficha.
- **Inicio** (RF10): cantidad real de animales activos y botón "Registrar animal"; el icono ☰ abre el **Menú** con todos los módulos del diseño. Sanidad, Tareas, Gastos, Historial de cambios y Sincronización aparecen deshabilitados como "Próximamente".
- **Configuración**: perfil, datos de la finca y apariencia (Claro, Oscuro, Seguir el sistema).

**Fidelidad con el Figma:** las pantallas siguen las plantillas de `../avance_ganhard/imagenes_figma/` (tarjetas blancas sin borde, etiquetas verdes en mayúsculas, botones píldora, filas con flecha ›, y el tema oscuro en menta). No se muestran datos que dependen de módulos aún no construidos (pendientes, cambios por enviar, "Registrado por", motivo de los cambios): aparecen deshabilitados o se omiten.

Reglas aplicadas: el código del animal es único por finca y no distingue mayúsculas ni espacios en los extremos; el peso es positivo, con hasta 2 decimales y coma o punto, de 2000 kg como máximo (un punto de miles como "1.050" se rechaza); los hechos no admiten fechas futuras; la hora es obligatoria salvo en un antecedente anterior al ingreso; el ingreso no puede ser anterior al nacimiento.

El perfil local no es una cuenta autenticada: no hay contraseñas ni envío de datos a servicios externos, y la app excluye base y preferencias de los respaldos.

## Base de datos

`ganhard.db`, **versión 2**, con cinco tablas: `usuario`, `finca`, `raza`, `animal` y `pesaje`. Se conservan los nombres de columnas y UUID del diccionario de datos. Los instantes se guardan en UTC como texto, las fechas como `AAAA-MM-DD` (aunque en pantalla se vean `dd/mm/aaaa`), la hora como `HH:MM:SS` y el peso como número; los tipos del diccionario son tipos lógicos.

La migración `Migraciones.V1_V2` crea las tablas nuevas y siembra las 10 razas del catálogo (la misma lista se siembra al crear una base nueva). **No se configura borrado automático de la base al cambiar su versión.** El contador `version` de cada registro queda en 0 hasta la integración remota; no debe confundirse con la versión del esquema Room. La columna `id_tarea` de `pesaje` se agregará con las tareas.

## Etapas siguientes

| Etapa | Resultado |
|---|---|
| 3 | Correcciones, anulaciones e historial básico de modificaciones. |
| 4 | Sanidad, tareas y gastos, incorporados por módulos. |
| 5 | Cuenta Supabase, integrantes y permisos. |
| 6 | Sincronización y resolución de conflictos entre dispositivos. |

## Abrir y ejecutar

1. Abre esta carpeta `GanHard` en Android Studio y espera a que termine la sincronización de Gradle. Usa el JDK incorporado en Android Studio.
2. En **Tools → Device Manager** inicia el teléfono virtual (API 24 es la versión mínima) y pulsa **Run** sobre `app`.
3. Recorrido de la demostración: configura la finca → **Registrar animal** `GH-001` → ☰ **Menú → Animales** y busca `GH-001` → abre su ficha → **Registrar pesaje** → vuelve a la ficha y comprueba el peso registrado → cierra y abre la app sin conexión y verifica que el animal y el pesaje continúan.
4. Casos útiles: un código repetido (`gh-001`), un peso `0` o con tres decimales, una fecha futura, el filtro Vendidos, editar el animal y marcarlo como vendido (el inventario de Inicio baja), y cambiar el tema desde Configuración.

## Verificación

Desde la terminal de Android Studio, dentro de esta carpeta:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
```

Con el emulador encendido:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

**Ojo:** esa tarea instala la app de prueba y la desinstala al terminar, lo que borra los datos que hayas escrito a mano en ese emulador.

Las pruebas locales usan JUnit y Robolectric (Android simulado en el equipo, con la base Room real en memoria): validadores, repositorios, esquema y migración, y cada pantalla (casos válidos e inválidos, rotación, errores de base de datos). Las pruebas instrumentadas (`ConfiguracionPersistenciaTest` y `MigracionInstrumentadaTest`) se ejecutan en un emulador.

**Resultado del 12 de octubre de 2026:** compilación y APK de prueba completos; **250 pruebas locales aprobadas, 0 fallos**; lint con 0 errores y 21 advertencias (dependencias por actualizar, fondos superpuestos y `notifyDataSetChanged` en listas cortas); **3 pruebas instrumentadas aprobadas en un emulador** (AVD Medium_Phone, Android 17 / API 37, x86_64, 1080×2400, orientación vertical), incluida la migración 1→2 con el ayudante oficial de Room. Las pantallas se revisaron visualmente en ese emulador, en tema claro y oscuro, comparándolas con el Figma; las capturas están en `../avance_ganhard/capturas_emulador/`.

APK generado: `app/build/outputs/apk/debug/app-debug.apk`.

Referencias técnicas utilizadas: [Room para proyectos Java](https://developer.android.com/jetpack/androidx/releases/room) y [pruebas locales con Robolectric](https://robolectric.org/getting-started/).
