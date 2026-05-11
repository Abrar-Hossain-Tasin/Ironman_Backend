//package com.ironman.config;
//
//import com.ironman.repository.UserRepository;
//import java.util.Arrays;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpMethod;
//import org.springframework.core.annotation.Order;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.AuthenticationProvider;
//import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
//import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
//import org.springframework.security.web.util.matcher.OrRequestMatcher;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//import org.springframework.web.filter.CorsFilter;
//
//@Configuration
//@EnableWebSecurity
//@EnableMethodSecurity
//public class SecurityConfig {
//  @Value("${app.cors.allowed-origins}")
//  private String allowedOrigins;
//
////  @Bean
////  public WebSecurityCustomizer webSecurityCustomizer() {
////    return web -> web.ignoring()
////        .requestMatchers(HttpMethod.OPTIONS, "/**")
////        .requestMatchers(HttpMethod.GET, "/api/v1/health")
////        .requestMatchers(HttpMethod.GET, "/api/v1/services", "/api/v1/services/**")
////        .requestMatchers(HttpMethod.GET, "/api/v1/tracking", "/api/v1/tracking/**")
////        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh", "api/v1/admin/**");
////  }
//
//  @Bean
//  public WebSecurityCustomizer webSecurityCustomizer() {
//    return web -> web.ignoring()
//            .requestMatchers(HttpMethod.OPTIONS, "/**")
//            .requestMatchers(HttpMethod.GET, "/api/v1/health")
//            .requestMatchers(HttpMethod.GET, "/api/v1/services", "/api/v1/services/**")
//            .requestMatchers(HttpMethod.GET, "/api/v1/tracking", "/api/v1/tracking/**")
//            // REMOVED "api/v1/admin/**" from here
//            .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh");
//  }
//
//  @Bean
//  @Order(1)
//  public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
//    return http
//        .securityMatcher(new OrRequestMatcher(
//            new AntPathRequestMatcher("/**", HttpMethod.OPTIONS.name()),
//            new AntPathRequestMatcher("/api/v1/health", HttpMethod.GET.name()),
//            new AntPathRequestMatcher("/api/v1/services", HttpMethod.GET.name()),
//            new AntPathRequestMatcher("/api/v1/services/**", HttpMethod.GET.name()),
//            new AntPathRequestMatcher("/api/v1/tracking", HttpMethod.GET.name()),
//            new AntPathRequestMatcher("/api/v1/tracking/**", HttpMethod.GET.name()),
//            new AntPathRequestMatcher("/api/v1/auth/register", HttpMethod.POST.name()),
//            new AntPathRequestMatcher("/api/v1/auth/login", HttpMethod.POST.name()),
//            new AntPathRequestMatcher("/api/v1/auth/refresh", HttpMethod.POST.name())
//        ))
//        .csrf(csrf -> csrf.disable())
//        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
//        .build();
//  }
//
//  @Bean
//  @Order(2)
//  public SecurityFilterChain securityFilterChain(
//      HttpSecurity http,
//      AuthenticationProvider authenticationProvider,
//      JwtAuthenticationFilter jwtAuthenticationFilter
//  ) throws Exception {
//    return http
//        .csrf(csrf -> csrf.disable())
//        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//        .authenticationProvider(authenticationProvider)
//        .authorizeHttpRequests(auth -> auth
//            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
//            .requestMatchers("/api/v1/health").permitAll()
//            .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
//            .requestMatchers(HttpMethod.GET, "/api/v1/services", "/api/v1/services/**", "/api/v1/tracking", "/api/v1/tracking/**").permitAll()
//            .anyRequest().authenticated()
//        )
//        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
//        .build();
//  }
//
//  @Bean
//  public UserDetailsService userDetailsService(UserRepository userRepository) {
//    return username -> userRepository.findByEmailIgnoreCase(username)
//        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
//  }
//
//  @Bean
//  public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
//    var provider = new DaoAuthenticationProvider();
//    provider.setUserDetailsService(userDetailsService);
//    provider.setPasswordEncoder(passwordEncoder);
//    return provider;
//  }
//
//  @Bean
//  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
//    return configuration.getAuthenticationManager();
//  }
//
//  @Bean
//  public PasswordEncoder passwordEncoder() {
//    return new BCryptPasswordEncoder();
//  }
//
//  @Bean
//  public CorsConfigurationSource corsConfigurationSource() {
//    var configuration = new CorsConfiguration();
//    configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
//    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//    configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
//    configuration.setAllowCredentials(true);
//
//    var source = new UrlBasedCorsConfigurationSource();
//    source.registerCorsConfiguration("/**", configuration);
//    return source;
//  }
//
//  @Bean
//  public CorsFilter corsFilter(CorsConfigurationSource corsConfigurationSource) {
//    return new CorsFilter(corsConfigurationSource);
//  }
//}
package com.ironman.config;

import com.ironman.repository.UserRepository;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
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
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Value("${app.cors.allowed-origins}")
  private String allowedOrigins;

  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return web -> web.ignoring()
            .requestMatchers(HttpMethod.OPTIONS, "/**")
            .requestMatchers(HttpMethod.GET, "/api/v1/health")
            .requestMatchers(HttpMethod.GET, "/api/v1/services", "/api/v1/services/**")
            .requestMatchers(HttpMethod.GET, "/api/v1/tracking", "/api/v1/tracking/**")
            .requestMatchers(HttpMethod.POST,
                    "/api/v1/auth/register",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh");
  }

  // ── REMOVED the old publicSecurityFilterChain (@Order(1)) bean entirely ──
  // That second chain caused JwtAuthenticationFilter to run outside Spring
  // Security's context management → context got wiped → 403 for DELIVERY_MAN.

  @Bean
  public SecurityFilterChain securityFilterChain(
          HttpSecurity http,
          AuthenticationProvider authenticationProvider,
          JwtAuthenticationFilter jwtAuthenticationFilter
  ) throws Exception {
    return http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider)
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(HttpMethod.GET,
                            "/api/v1/health",
                            "/api/v1/services", "/api/v1/services/**",
                            "/api/v1/tracking", "/api/v1/tracking/**").permitAll()
                    .requestMatchers(HttpMethod.POST,
                            "/api/v1/auth/register",
                            "/api/v1/auth/login",
                            "/api/v1/auth/refresh").permitAll()
                    .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class)
            .build();
  }

  /**
   * KEY FIX: JwtAuthenticationFilter is @Component so Spring Boot
   * auto-registers it as a raw servlet filter OUTSIDE the Security chain.
   * That means it ran twice — once before SecurityContextHolder was
   * initialised and once inside the chain — corrupting the context → 403.
   * Setting enabled=false stops the auto-registration.
   */
  @Bean
  public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
          JwtAuthenticationFilter filter) {
    var reg = new FilterRegistrationBean<>(filter);
    reg.setEnabled(false);   // ← only run inside the SecurityFilterChain
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

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    var configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(
            Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
    configuration.setAllowedMethods(
            Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")); // PATCH added
    configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
    configuration.setAllowCredentials(true);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public CorsFilter corsFilter(CorsConfigurationSource corsConfigurationSource) {
    return new CorsFilter(corsConfigurationSource);
  }
}