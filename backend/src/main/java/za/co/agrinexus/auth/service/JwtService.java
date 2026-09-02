/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.security.oauth2.jose.jws.JwsAlgorithm
 *  org.springframework.security.oauth2.jose.jws.MacAlgorithm
 *  org.springframework.security.oauth2.jwt.JwsHeader
 *  org.springframework.security.oauth2.jwt.JwtClaimsSet
 *  org.springframework.security.oauth2.jwt.JwtEncoder
 *  org.springframework.security.oauth2.jwt.JwtEncoderParameters
 *  org.springframework.stereotype.Service
 */
package za.co.agrinexus.auth.service;

import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import za.co.agrinexus.auth.model.User;

@Service
public class JwtService {
    private final JwtEncoder jwtEncoder;
    private final Duration expiry;

    public JwtService(JwtEncoder jwtEncoder, @Value(value="${agrinexus.security.jwt-expiry-minutes}") long expiryMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.expiry = Duration.ofMinutes(expiryMinutes);
    }

    public IssuedToken issue(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(this.expiry);
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("agrinexus-api").issuedAt(issuedAt).expiresAt(expiresAt).subject(user.getEmail()).claim("role", (Object)user.getRole().name()).claim("userId", (Object)user.getId().toString()).build();
        JwsHeader header = JwsHeader.with((JwsAlgorithm)MacAlgorithm.HS256).build();
        String value = this.jwtEncoder.encode(JwtEncoderParameters.from((JwsHeader)header, (JwtClaimsSet)claims)).getTokenValue();
        return new IssuedToken(value, expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }
}
