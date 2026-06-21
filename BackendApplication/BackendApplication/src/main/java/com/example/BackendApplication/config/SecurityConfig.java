package com.example.BackendApplication.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.BackendApplication.service.JwtService;
import com.example.BackendApplication.service.UserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserService userService;
    private final AppProperties props;
    private final OAuth2AuthorizedClientService authorizedClientService;

    // Standard constructor injection (No Lombok!)
    public SecurityConfig(JwtService jwtService, UserService userService, AppProperties props,
            OAuth2AuthorizedClientService authorizedClientService) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.props = props;
        this.authorizedClientService = authorizedClientService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Disable CSRF for external GitHub webhooks
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**"))

                // 2. Apply our CORS configurations
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. Make session management stateless (we are using JWTs)
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Define route permissions
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/webhooks/**").permitAll()
                        .requestMatchers("/login/**", "/oauth2/**").permitAll()
                        .anyRequest().authenticated())

                // 5. Configure OAuth2 GitHub Login flow
                .oauth2Login(oauth2 -> oauth2
                .successHandler((request, response, authentication) -> {
                    OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                    String githubId = oauth2User.getAttribute("id").toString();
                    String login = oauth2User.getAttribute("login");
                    String avatarUrl = oauth2User.getAttribute("avatar_url");

                    OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
                    OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                        oauthToken.getAuthorizedClientRegistrationId(),
                        oauthToken.getName()
                    );
                    String accessToken = client.getAccessToken().getTokenValue();

                    var user = userService.findOrCreate(githubId, login, avatarUrl, accessToken);
                    String jwt = jwtService.generate(user.getId(), user.getGithubLogin());

                    // 1. Create the HttpOnly Cookie
                    jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("token", jwt);
                    cookie.setHttpOnly(true);
                    cookie.setSecure(false); // Set to true in production (HTTPS only)
                    cookie.setPath("/");
                    cookie.setMaxAge(props.getJwt().getExpiryHours() * 3600); // 3600 seconds in an hour

                    // 2. Add the cookie to the HTTP response
                    response.addCookie(cookie);

                    // 3. Redirect the user straight to the frontend dashboard!
                    response.sendRedirect(props.getFrontendUrl() + "/dashboard");
                }))


                // 6. Register our custom JWT verification filter
                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Custom JWT filter that parses Bearer tokens from incoming HTTP headers
    @Bean
    public OncePerRequestFilter jwtAuthFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                    throws java.io.IOException, jakarta.servlet.ServletException {

                jakarta.servlet.http.Cookie[] cookies = req.getCookies();

                String token = null;
                if (cookies != null) {
                    for (var cookie : cookies) {
                        if ("token".equals(cookie.getName())) {
                            token = cookie.getValue();
                            break;
                        }
                    }
                }

                // If a token cookie was found, validate and authentivate the user
                if (token != null) {

                    try {
                        Long userId = jwtService.extractUserId(token);

                        var user = userService.findById(userId);

                        // Corrected class name spelling and simplified parameters:
                        var auth = new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER")));

                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } catch (Exception ignores) {
                        // If token is invalid or Expired Do not auhtneitcate
                    }
                }
                chain.doFilter(req, res);
            }
        };
    }

    // CORS configurations allowing port 3000 to interact with 8080
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(props.getFrontendUrl()));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
