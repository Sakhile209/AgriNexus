package za.co.agrinexus.auth.verification;
public class VerificationProblem extends RuntimeException {
 public final String code;public final int status;
 public VerificationProblem(String code,String message,int status){super(message);this.code=code;this.status=status;}
}
