package za.co.agrinexus.auth.service;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.co.agrinexus.auth.model.*;
import za.co.agrinexus.auth.dto.*;
import za.co.agrinexus.auth.repository.UserRepository;
import za.co.agrinexus.auth.verification.*;
class VerifiedLoginTest {
 @Test void newUserStartsUnverified(){assertFalse(new User("T","F","test@example.invalid","+27000000000","bcrypt").isEmailVerified());}
 @Test void unverifiedLoginNeverIssuesJwt(){var users=mock(UserRepository.class);var user=mock(User.class);when(users.findByEmail("test@example.invalid")).thenReturn(Optional.of(user));var jwt=mock(JwtService.class);var service=new AuthService(users,mock(PasswordEncoder.class),mock(AuthenticationManager.class),jwt,mock(EmailVerificationService.class));assertEquals("EMAIL_NOT_VERIFIED",assertThrows(VerificationProblem.class,()->service.login(new LoginRequest("test@example.invalid","correct-password"))).code);verifyNoInteractions(jwt);}
 @Test void verifiedLoginReturnsExistingAuthResponse(){var users=mock(UserRepository.class);var user=mock(User.class);when(user.isEmailVerified()).thenReturn(true);when(user.getId()).thenReturn(UUID.randomUUID());when(users.findByEmail("test@example.invalid")).thenReturn(Optional.of(user));var jwt=mock(JwtService.class);when(jwt.issue(user)).thenReturn(new JwtService.IssuedToken("jwt",Instant.now().plusSeconds(3600)));var service=new AuthService(users,mock(PasswordEncoder.class),mock(AuthenticationManager.class),jwt,mock(EmailVerificationService.class));assertEquals("jwt",service.login(new LoginRequest("test@example.invalid","correct-password")).accessToken());}
}
