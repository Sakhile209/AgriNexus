package za.co.agrinexus.auth.web;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.agrinexus.auth.dto.*;
import za.co.agrinexus.auth.service.AuthService;
import za.co.agrinexus.auth.verification.*;
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
 private final AuthService auth;private final EmailVerificationService verification;private final VerificationStore store;
 public AuthController(AuthService auth,EmailVerificationService verification,VerificationStore store){this.auth=auth;this.verification=verification;this.store=store;}
 private void limit(String key,int count,int window,int cooldown){if(!store.limit(key,count,window,cooldown))throw new VerificationProblem("RATE_LIMITED","Too many requests. Please wait before trying again.",429);}
 @PostMapping("/register") public ResponseEntity<EmailVerificationService.Pending> register(@Valid @RequestBody RegisterRequest r,HttpServletRequest request){
  limit("register-ip:"+request.getRemoteAddr(),20,3600,0);limit("verification-email:"+AuthService.normalizeEmail(r.email()),5,3600,60);
  return ResponseEntity.accepted().body(auth.register(r));
 }
 @PostMapping("/login") public AuthResponse login(@Valid @RequestBody LoginRequest r,HttpServletRequest request){limit("login-ip:"+request.getRemoteAddr(),30,900,0);limit("login-email:"+AuthService.normalizeEmail(r.email()),10,900,0);return auth.login(r);}
 @PostMapping("/verification/resend") public EmailVerificationService.Pending resend(@Valid @RequestBody ResendRequest r,HttpServletRequest request){
  limit("resend-ip:"+request.getRemoteAddr(),20,3600,0);limit("verification-email:"+AuthService.normalizeEmail(r.email()),5,3600,60);return verification.resend(r.email());
 }
 @PostMapping("/verification/confirm") public Map<String,String> confirm(@Valid @RequestBody VerifyRequest r,HttpServletRequest request){limit("verify-ip:"+request.getRemoteAddr(),60,300,0);verification.verify(r.token());return Map.of("message","Email verified successfully! You can now log in to AgriNexus.");}
 @PostMapping("/logout") public ResponseEntity<Void> logout(){return ResponseEntity.noContent().build();}
 public record ResendRequest(@NotBlank @Email @Size(max=254) String email){}
 public record VerifyRequest(@NotBlank @Size(max=200) String token){}
}
