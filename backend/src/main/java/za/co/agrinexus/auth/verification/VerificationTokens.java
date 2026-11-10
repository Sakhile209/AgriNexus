package za.co.agrinexus.auth.verification;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class VerificationTokens {
 private static final SecureRandom random=new SecureRandom();
 private VerificationTokens(){}
 public static String generate(){byte[] bytes=new byte[32];random.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
 public static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 unavailable");}}
 public static boolean validFormat(String value){return value!=null&&value.matches("[A-Za-z0-9_-]{43}");}
}
