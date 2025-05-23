package com.dormhub.config;

import com.dormhub.security.JwtAuthFilter;
import com.dormhub.service.CustomUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(CustomUserDetailsService userDetailsService, JwtAuthFilter jwtAuthFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/login", "/register", "/forgot-password", "/reset-password",
                    "/api/auth/**",
                    "/assets/**", "/css/**", "/js/**", "/img/**", "/favicon.ico"
                ).permitAll()
                .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/help-desk/**").hasAuthority("ROLE_HELP_DESK")
                .requestMatchers("/mahasiswa/**").hasAnyAuthority("ROLE_MAHASISWA", "ROLE_SENIOR_RESIDENCE")
                .anyRequest().authenticated()
            )
            .exceptionHandling(exc -> exc
                .accessDeniedPage("/login?error=akses-ditolak")
                .authenticationEntryPoint((request, response, authException) -> {
                    logger.warn("Akses tidak sah: {}", authException.getMessage());
                    response.sendRedirect("/login?error=tidak-terautentikasi");
                })
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        logger.info("SecurityFilterChain configuration completed with JWT support");
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Encoder for password hashing
    }
}
