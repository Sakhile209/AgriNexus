package za.co.agrinexus.auth.verification;
import java.time.*;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.agrinexus.auth.dto.RegisterRequest;
import za.co.agrinexus.auth.service.AuthService;
@Service
public class EmailVerificationService {
 private final VerificationStore store;private final VerificationMail mail;
 public EmailVerificationService(VerificationStore store,VerificationMail mail){this.store=store;this.mail=mail;}
 public record Pending(String message,boolean verificationRequired,int resendAfterSeconds){}
 public static Pending registered(){return new Pending("Registration successful! Please check your email to verify your account.",true,60);}
 public static Pending resent(){return new Pending("If an unverified account exists for this email address, a verification email will be sent. Please check your inbox and spam folder.",true,60);}
 @Transactional public Pending register(RegisterRequest r,String hash){
  mail.requireConfigured();String email=AuthService.normalizeEmail(r.email());
  var existing=store.account(email);var account=existing==null?store.create(email,r,hash):existing;
  account=store.lock(account.id());if(!account.verified()&&account.status().equals("ACTIVE"))store.queue(account.id());return registered();
 }
 @Transactional public Pending resend(String email){mail.requireConfigured();var account=store.account(AuthService.normalizeEmail(email));if(account!=null){account=store.lock(account.id());if(!account.verified()&&account.status().equals("ACTIVE"))store.queue(account.id());}return resent();}
 @Transactional public void verify(String raw){
  if(!VerificationTokens.validFormat(raw))throw invalid();String hash=VerificationTokens.hash(raw);var token=store.token(hash);if(token==null)throw invalid();
  var account=store.lock(token.userId());token=store.token(hash);
  if(account==null||token==null||token.usedAt()!=null||token.invalidatedAt()!=null)throw invalid();
  if(!token.expiresAt().isAfter(Instant.now()))throw new VerificationProblem("VERIFICATION_EXPIRED","This verification link has expired. Request a new verification email.",410);
  if(account.verified()||!account.status().equals("ACTIVE")||!store.consume(token.id()))throw invalid();
  store.verify(account.id());store.invalidate(account.id());
 }
 private VerificationProblem invalid(){return new VerificationProblem("VERIFICATION_INVALID","This verification link is invalid or has already been used. Request a new verification email.",400);}
}
