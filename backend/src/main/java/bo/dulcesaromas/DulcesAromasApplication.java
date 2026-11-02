package bo.dulcesaromas;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

// Sin el usuario en memoria que Spring Security crea por defecto: las cuentas viven en la base.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
public class DulcesAromasApplication {

	public static void main(String[] args) {
		SpringApplication.run(DulcesAromasApplication.class, args);
	}

	/**
	 * La hora actual sale de este reloj, para que las pruebas puedan adelantarlo.
	 */
	@Bean
	Clock reloj() {
		return Clock.systemUTC();
	}

}
