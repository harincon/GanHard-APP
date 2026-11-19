package com.example.ganhard.modelo.remoto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Cliente de Supabase sin librerías: el servicio de cuentas (/auth/v1) y las funciones de la base (/rest/v1/rpc).
 * Toda regla de negocio vive en las funciones del servidor (supabase/esquema.sql); aquí solo se envían los datos y se
 * traduce lo que responde a un {@link ErrorRemoto}.
 */
public final class SupabaseServicio implements ServicioRemoto {
    public static final int TIEMPO_POR_DEFECTO_MS = 15_000;

    private static final class Respuesta {
        final int estado;
        final String cuerpo;
        Respuesta(int estado, String cuerpo) { this.estado = estado; this.cuerpo = cuerpo; }
        boolean exitosa() { return estado >= 200 && estado < 300; }
    }

    private final String url, clave;
    private final int tiempoMs;

    public SupabaseServicio(String url, String claveAnonima) { this(url, claveAnonima, TIEMPO_POR_DEFECTO_MS); }

    public SupabaseServicio(String url, String claveAnonima, int tiempoMs) {
        String limpia = url == null ? "" : url.trim();
        while (limpia.endsWith("/")) limpia = limpia.substring(0, limpia.length() - 1);
        this.url = limpia;
        this.clave = claveAnonima == null ? "" : claveAnonima.trim();
        this.tiempoMs = tiempoMs;
    }

    @Override public boolean configurado() { return !url.isEmpty() && !clave.isEmpty(); }

    // --- Cuenta ---

    @Override public void registrar(String correo, String contrasena, String nombre) throws ErrorRemoto {
        JSONObject cuerpo = json("email", correo, "password", contrasena);
        put(cuerpo, "data", json("nombre", nombre));
        exigir(enviar("/auth/v1/signup", null, cuerpo));
    }

    @Override public SesionRemota iniciarSesion(String correo, String contrasena) throws ErrorRemoto {
        return leerSesion(exigir(enviar("/auth/v1/token?grant_type=password", null, json("email", correo, "password", contrasena))));
    }

    @Override public SesionRemota renovar(String tokenRenovacion) throws ErrorRemoto {
        Respuesta respuesta = enviar("/auth/v1/token?grant_type=refresh_token", null, json("refresh_token", tokenRenovacion));
        // Un token de renovación vencido, usado o revocado se rechaza con 400; para la app es lo mismo que no tener permiso.
        if (respuesta.estado == 400 || respuesta.estado == 401 || respuesta.estado == 403) throw ErrorRemoto.de(ErrorRemoto.Tipo.NO_AUTORIZADO, null);
        return leerSesion(exigir(respuesta));
    }

    @Override public SesionRemota confirmarCorreo(String correo, String codigo) throws ErrorRemoto {
        return leerSesion(exigir(enviar("/auth/v1/verify", null, json("type", "signup", "email", correo, "token", codigo))));
    }

    @Override public void reenviarConfirmacion(String correo) throws ErrorRemoto {
        exigir(enviar("/auth/v1/resend", null, json("type", "signup", "email", correo)));
    }

    @Override public void recuperarAcceso(String correo) throws ErrorRemoto {
        exigir(enviar("/auth/v1/recover", null, json("email", correo)));
    }

    @Override public void cerrarSesion(String tokenAcceso) throws ErrorRemoto {
        exigir(enviar("/auth/v1/logout", tokenAcceso, new JSONObject()));
    }

    // --- Fincas, invitaciones e integrantes ---

    @Override public void vincularFinca(String tokenAcceso, FincaRemota finca) throws ErrorRemoto {
        JSONObject parametros = json("p_id_finca", finca.idFinca, "p_nombre", finca.nombre);
        put(parametros, "p_ubicacion", finca.ubicacion == null ? JSONObject.NULL : finca.ubicacion);
        put(parametros, "p_area_hectareas", finca.areaHectareas == null ? JSONObject.NULL : finca.areaHectareas);
        rpc("vincular_finca", tokenAcceso, parametros);
    }

    @Override public List<PertenenciaRemota> misFincas(String tokenAcceso) throws ErrorRemoto {
        JSONArray filas = arreglo(rpc("mis_fincas", tokenAcceso, new JSONObject()));
        List<PertenenciaRemota> lista = new ArrayList<>();
        try {
            for (int i = 0; i < filas.length(); i++) {
                JSONObject f = filas.getJSONObject(i);
                lista.add(new PertenenciaRemota(texto(f, "id_finca"), texto(f, "nombre"), texto(f, "rol"), texto(f, "estado")));
            }
        } catch (JSONException ex) { throw respuestaInvalida(ex); }
        return lista;
    }

