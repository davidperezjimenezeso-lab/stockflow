
package com.stockflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .cors(Customizer.withDefaults())

                // Configuración temporal para HTTP Basic en desarrollo local
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth

                        // Permitir las solicitudes preflight de CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**")
                        .permitAll()

                        // Consultas para ADMIN y EMPLOYEE
                        .requestMatchers(HttpMethod.GET, "/api/**")
                        .hasAnyRole("ADMIN", "EMPLOYEE")

                        // Gestión de productos: ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/products")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/api/products/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.DELETE, "/api/products/**")
                        .hasRole("ADMIN")

                        // Gestión de categorías: ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/categories")
                        .hasRole("ADMIN")

                        // Movimientos de inventario
                        .requestMatchers(HttpMethod.POST, "/api/stock-movements")
                        .hasAnyRole("ADMIN", "EMPLOYEE")

                        // Ventas
                        .requestMatchers(HttpMethod.POST, "/api/sales")
                        .hasAnyRole("ADMIN", "EMPLOYEE")

                        // Bloquear el resto de operaciones
                        .anyRequest().denyAll()
                )

                .httpBasic(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(
                List.of(
                        "http://localhost:4200",
                        "http://localhost:4201"
                )
        );

        config.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );

        config.setAllowedHeaders(
                List.of("Authorization", "Content-Type")
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/**", config);

        return source;
    }
}
