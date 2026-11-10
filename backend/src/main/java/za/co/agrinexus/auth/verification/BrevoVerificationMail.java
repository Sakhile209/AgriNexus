package za.co.agrinexus.auth.verification;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service
public class BrevoVerificationMail implements VerificationMail {
 private final String key,sender,base;private final ObjectMapper json;
 private final HttpClient http;
 @org.springframework.beans.factory.annotation.Autowired
 public BrevoVerificationMail(ObjectMapper json,@Value("${BREVO_API_KEY:}")String key,@Value("${EMAIL_FROM:}")String sender,@Value("${VERIFICATION_BASE_URL:}")String base){this(json,key,sender,base,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build());}
 BrevoVerificationMail(ObjectMapper json,String key,String sender,String base,HttpClient http){this.json=json;this.key=key;this.sender=sender;this.base=base;this.http=http;validateUrl(base);}

 static void validateUrl(String base){
  if(base.isBlank())return;
  URI uri;try{uri=URI.create(base);}catch(Exception e){throw new IllegalArgumentException("Invalid VERIFICATION_BASE_URL");}
  boolean localhost=Set.of("localhost","127.0.0.1","[::1]").contains(String.valueOf(uri.getHost()));
  if(uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||(!"https".equals(uri.getScheme())&&!(localhost&&"http".equals(uri.getScheme()))))throw new IllegalArgumentException("VERIFICATION_BASE_URL must use HTTPS (HTTP is permitted only on localhost)");
 }
 public void requireConfigured(){if(key.isBlank()||sender.isBlank()||base.isBlank()||sender.chars().anyMatch(Character::isWhitespace)||!sender.matches("[^<>@]+@[^<>@]+[.][^<>@]+"))throw new VerificationProblem("EMAIL_UNAVAILABLE","Email verification is temporarily unavailable. Please try again later.",503);}

 public void send(String email,String token){
  requireConfigured();
  // Fragment keeps the raw token out of web-server access logs and HTTP Referer headers.
  String link=base.replaceAll("/+$","")+"/#verify-email="+token;
  Map<String,Object> body=Map.of("sender",Map.of("email",sender,"name","AgriNexus"),"to",List.of(Map.of("email",email)),"subject","Verify Your Email — AgriNexus","htmlContent",html(link),"textContent",text(link));
  try{
   var request=HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/smtp/email")).timeout(Duration.ofSeconds(20)).header("api-key",key).header("Content-Type","application/json").header("Accept","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
   var response=http.send(request,HttpResponse.BodyHandlers.ofString());
   if(response.statusCode()!=201||!json.readTree(response.body()).path("messageId").isTextual())throw new IllegalStateException("Provider did not accept email");
  }catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();throw new IllegalStateException("Verification email delivery failed");}
 }
 public static String text(String link){return "Welcome to AgriNexus!\n\nVerify your email to start managing your farm:\n"+link+"\n\nThis single-use link expires in 15 minutes. If you did not create this account, ignore this email. Your account will remain unverified.\n\nAgriNexus — Connected Agriculture";}
 public static String html(String link){String safe=link.replace("&","&amp;").replace("\"","&quot;").replace("<","&lt;").replace(">","&gt;").replace("'","&#39;");return "<!doctype html><html><body style='margin:0;background:#f3f8f3;font-family:Arial,sans-serif;color:#183727'><main style='max-width:560px;margin:32px auto;background:#fff;padding:36px;border-radius:12px'><h1 style='color:#117b38'>AgriNexus</h1><p style='color:#697e70'>Connected Agriculture</p><h2>Welcome to AgriNexus</h2><p>Thanks for joining our farming community. Please verify your email address to start managing your farm securely.</p><p style='margin:30px 0'><a style='display:inline-block;background:#138b40;color:#fff;padding:14px 24px;text-decoration:none;border-radius:7px' href='"+safe+"'>Verify email address</a></p><p>This single-use link expires in <strong>15 minutes</strong>.</p><p>If the button does not work, open this link:</p><p style='word-break:break-all'><a href='"+safe+"'>"+safe+"</a></p><hr style='border:0;border-top:1px solid #e2ece3'><p style='font-size:13px;color:#617466'>If you did not create an AgriNexus account, you can safely ignore this email. No action is required and the account will remain unverified.</p></main></body></html>";}
}
