package za.co.agrinexus.auth.verification;
public interface VerificationMail {
 void requireConfigured();
 void send(String email,String token);
}
