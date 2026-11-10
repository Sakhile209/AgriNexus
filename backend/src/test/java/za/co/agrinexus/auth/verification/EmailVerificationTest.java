package za.co.agrinexus.auth.verification;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.transaction.support.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import za.co.agrinexus.auth.dto.RegisterRequest;
class EmailVerificationTest {
 private final VerificationStore store=mock(VerificationStore.class);
 private final VerificationMail mail=mock(VerificationMail.class);
 private final EmailVerificationService service=new EmailVerificationService(store,mail);
 private final UUID id=UUID.randomUUID();
 private final String raw=VerificationTokens.generate();
 private VerificationStore.Account account(boolean verified){return new VerificationStore.Account(id,"farmer@example.invalid",verified,"ACTIVE");}
 private VerificationStore.Token token(Instant expiry,Instant used,Instant invalidated){return new VerificationStore.Token(UUID.randomUUID(),id,expiry,used,invalidated);}
 @Test void tokensAreRandomAndOnlyHashesAreStored(){String a=VerificationTokens.generate(),b=VerificationTokens.generate();assertNotEquals(a,b);assertTrue(VerificationTokens.validFormat(a));assertEquals(64,VerificationTokens.hash(a).length());assertNotEquals(a,VerificationTokens.hash(a));}
 @Test void registrationCreatesPendingAccountAndQueuesRealDelivery(){var request=new RegisterRequest("Test","Farmer"," FARMer@example.invalid ","+27000000000","password-1234");when(store.create(anyString(),eq(request),eq("bcrypt-hash"))).thenReturn(account(false));when(store.lock(id)).thenReturn(account(false));var result=service.register(request,"bcrypt-hash");assertTrue(result.verificationRequired());assertEquals(60,result.resendAfterSeconds());verify(store).queue(id);assertFalse(result.toString().contains("accessToken"));}
 @Test void duplicateRegistrationIsGenericAndNeverOverwritesVerifiedAccount(){when(store.account("farmer@example.invalid")).thenReturn(account(true));when(store.lock(id)).thenReturn(account(true));var result=service.register(new RegisterRequest("T","F","farmer@example.invalid","+27000000000","password-1234"),"unused");assertEquals(EmailVerificationService.registered(),result);verify(store,never()).create(anyString(),any(),anyString());verify(store,never()).queue(any());}
 @Test void validTokenVerifiesAccountAndInvalidatesOtherTokens(){var token=token(Instant.now().plusSeconds(900),null,null);when(store.token(anyString())).thenReturn(token);when(store.lock(id)).thenReturn(account(false));when(store.consume(token.id())).thenReturn(true);service.verify(raw);verify(store).verify(id);verify(store).invalidate(id);}
 @Test void expiredTokenIsRejected(){when(store.token(anyString())).thenReturn(token(Instant.now().minusSeconds(1),null,null));when(store.lock(id)).thenReturn(account(false));var e=assertThrows(VerificationProblem.class,()->service.verify(raw));assertEquals("VERIFICATION_EXPIRED",e.code);verify(store,never()).verify(any());}
 @Test void invalidAndReusedTokensAreRejected(){assertEquals("VERIFICATION_INVALID",assertThrows(VerificationProblem.class,()->service.verify("invalid")).code);when(store.token(anyString())).thenReturn(token(Instant.now().plusSeconds(900),Instant.now(),null));when(store.lock(id)).thenReturn(account(false));assertEquals("VERIFICATION_INVALID",assertThrows(VerificationProblem.class,()->service.verify(raw)).code);verify(store,never()).verify(any());}
 @Test void supersededTokensAreRejected(){when(store.token(anyString())).thenReturn(token(Instant.now().plusSeconds(900),null,Instant.now()));when(store.lock(id)).thenReturn(account(false));assertThrows(VerificationProblem.class,()->service.verify(raw));}
 @Test void resendAcknowledgementDoesNotRevealAccountExistence(){var missing=service.resend("nobody@example.invalid");when(store.account("farmer@example.invalid")).thenReturn(account(false));when(store.lock(id)).thenReturn(account(false));var exists=service.resend("farmer@example.invalid");assertEquals(missing,exists);verify(store).queue(id);}
 @Test void missingEmailConfigurationFailsWithoutSavingAnAccount(){doThrow(new VerificationProblem("EMAIL_UNAVAILABLE","Unavailable",503)).when(mail).requireConfigured();assertEquals(503,assertThrows(VerificationProblem.class,()->service.resend("farmer@example.invalid")).status);verifyNoInteractions(store);}
 @Test void emailTemplateContainsExpiryAndNoPassword(){String link="https://agrinexus.example/#verify-email="+raw,html=BrevoVerificationMail.html(link);assertTrue(html.contains("AgriNexus"));assertTrue(html.contains("15 minutes"));assertTrue(html.contains("did not create"));assertFalse(html.toLowerCase().contains("password"));assertTrue(BrevoVerificationMail.text(link).contains(link));}
 @Test void productionLinksRequireHttps(){assertThrows(IllegalArgumentException.class,()->BrevoVerificationMail.validateUrl("http://agrinexus.example"));assertDoesNotThrow(()->BrevoVerificationMail.validateUrl("http://localhost:5174"));assertDoesNotThrow(()->BrevoVerificationMail.validateUrl("https://agrinexus.example"));assertThrows(IllegalArgumentException.class,()->BrevoVerificationMail.validateUrl("https://attacker@agrinexus.example"));}
 @Test void passwordsBeyondBcryptByteLimitAreRejectedByValidation(){try(var factory=jakarta.validation.Validation.buildDefaultValidatorFactory()){
  var validator=factory.getValidator();assertFalse(validator.validate(new RegisterRequest("T","F","test@example.invalid","+27000000000","a".repeat(73))).isEmpty());assertFalse(validator.validate(new RegisterRequest("T","F","test@example.invalid","+27000000000","é".repeat(37))).isEmpty());
 }}
}
