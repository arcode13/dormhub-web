package com.dormhub.config;

import com.dormhub.service.CustomUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF if not needed
            .headers(headers -> headers.frameOptions().sameOrigin()) // Needed for iframes (optional)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/forgot-password", "/reset-password", "/assets/**", "/css/**", "/js/**", "/img/**").permitAll() // Public pages
                .anyRequest().authenticated() // All other pages require authentication
            )
            .formLogin(login -> login
                .loginPage("/login") // Custom login page
                .loginProcessingUrl("/login") // Login processing URL
                .usernameParameter("email") // Use email as username
                .passwordParameter("password") // Password remains the same
                .defaultSuccessUrl("/", true) // Redirect after successful login
                .successHandler((request, response, authentication) -> {
                    String redirectUrl = getRedirectUrlBasedOnRole(authentication);
                    response.sendRedirect(redirectUrl); // Redirect based on role
                })
                .failureUrl("/login") // Redirect if login fails
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout") // Logout URL
                .logoutSuccessUrl("/login") // Redirect after logout
                .permitAll()
            )
            .rememberMe(remember -> remember
                .key("uniqueAndSecretKey") // Secure key for remember-me functionality
                .tokenValiditySeconds(1209600) // Duration (2 weeks)
            );

        logger.info("SecurityFilterChain configuration completed");
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

    private String getRedirectUrlBasedOnRole(org.springframework.security.core.Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .findFirst()
            .map(role -> {
                switch (role) {
                    case "ROLE_MAHASISWA":
                    case "ROLE_SENIOR_RESIDENCE":
                        return "/mahasiswa/dashboard";
                    case "ROLE_HELP_DESK":
                        return "/help-desk/dashboard";
                    case "ROLE_ADMIN":
                        return "/admin/dashboard";
                    default:
                        return "/";
                }
            })
            .orElse("/");
    }
}
