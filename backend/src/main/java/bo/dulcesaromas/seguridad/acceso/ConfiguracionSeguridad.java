package bo.dulcesaromas.seguridad.acceso;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Toda ruta exige sesión salvo las que se abren aquí. Cada controlador declara además el permiso que exige
 * con {@code @PreAuthorize}, así que el servidor valida cada acción (RNF07).
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(PropiedadesSeguridad.class)
class ConfiguracionSeguridad {

	@Bean
	SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, ServicioSesiones sesiones, ManejadorAccesos manejador) {
		return http
			// Sin cookies de sesión: el token va en una cabecera, así que no hay CSRF que prevenir.
			.csrf(AbstractHttpConfigurer::disable)
			.cors(Customizer.withDefaults())
			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(a -> a
				.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/clientes", "/api/auth/recuperacion",
						"/api/auth/recuperacion/confirmar").permitAll()
				// RN15: el catálogo y el total del carrito se consultan sin sesión; hacer el pedido sí la exige.
				.requestMatchers(HttpMethod.GET, "/api/catalogo/**", "/api/metodos-pago/*/qr").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/pedidos/cotizacion").permitAll()
				.requestMatchers("/error").permitAll()
				.anyRequest().authenticated())
			.addFilterBefore(new FiltroToken(sesiones), UsernamePasswordAuthenticationFilter.class)
			.exceptionHandling(e -> e.authenticationEntryPoint(manejador).accessDeniedHandler(manejador))
			.build();
	}

	/**
	 * El Panel web se sirve desde otro origen que la API; solo esos orígenes pueden llamarla desde un navegador.
	 * La app móvil no usa CORS. Spring Security busca este bean por su nombre.
	 */
	@Bean
	CorsConfigurationSource corsConfigurationSource(@Value("${seguridad.cors.origenes}") List<String> origenes) {
		var configuracion = new CorsConfiguration();
		configuracion.setAllowedOrigins(origenes);
		configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
		configuracion.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
		var fuente = new UrlBasedCorsConfigurationSource();
		fuente.registerCorsConfiguration("/api/**", configuracion);
		return fuente;
	}

	@Bean
	PasswordEncoder codificadorContrasenas() {
		return new BCryptPasswordEncoder();
	}

}
