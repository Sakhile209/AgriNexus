package za.co.agrinexus.auth.verification;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class BrevoVerificationMailTest {
 @Test @SuppressWarnings("unchecked") void submitsToRealProviderContractWithoutLeakingCredentials()throws Exception{
  var http=mock(HttpClient.class);HttpResponse<String> response=mock(HttpResponse.class);when(response.statusCode()).thenReturn(201);when(response.body()).thenReturn("{\"messageId\":\"accepted-test-message\"}");when(http.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);
  new BrevoVerificationMail(new ObjectMapper(),"test-api-key","verify@agrinexus.example","https://agrinexus.example",http).send("farmer@example.invalid",VerificationTokens.generate());
  var request=ArgumentCaptor.forClass(HttpRequest.class);verify(http).send(request.capture(),any(HttpResponse.BodyHandler.class));assertEquals("https://api.brevo.com/v3/smtp/email",request.getValue().uri().toString());assertEquals("POST",request.getValue().method());assertEquals("test-api-key",request.getValue().headers().firstValue("api-key").orElseThrow());assertFalse(request.getValue().uri().toString().contains("test-api-key"));
 }
 @Test @SuppressWarnings("unchecked") void providerRejectionDoesNotExposeItsBodyOrSecrets()throws Exception{
  var http=mock(HttpClient.class);HttpResponse<String> response=mock(HttpResponse.class);when(response.statusCode()).thenReturn(429);when(response.body()).thenReturn("sensitive provider response");when(http.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);
  var e=assertThrows(IllegalStateException.class,()->new BrevoVerificationMail(new ObjectMapper(),"test-api-key","verify@agrinexus.example","https://agrinexus.example",http).send("farmer@example.invalid",VerificationTokens.generate()));assertEquals("Verification email delivery failed",e.getMessage());assertNull(e.getCause());
 }
 @Test void missingConfigurationFailsClosed(){assertThrows(VerificationProblem.class,()->new BrevoVerificationMail(new ObjectMapper(),"","","").requireConfigured());}
}
