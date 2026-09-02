/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.nimbusds.jose.jwk.source.ImmutableSecret
 *  com.nimbusds.jose.jwk.source.JWKSource
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.http.HttpMethod
 *  org.springframework.security.authentication.AuthenticationManager
 *  org.springframework.security.authentication.AuthenticationProvider
 *  org.springframework.security.authentication.ProviderManager
 *  org.springframework.security.authentication.dao.DaoAuthenticationProvider
 *  org.springframework.security.config.annotation.web.builders.HttpSecurity
 *  org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer$AuthorizedUrl
 *  org.springframework.security.config.http.SessionCreationPolicy
 *  org.springframework.security.core.userdetails.User
 *  org.springframework.security.core.userdetails.UserDetailsService
 *  org.springframework.security.core.userdetails.UsernameNotFoundException
 *  org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
 *  org.springframework.security.crypto.password.PasswordEncoder
 *  org.springframework.security.oauth2.jose.jws.MacAlgorithm
 *  org.springframework.security.oauth2.jwt.JwtDecoder
 *  org.springframework.security.oauth2.jwt.JwtEncoder
 *  org.springframework.security.oauth2.jwt.NimbusJwtDecoder
 *  org.springframework.security.oauth2.jwt.NimbusJwtEncoder
 *  org.springframework.security.web.SecurityFilterChain
 */
package za.co.agrinexus.shared.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import za.co.agrinexus.auth.model.AccountStatus;
import za.co.agrinexus.auth.repository.UserRepository;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return (SecurityFilterChain)http.csrf(csrf -> csrf.disable()).cors(cors -> {}).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(authorize -> ((AuthorizeHttpRequestsConfigurer.AuthorizedUrl)((AuthorizeHttpRequestsConfigurer.AuthorizedUrl)((AuthorizeHttpRequestsConfigurer.AuthorizedUrl)((AuthorizeHttpRequestsConfigurer.AuthorizedUrl)authorize.requestMatchers(HttpMethod.GET, new String[]{"/api/v1/system/status", "/api/v1/market-prices", "/api/v1/marketplace/listings"})).permitAll().requestMatchers(HttpMethod.POST, new String[]{"/api/v1/auth/register", "/api/v1/auth/login"})).permitAll().requestMatchers(new String[]{"/actuator/health"})).permitAll().anyRequest()).authenticated()).oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> {})).build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return username -> users.findByEmail(username).map(user -> User.builder().username(user.getEmail()).password(user.getPasswordHash()).roles(new String[]{user.getRole().name()}).disabled(user.getAccountStatus() == AccountStatus.DISABLED).accountLocked(user.getAccountStatus() == AccountStatus.LOCKED).build()).orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(new AuthenticationProvider[]{provider});
    }

    @Bean
    SecretKey jwtSecretKey(@Value(value="${agrinexus.security.jwt-secret}") String secret) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes.");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder((JWKSource)new ImmutableSecret(key));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key) {
        return NimbusJwtDecoder.withSecretKey((SecretKey)key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
