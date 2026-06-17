package com.example.DtaAssigement.config;

import com.example.DtaAssigement.security.CustomUserDetailsService;
import com.example.DtaAssigement.security.JwtAuthenticationFilter;
import com.example.DtaAssigement.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.example.DtaAssigement.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.example.DtaAssigement.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.example.DtaAssigement.service.impl.CustomOAuth2UserService;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@AllArgsConstructor
public class SecurityConfig {

        private final CustomUserDetailsService userDetailsService;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final CustomOAuth2UserService customOAuth2UserService;

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
                return authConfig.getAuthenticationManager();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http,
                        AuthenticationConfiguration authConfig,
                        OAuth2AuthenticationSuccessHandler oauth2SuccessHandler,
                        OAuth2AuthenticationFailureHandler oauth2FailureHandler,
                        HttpCookieOAuth2AuthorizationRequestRepository cookieAuthRepository) throws Exception {
                http
                                .cors(Customizer.withDefaults()) // 1. Bật CORS
                                .csrf(AbstractHttpConfigurer::disable) // 2. Tắt CSRF
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(antMatcher("/api/auth/**"),
                                                                antMatcher("/v3/api-docs/**"),
                                                                antMatcher("/swagger-ui/**"),
                                                                antMatcher("/swagger-ui.html"),
                                                                antMatcher("/docs/**"),
                                                                antMatcher("/error"),
                                                                antMatcher("/favicon.ico"),
                                                                // MoMo: allow redirect landing page and IPN webhook
                                                                // without auth
                                                                antMatcher("/api/invoices/return/momo"),
                                                                antMatcher("/api/invoices/webhook/momo"),
                                                                // PayPal & SSE: allow public callbacks
                                                                antMatcher("/api/invoices/paypal/webhook"),
                                                                antMatcher("/api/invoices/paypal/success"),
                                                                antMatcher("/api/invoices/paypal/cancel"),
                                                                antMatcher("/api/invoices/sse/order/**"))
                                                .permitAll()
                                                .requestMatchers(antMatcher("/api/menu-items/**")).permitAll()
                                                .requestMatchers(antMatcher("/api/categories/**")).permitAll()
                                                .requestMatchers(antMatcher("/api/revenues/items/top")).permitAll()
                                                .requestMatchers(antMatcher(HttpMethod.POST, "/api/contacts")).permitAll()
                                                .requestMatchers(antMatcher("/**/*.html"),
                                                                antMatcher("/**/*.js"),
                                                                antMatcher("/**/*.css"))
                                                .permitAll()
 
                                                .requestMatchers("/ws/**").permitAll()
 
                                                .anyRequest().authenticated())
 
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
 
                                // Cấu hình OAuth2 login
                                .oauth2Login(oauth2 -> oauth2
                                                .authorizationEndpoint(auth -> auth
                                                                .authorizationRequestRepository(cookieAuthRepository))
                                                .redirectionEndpoint(redir -> redir
                                                                .baseUri("/login/oauth2/code/*"))
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService))
                                                .successHandler(oauth2SuccessHandler)
                                                .failureHandler(oauth2FailureHandler))

                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration cfg = new CorsConfiguration();
                cfg.setAllowedOriginPatterns(List.of("*"));
                cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                cfg.setAllowedHeaders(List.of("*"));
                cfg.setAllowCredentials(true);
                UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
                src.registerCorsConfiguration("/**", cfg);
                return src;
        }

}
