package bo.dulcesaromas.seguridad.cuentas;

/**
 * Envía un correo de texto. Solo se usa para recuperar la contraseña (CU12); las notificaciones automáticas
 * están fuera del alcance.
 */
public interface EnviadorCorreo {

	void enviar(String para, String asunto, String texto);

}