    @Override public List<InvitacionRemota> misInvitaciones(String tokenAcceso) throws ErrorRemoto {
        try { return invitaciones(arreglo(rpc("mis_invitaciones", tokenAcceso, new JSONObject()))); }
        catch (JSONException ex) { throw respuestaInvalida(ex); }
    }

    @Override public void aceptarInvitacion(String tokenAcceso, String idInvitacion) throws ErrorRemoto {
        rpc("aceptar_invitacion", tokenAcceso, json("p_id_invitacion", idInvitacion));
    }

    @Override public IntegrantesRemotos integrantes(String tokenAcceso, String idFinca) throws ErrorRemoto {
        String cuerpo = rpc("integrantes", tokenAcceso, json("p_id_finca", idFinca));
        try {
            JSONObject datos = new JSONObject(cuerpo);
            List<IntegranteRemoto> integrantes = new ArrayList<>();
            JSONArray filas = datos.optJSONArray("integrantes");
            for (int i = 0; filas != null && i < filas.length(); i++) {
                JSONObject f = filas.getJSONObject(i);
                integrantes.add(new IntegranteRemoto(texto(f, "id_usuario_finca"), texto(f, "nombre"), texto(f, "correo"), texto(f, "rol"),
                        texto(f, "estado"), texto(f, "fecha_ingreso"), texto(f, "fecha_retiro"), texto(f, "motivo_retiro")));
            }
            JSONArray pendientes = datos.optJSONArray("invitaciones");
            return new IntegrantesRemotos(integrantes, pendientes == null ? new ArrayList<>() : invitaciones(pendientes));
        } catch (JSONException ex) { throw respuestaInvalida(ex); }
    }

    @Override public void invitarEncargado(String tokenAcceso, String idFinca, String correo) throws ErrorRemoto {
        rpc("invitar_encargado", tokenAcceso, json("p_id_finca", idFinca, "p_correo", correo));
    }

    @Override public void cancelarInvitacion(String tokenAcceso, String idInvitacion) throws ErrorRemoto {
        rpc("cancelar_invitacion", tokenAcceso, json("p_id_invitacion", idInvitacion));
    }

    @Override public void retirarAcceso(String tokenAcceso, String idUsuarioFinca, String motivo) throws ErrorRemoto {
        rpc("retirar_acceso", tokenAcceso, json("p_id_usuario_finca", idUsuarioFinca, "p_motivo", motivo));
    }

    @Override public int subirCambios(String tokenAcceso, String idFinca, String tabla, JSONArray filas) throws ErrorRemoto {
        String cuerpo = rpc("subir_cambios", tokenAcceso, json("p_id_finca", idFinca, "p_tabla", tabla, "p_filas", filas));
        try {
            return Integer.parseInt(cuerpo.trim());
        } catch (NumberFormatException ex) {
            throw respuestaInvalida(ex);
        }
    }

    @Override public JSONArray bajarCambios(String tokenAcceso, String idFinca, String tabla, String desdeEn, String desdeId, int limite) throws ErrorRemoto {
        // Un parámetro nulo debe viajar como null explícito: si se omite, el servidor no encuentra la función.
        return arreglo(rpc("bajar_cambios", tokenAcceso, json("p_id_finca", idFinca, "p_tabla", tabla,
                "p_desde_en", desdeEn == null ? JSONObject.NULL : desdeEn, "p_desde_id", desdeId == null ? JSONObject.NULL : desdeId,
                "p_limite", limite)));
    }

