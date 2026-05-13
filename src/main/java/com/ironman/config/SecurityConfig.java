package com.ironman.config;

import com.ironman.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  @Value("${app.cors.allowed-origins}")
  private String allowedOrigins;

  // 1. REMOVED WebSecurityCustomizer ignoring() 
  // Bypassing the security chain also bypasses CORS headers. 
  // We handle "permiting" inside the filter chain instead.

  @Bean
  public SecurityFilterChain securityFilterChain(
          HttpSecurity http,
          AuthenticationProvider authenticationProvider,
          JwtAuthenticationFilter jwtAuthenticationFilter,
          AuthRateLimitFilter authRateLimitFilter
  ) throws Exception {
    return http
            // 2. MUST BE FIRST: Enable CORS with the source defined below
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider)
            .authorizeHttpRequests(auth -> auth
                    // Permit all auth-related endpoints here so CORS headers are still applied
                    .requestMatchers("/api/v1/auth/**", "/api/v1/health").permitAll()
                    .requestMatchers("/api/v1/services/**", "/api/v1/tracking/**").permitAll()
                    // Allow preflight OPTIONS requests
                    .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                    // Admin paths — require ADMIN role at the filter chain level (defence-in-depth
                    // alongside the @PreAuthorize annotations on the controllers)
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
            )
            // Without an explicit AuthenticationEntryPoint Spring Security 6 falls back to
            // Http403ForbiddenEntryPoint, so an expired / missing token returns 403 instead of
            // 401, and the frontend never knows to refresh.  Return 401 so the client can
            // trigger the token-refresh flow correctly.
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) ->
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
            )
            // Rate-limit auth endpoints before the JWT filter so we don't waste
            // crypto cycles on bots flooding the login route.
            .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
  }

  /** Prevent Spring Boot from auto-registering AuthRateLimitFilter as a global
   *  servlet filter — we add it explicitly to the SecurityFilterChain above so
   *  ordering relative to JwtAuthenticationFilter is deterministic. */
  @Bean
  public FilterRegistrationBean<AuthRateLimitFilter> authRateLimitRegistration(
          AuthRateLimitFilter filter) {
    var reg = new FilterRegistrationBean<>(filter);
    reg.setEnabled(false);
    return reg;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    var configuration = new CorsConfiguration();

    // Comma-separated, trimmed. Supports glob patterns (e.g. https://ironman-frontend-*.vercel.app)
    // via setAllowedOriginPatterns so Vercel preview deployments work without redeploying the API.
    List<String> origins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();

    configuration.setAllowedOriginPatterns(origins);
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

    // Standard headers plus any custom headers your frontend might send
    configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "X-Requested-With",
            "Cache-Control"
    ));

    // Expose the Retry-After header so the frontend can show "try again in N seconds"
    // when the AuthRateLimitFilter kicks in (otherwise the browser hides it).
    configuration.setExposedHeaders(Arrays.asList("Retry-After"));

    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L); // Cache preflight for 1 hour

    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
          JwtAuthenticationFilter filter) {
    var reg = new FilterRegistrationBean<>(filter);
    reg.setEnabled(false);   
    return reg;
  }

  @Bean
  public UserDetailsService userDetailsService(UserRepository userRepository) {
    return username -> userRepository.findByEmailIgnoreCase(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }

  @Bean
  public AuthenticationProvider authenticationProvider(
          UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
    var provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return provider;
  }

  @Bean
  public AuthenticationManager authenticationManager(
          AuthenticationConfiguration configuration) throws Exception {
    return configuration.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}