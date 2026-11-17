# Servidor de cuentas (Supabase)

La cuenta es **opcional**: sin ella GanHard funciona igual, con los datos del teléfono. Con ella se puede vincular la
finca e invitar a un encargado. Esta guía crea el servidor gratuito que usa la app. Se hace una sola vez.

## 1. Crear el proyecto

1. Entra a <https://supabase.com> y crea una cuenta (puedes usar GitHub).
2. **New project** → nombre `ganhard`, región cercana a ti, plan **Free**.
3. Elige una contraseña para la base de datos y guárdala (la app no la usa).
4. Espera a que termine de crearse (1–2 minutos).

## 2. Instalar las tablas y funciones

1. En el proyecto: **SQL Editor** → **New query**.
2. Abre [`esquema.sql`](esquema.sql), copia **todo** su contenido y pégalo.
3. Pulsa **Run**. Debe responder `Success. No rows returned`. Puedes volver a ejecutarlo sin problema.

Las tablas quedan cerradas: la app no puede leerlas ni escribirlas directamente. Solo puede llamar a las funciones del
archivo, y cada una comprueba quién es la cuenta y qué le toca hacer (por ejemplo, solo el dueño invita o retira).

## 3. Ajustar el inicio de sesión

En **Authentication → Sign In / Providers → Email**:

- **Confirm email**: déjalo **activado**. Las invitaciones solo se pueden aceptar con el correo confirmado; si lo
  desactivas, cualquiera podría registrarse con el correo de otra persona.
- **Minimum password length**: pon `8` (es el mínimo que pide la app).

En **Authentication → URL Configuration**, el enlace del correo de confirmación lleva a *Site URL*. La app no tiene
página web, así que al pulsar el enlace puede verse una página vacía o un error del navegador: **no importa**, la
cuenta ya queda confirmada y se puede iniciar sesión.

## 4. Conectar la app

1. **Project Settings → API** (o **API Keys**). Copia:
   - **Project URL** (`https://xxxx.supabase.co`)
   - la clave **anon** (también llamada *publishable*).
2. Abre el archivo `local.properties` de la carpeta del proyecto (el que ya tiene `sdk.dir`) y agrega:

   ```properties
   supabase.url=https://xxxx.supabase.co
   supabase.anon.key=eyJ...tu-clave-anon...
   ```

3. Compila de nuevo (**Build → Rebuild Project**) y ejecuta la app.

`local.properties` no se sube a GitHub. La clave **anon** está pensada para ir dentro de la app (por eso las tablas
están cerradas); **nunca** pongas la clave `service_role` en la app.

Si no pones estos datos, la app compila y funciona normalmente, pero la pantalla **Ajustes → Cuenta** dirá que las
cuentas no están disponibles.

## 5. Probar

Necesitas dos correos reales (por ejemplo, el tuyo y otro).

1. **Ajustes → Cuenta → Crear cuenta** con el primer correo. Revisa tu bandeja (y spam), confirma el correo.
2. **Iniciar sesión** → **Vincular mi finca a mi cuenta**.
3. **Integrantes e invitaciones → Invitar a un encargado** con el segundo correo.
4. En otro teléfono o emulador (o cerrando sesión), crea la segunda cuenta, confírmala e inicia sesión.
5. **Invitaciones recibidas** → **Aceptar**.
6. Con la primera cuenta, **Integrantes** → **Retirar acceso** (pide motivo). La persona queda en la lista como
   *Acceso retirado*, con fecha y motivo.

## Cosas que conviene saber

- **Límite de correos**: el servicio gratuito envía muy pocos correos de confirmación por hora. Si al registrarte la app
  dice *Demasiados intentos*, espera un rato. Para uso real se configura un servidor de correo propio en
  **Authentication → SMTP Settings**.
- **Una finca por cuenta** en esta etapa. Una invitación vale **7 días**.
- **Cerrar sesión** solo cierra la sesión en el teléfono: no borra datos locales ni el vínculo de la finca.
- Si la app dice que *el servidor aún no tiene instalado el esquema*, falta ejecutar `esquema.sql` (paso 2).
- Para empezar de cero, borra las cuentas en **Authentication → Users** y las tablas `invitacion_finca`,
  `usuario_finca`, `finca` y `perfil` en **Table Editor**, y vuelve a ejecutar `esquema.sql`.
- Esta etapa no sincroniza animales, pesajes ni demás registros: eso llega en la siguiente.
