package za.co.agrinexus.shared.config;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import za.co.agrinexus.auth.repository.UserRepository;
import za.co.agrinexus.auth.model.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
class VerifiedJwtTest {
 @Test void signedJwtStillRequiresAVerifiedActiveAccount(){var config=new SecurityConfig();var key=config.jwtSecretKey("test-only-secret-at-least-thirty-two-bytes");var encoder=config.jwtEncoder(key);var users=mock(UserRepository.class);UUID id=UUID.randomUUID();var user=mock(User.class);when(user.getId()).thenReturn(id);when(user.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);when(users.findByEmail("test@example.invalid")).thenReturn(Optional.of(user));var claims=JwtClaimsSet.builder().issuer("agrinexus-api").subject("test@example.invalid").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).claim("userId",id.toString()).build();String signed=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();var decoder=config.jwtDecoder(key,users);assertThrows(JwtValidationException.class,()->decoder.decode(signed));when(user.isEmailVerified()).thenReturn(true);assertEquals("test@example.invalid",decoder.decode(signed).getSubject());when(user.getAccountStatus()).thenReturn(AccountStatus.DISABLED);assertThrows(JwtValidationException.class,()->decoder.decode(signed));}
}