    @Override public DatosFincaRemota datosDeFinca(String tokenAcceso, String idFinca) throws ErrorRemoto {
        try {
            JSONObject datos = new JSONObject(rpc("datos_de_finca", tokenAcceso, json("p_id_finca", idFinca)));
            JSONObject f = datos.getJSONObject("finca");
            Double area = f.isNull("area_hectareas") ? null : f.getDouble("area_hectareas");
            FincaRemota finca = new FincaRemota(texto(f, "id_finca"), texto(f, "nombre"), f.isNull("ubicacion") ? null : f.getString("ubicacion"), area);
            JSONArray filas = datos.getJSONArray("personas");
            List<PersonaRemota> personas = new ArrayList<>();
            for (int i = 0; i < filas.length(); i++) {
                JSONObject p = filas.getJSONObject(i);
                personas.add(new PersonaRemota(texto(p, "id_cuenta"), texto(p, "nombre"), texto(p, "correo"), texto(p, "rol"), texto(p, "estado")));
            }
            return new DatosFincaRemota(finca, texto(f, "id_propietario"), texto(f, "creado_en"), personas);
        } catch (JSONException ex) {
            throw respuestaInvalida(ex);
        }
    }

    // --- Lectura de respuestas ---

    private static List<InvitacionRemota> invitaciones(JSONArray filas) throws JSONException {
        List<InvitacionRemota> lista = new ArrayList<>();
        for (int i = 0; i < filas.length(); i++) {
            JSONObject f = filas.getJSONObject(i);
            String estado = texto(f, "estado");
            lista.add(new InvitacionRemota(texto(f, "id_invitacion_finca"), texto(f, "id_finca"), texto(f, "nombre_finca"), texto(f, "invitado_por"),
                    texto(f, "correo_invitado"), estado == null ? InvitacionRemota.PENDIENTE : estado, texto(f, "creada_en"), texto(f, "vence_en")));
        }
        return lista;
    }

    private static SesionRemota leerSesion(String cuerpo) throws ErrorRemoto {
        try {
            JSONObject datos = new JSONObject(cuerpo);
            JSONObject usuario = datos.getJSONObject("user");
            long expira = datos.has("expires_at") && !datos.isNull("expires_at") ? datos.getLong("expires_at")
                    : System.currentTimeMillis() / 1000 + datos.getLong("expires_in");
            return new SesionRemota(datos.getString("access_token"), datos.getString("refresh_token"), usuario.getString("id"),
                    texto(usuario, "email") == null ? "" : texto(usuario, "email"), texto(usuario, "email_confirmed_at") != null, expira);
        } catch (JSONException ex) { throw respuestaInvalida(ex); }
    }

    private static JSONArray arreglo(String cuerpo) throws ErrorRemoto {
        try { return new JSONArray(cuerpo); }
        catch (JSONException ex) { throw respuestaInvalida(ex); }
    }

    private static ErrorRemoto respuestaInvalida(Exception causa) {
        return ErrorRemoto.de(ErrorRemoto.Tipo.SERVIDOR, null, "El servidor respondió algo inesperado", causa);
    }

    /** El texto de un campo, o nulo si falta o es null en el JSON. */
    private static String texto(JSONObject objeto, String campo) {
        return objeto.isNull(campo) ? null : objeto.optString(campo, null);
    }

    // --- HTTP ---

    private String rpc(String funcion, String token, JSONObject parametros) throws ErrorRemoto {
        return exigir(enviar("/rest/v1/rpc/" + funcion, token, parametros));
    }

