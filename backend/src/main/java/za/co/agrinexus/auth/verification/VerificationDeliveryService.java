package za.co.agrinexus.auth.verification;
import java.time.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
@Service
@EnableScheduling
public class VerificationDeliveryService {
 private final VerificationStore store;private final VerificationMail mail;private final TransactionTemplate transactions;
 public VerificationDeliveryService(VerificationStore store,VerificationMail mail,TransactionTemplate transactions){this.store=store;this.mail=mail;this.transactions=transactions;}
 private record Delivery(String email,String raw){}
 @Scheduled(fixedDelayString="${agrinexus.verification.poll-ms:5000}",initialDelay=5000)
 public void deliverPending(){try{mail.requireConfigured();}catch(VerificationProblem unavailable){return;}for(int i=0;i<10;i++){var claim=store.claim();if(claim==null)return;deliver(claim);}}
 public void deliver(VerificationStore.Claim claim){
  try{
   Delivery delivery=transactions.execute(status->{var account=store.lock(claim.userId());if(account==null||account.verified()||!account.status().equals("ACTIVE"))return null;
    String raw=VerificationTokens.generate();store.invalidate(account.id());store.insertToken(account.id(),VerificationTokens.hash(raw),Instant.now().plus(Duration.ofMinutes(15)));return new Delivery(account.email(),raw);
   });
   // Only the hash was committed. The raw token exists in memory and in the email, never the database/logs.
   if(delivery!=null)mail.send(delivery.email(),delivery.raw());store.sent(claim);
  }catch(RuntimeException ex){org.slf4j.LoggerFactory.getLogger(VerificationDeliveryService.class).warn("Verification email delivery failed; pending job retained for bounded retries.");store.failed(claim,Math.min(900,60L*(1L<<Math.min(claim.attempts()-1,4))));}
 }
}
