package za.co.agrinexus.integration;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import org.springframework.stereotype.Component;
@Component
public class ProviderHttp {
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
 public static class RateLimited extends RuntimeException {
  public final long seconds;
  RateLimited(long seconds) { this.seconds=seconds; }
 }
 public byte[] bytes(String url,String token) {
  try {
   var builder=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(12)).header("User-Agent","AgriNexus/0.1 (agricultural data integration)").GET();
   if(token!=null&&!token.isBlank()) builder.header("Authorization","Bearer "+token);
   var response=client.send(builder.build(),HttpResponse.BodyHandlers.ofInputStream());
   try(var body=response.body()) {
    if(response.statusCode()==429) {
     long delay=300;
     try{delay=Math.max(60,Math.min(86400,Long.parseLong(response.headers().firstValue("Retry-After").orElse("300"))));}catch(NumberFormatException ignored){}
     throw new RateLimited(delay);
    }
    if(response.statusCode()!=200) throw new IllegalStateException("Provider request failed");
    byte[] data=body.readNBytes(5_000_001);
    if(data.length>5_000_000) throw new IllegalStateException("Provider response too large");
    return data;
   }
  } catch(RateLimited e){throw e;}catch(Exception e){throw new IllegalStateException("Provider request failed",e);}
 }
 public String text(String url,String token) { return new String(bytes(url,token),java.nio.charset.StandardCharsets.UTF_8); }
}