    private Respuesta enviar(String ruta, String token, JSONObject cuerpo) throws ErrorRemoto {
        if (!configurado()) throw ErrorRemoto.de(ErrorRemoto.Tipo.SIN_CONFIGURAR, null);
        HttpURLConnection conexion = null;
        try {
            conexion = (HttpURLConnection) new URL(url + ruta).openConnection();
            conexion.setRequestMethod("POST");
            conexion.setConnectTimeout(tiempoMs);
            conexion.setReadTimeout(tiempoMs);
            conexion.setDoOutput(true);
            conexion.setRequestProperty("apikey", clave);
            if (token != null) conexion.setRequestProperty("Authorization", "Bearer " + token);
            conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conexion.setRequestProperty("Accept", "application/json");
            byte[] bytes = cuerpo.toString().getBytes(StandardCharsets.UTF_8);
            conexion.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream salida = conexion.getOutputStream()) { salida.write(bytes); }
            int estado = conexion.getResponseCode();
            InputStream flujo = estado >= 400 ? conexion.getErrorStream() : conexion.getInputStream();
            return new Respuesta(estado, flujo == null ? "" : leer(flujo));
        } catch (IOException ex) {
            // Sin red, servidor caído o respuesta demasiado lenta: para la persona es lo mismo.
            throw ErrorRemoto.de(ErrorRemoto.Tipo.SIN_CONEXION, null, "No hay conexión con el servidor", ex);
        } finally {
            if (conexion != null) conexion.disconnect();
        }
    }

    private static String leer(InputStream entrada) throws IOException {
        try (InputStream flujo = entrada) {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            int n;
            while ((n = flujo.read(buffer)) > 0) salida.write(buffer, 0, n);
            return new String(salida.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    /** Devuelve el cuerpo si la respuesta fue exitosa; si no, lanza el error que le corresponde. */
    private static String exigir(Respuesta respuesta) throws ErrorRemoto {
        if (respuesta.exitosa()) return respuesta.cuerpo;
        throw traducir(respuesta);
    }

    private static ErrorRemoto traducir(Respuesta respuesta) {
        JSONObject datos = null;
        try { datos = new JSONObject(respuesta.cuerpo); } catch (JSONException ignorada) { /* no era JSON */ }
        String codigoError = datos == null ? null : texto(datos, "error_code");
        String codigo = datos == null ? null : texto(datos, "code");
        String mensaje = datos == null ? null : primero(texto(datos, "message"), texto(datos, "msg"), texto(datos, "error_description"));
        String error = datos == null ? null : texto(datos, "error");
        String detalle = "HTTP " + respuesta.estado + (mensaje == null ? "" : ": " + mensaje);

        if (respuesta.estado == 429 || (codigoError != null && codigoError.startsWith("over_") && codigoError.endsWith("_limit"))) {
            return ErrorRemoto.de(ErrorRemoto.Tipo.REGLA, ErrorRemoto.DEMASIADOS_INTENTOS, detalle, null);
        }
        // Un código de confirmación equivocado o vencido (Supabase no distingue entre los dos).
        if ("otp_expired".equals(codigoError) || "invalid_otp".equals(codigoError)) {
            return ErrorRemoto.de(ErrorRemoto.Tipo.REGLA, ErrorRemoto.CODIGO_INVALIDO, detalle, null);
        }
        if ("weak_password".equals(codigoError)) return ErrorRemoto.de(ErrorRemoto.Tipo.REGLA, ErrorRemoto.CONTRASENA_DEBIL, detalle, null);
        if ("user_already_exists".equals(codigoError) || "email_exists".equals(codigoError)) {
            return ErrorRemoto.de(ErrorRemoto.Tipo.REGLA, ErrorRemoto.CORREO_REGISTRADO, detalle, null);
        }
        if ("email_not_confirmed".equals(codigoError)) return ErrorRemoto.de(ErrorRemoto.Tipo.CORREO_SIN_CONFIRMAR, null, detalle, null);
        if ("invalid_credentials".equals(codigoError) || "invalid_grant".equals(error)) {
            return ErrorRemoto.de(ErrorRemoto.Tipo.CREDENCIALES, null, detalle, null);
        }
        // Las funciones del servidor señalan una regla con RAISE EXCEPTION '<codigo>', que PostgREST entrega como P0001.
        if ("P0001".equals(codigo) && mensaje != null) {
            if ("correo_sin_confirmar".equals(mensaje)) return ErrorRemoto.de(ErrorRemoto.Tipo.CORREO_SIN_CONFIRMAR, null, detalle, null);
            return ErrorRemoto.de(ErrorRemoto.Tipo.REGLA, mensaje, detalle, null);
        }
        if ("PGRST202".equals(codigo)) return ErrorRemoto.de(ErrorRemoto.Tipo.SERVIDOR, ErrorRemoto.ESQUEMA_NO_INSTALADO, detalle, null);
        if (respuesta.estado == 401 || respuesta.estado == 403) return ErrorRemoto.de(ErrorRemoto.Tipo.NO_AUTORIZADO, null, detalle, null);
        return ErrorRemoto.de(ErrorRemoto.Tipo.SERVIDOR, null, detalle, null);
    }

    private static String primero(String... valores) {
        for (String valor : valores) if (valor != null && !valor.isEmpty()) return valor;
        return null;
    }

    private static JSONObject json(Object... paresClaveValor) {
        JSONObject objeto = new JSONObject();
        for (int i = 0; i < paresClaveValor.length; i += 2) put(objeto, (String) paresClaveValor[i], paresClaveValor[i + 1]);
        return objeto;
    }

    private static void put(JSONObject objeto, String clave, Object valor) {
        try { objeto.put(clave, valor); }
        catch (JSONException ex) { throw new IllegalStateException("Valor no válido para " + clave, ex); }
    }
}
