package za.co.agrinexus.auth.verification;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.*;
import org.springframework.transaction.PlatformTransactionManager;
class VerificationDeliveryTest {
 @Test void commitsOnlyHashBeforeSendingAndRemovesSuccessfulJob(){var store=mock(VerificationStore.class);var mail=mock(VerificationMail.class);var tx=new TransactionTemplate(mock(PlatformTransactionManager.class));UUID id=UUID.randomUUID();var claim=new VerificationStore.Claim(id,UUID.randomUUID(),1,1);when(store.lock(id)).thenReturn(new VerificationStore.Account(id,"test@example.invalid",false,"ACTIVE"));new VerificationDeliveryService(store,mail,tx).deliver(claim);var raw=ArgumentCaptor.forClass(String.class);verify(mail).send(eq("test@example.invalid"),raw.capture());var hash=ArgumentCaptor.forClass(String.class);var expiry=ArgumentCaptor.forClass(Instant.class);verify(store).insertToken(eq(id),hash.capture(),expiry.capture());assertEquals(VerificationTokens.hash(raw.getValue()),hash.getValue());assertTrue(expiry.getValue().isAfter(Instant.now().plusSeconds(890)));verify(store).sent(claim);}
 @Test void failedDeliveryRemainsQueuedWithBackoff(){var store=mock(VerificationStore.class);var mail=mock(VerificationMail.class);UUID id=UUID.randomUUID();var claim=new VerificationStore.Claim(id,UUID.randomUUID(),1,2);when(store.lock(id)).thenReturn(new VerificationStore.Account(id,"test@example.invalid",false,"ACTIVE"));doThrow(new IllegalStateException("Test outage")).when(mail).send(anyString(),anyString());new VerificationDeliveryService(store,mail,new TransactionTemplate(mock(PlatformTransactionManager.class))).deliver(claim);verify(store).failed(claim,120);verify(store,never()).sent(any());}
}
