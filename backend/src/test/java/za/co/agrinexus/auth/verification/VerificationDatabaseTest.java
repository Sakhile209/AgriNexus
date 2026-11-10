package za.co.agrinexus.auth.verification;

import com.fasterxml.jackson.databind.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=${EMAIL_TEST_DB_URL}","spring.datasource.username=${EMAIL_TEST_DB_USER:agrinexus}","spring.datasource.password=${EMAIL_TEST_DB_PASSWORD:agrinexus-local}"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="EMAIL_TEST_DB_URL",matches=".*agrinexus_email_test_.*")
class VerificationDatabaseTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcClient db;@Autowired VerificationStore store;@Autowired TransactionTemplate tx;@Autowired DataSource datasource;@Autowired JwtEncoder jwt;
 @MockitoBean VerificationMail mail;
 @MockitoBean VerificationDeliveryService disabledScheduler;
 private String email;
 @BeforeEach void resetData()throws Exception{
  try(var c=datasource.getConnection()){assertTrue(c.getMetaData().getURL().contains("agrinexus_email_test_"),"Never run destructive test setup on a production database");}
  db.sql("DELETE FROM email_verification_delivery").update();db.sql("DELETE FROM email_verification_token").update();db.sql("DELETE FROM auth_request_limit").update();db.sql("DELETE FROM app_user").update();email="integration-"+UUID.randomUUID()+"@example.invalid";
 }
 private JsonNode postJson(String path,Object body,int expected)throws Exception{return json.readTree(mvc.perform(post("/api/v1/auth"+path).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString());}
 private Map<String,String> registration(){return Map.of("firstName","Test","lastName","Farmer","phoneNumber","+27000000000","email",email,"password","test-Password-12345");}
 private void register()throws Exception{var response=postJson("/register",registration(),202);assertTrue(response.path("verificationRequired").asBoolean());assertFalse(response.has("accessToken"));assertFalse(db.sql("SELECT email_verified FROM app_user WHERE email=:email").param("email",email).query(Boolean.class).single());}
 private String deliver(){var raw=new AtomicReference<String>();doAnswer(invocation->{raw.set(invocation.getArgument(1));return null;}).when(mail).send(anyString(),anyString());new VerificationDeliveryService(store,mail,tx).deliverPending();assertNotNull(raw.get());return raw.get();}
 private void cooldownPassed(){db.sql("UPDATE auth_request_limit SET last_request_at=last_request_at-INTERVAL '61 seconds'").update();}
 @Test void registrationVerificationAndProtectedLoginFlow()throws Exception{
  register();assertEquals("EMAIL_NOT_VERIFIED",postJson("/login",Map.of("email",email,"password","test-Password-12345"),403).path("code").asText());
  UUID id=db.sql("SELECT id FROM app_user WHERE email=:email").param("email",email).query(UUID.class).single();
  String signed=jwt.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),JwtClaimsSet.builder().issuer("agrinexus-api").subject(email).claim("userId",id.toString()).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build())).getTokenValue();
  mvc.perform(get("/api/v1/farms").header("Authorization","Bearer "+signed)).andExpect(status().isUnauthorized());
  String raw=deliver();String hash=db.sql("SELECT token_hash FROM email_verification_token WHERE user_id=:id").param("id",id).query(String.class).single();assertEquals(VerificationTokens.hash(raw),hash);assertNotEquals(raw,hash);
  assertEquals(0,db.sql("SELECT count(*) FROM email_verification_delivery").query(Integer.class).single());postJson("/verification/confirm",Map.of("token",raw),200);postJson("/verification/confirm",Map.of("token",raw),400);
  var login=postJson("/login",Map.of("email",email,"password","test-Password-12345"),200);assertTrue(login.hasNonNull("accessToken"));mvc.perform(get("/api/v1/farms").header("Authorization","Bearer "+login.path("accessToken").asText())).andExpect(status().isOk());
 }
 @Test void expiredAndInvalidTokens()throws Exception{register();String raw=deliver();db.sql("UPDATE email_verification_token SET expires_at=CURRENT_TIMESTAMP-INTERVAL '1 second'").update();assertEquals("VERIFICATION_EXPIRED",postJson("/verification/confirm",Map.of("token",raw),410).path("code").asText());postJson("/verification/confirm",Map.of("token",VerificationTokens.generate()),400);}
 @Test void resendCooldownPrivacyAndTokenRotation()throws Exception{
  register();String first=deliver();postJson("/verification/resend",Map.of("email",email),429);cooldownPassed();var known=postJson("/verification/resend",Map.of("email",email),200);var unknown=postJson("/verification/resend",Map.of("email","missing-"+email),200);assertEquals(known,unknown);String second=deliver();assertNotEquals(first,second);postJson("/verification/confirm",Map.of("token",first),400);postJson("/verification/confirm",Map.of("token",second),200);
 }
 @Test void concurrentReplayHasExactlyOneWinner()throws Exception{
  register();String raw=deliver();var pool=Executors.newFixedThreadPool(2);try{
   Callable<Integer> confirm=()->mvc.perform(post("/api/v1/auth/verification/confirm").contentType("application/json").content(json.writeValueAsString(Map.of("token",raw)))).andReturn().getResponse().getStatus();
   var results=pool.invokeAll(List.of(confirm,confirm));var codes=new ArrayList<Integer>();for(var result:results)codes.add(result.get());Collections.sort(codes);assertEquals(List.of(200,400),codes);
  }finally{pool.shutdownNow();}
 }
 @Test void deliveryFailureRetainsJobAndNeverEnablesLogin()throws Exception{
  register();doThrow(new IllegalStateException("Provider unavailable")).when(mail).send(anyString(),anyString());new VerificationDeliveryService(store,mail,tx).deliverPending();assertEquals("DELIVERY_FAILED",db.sql("SELECT last_error FROM email_verification_delivery").query(String.class).single());postJson("/login",Map.of("email",email,"password","test-Password-12345"),403);
 }
 @Test void duplicateRegistrationDoesNotOverwritePasswordOrCreateAnotherAccount()throws Exception{register();String hash=db.sql("SELECT password_hash FROM app_user WHERE email=:email").param("email",email).query(String.class).single();cooldownPassed();postJson("/register",registration(),202);assertEquals(1,db.sql("SELECT count(*) FROM app_user").query(Integer.class).single());assertEquals(hash,db.sql("SELECT password_hash FROM app_user WHERE email=:email").param("email",email).query(String.class).single());assertTrue(hash.startsWith("$2"));}
 @Test void concurrentRateLimitIsAtomic()throws Exception{
  var pool=Executors.newFixedThreadPool(8);try{List<Callable<Boolean>> requests=new ArrayList<>();for(int i=0;i<10;i++)requests.add(()->store.limit("test-bucket",5,3600,0));int allowed=0;for(var result:pool.invokeAll(requests))if(result.get())allowed++;assertEquals(5,allowed);}finally{pool.shutdownNow();}
 }
 @Test void migrationPreservesLegacyAccountsAndDefaultsNewAccountsToUnverified()throws Exception{
  String url=System.getenv("EMAIL_TEST_DB_URL"),user=System.getenv().getOrDefault("EMAIL_TEST_DB_USER","agrinexus"),password=System.getenv().getOrDefault("EMAIL_TEST_DB_PASSWORD","agrinexus-local"),schema="legacy_verification_test";
  Flyway.configure().dataSource(url,user,password).schemas(schema).target("7").load().migrate();
  try(var connection=java.sql.DriverManager.getConnection(url,user,password);var statement=connection.createStatement()){
   statement.execute("INSERT INTO "+schema+".app_user(id,first_name,last_name,email,phone_number,password_hash) VALUES('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','Legacy','Farmer','legacy@example.invalid','+27000000000','original-password-hash')");
   Flyway.configure().dataSource(url,user,password).schemas(schema).load().migrate();
   try(var result=statement.executeQuery("SELECT email_verified,email_verified_at,password_hash FROM "+schema+".app_user")){assertTrue(result.next());assertTrue(result.getBoolean(1));assertNull(result.getTimestamp(2));assertEquals("original-password-hash",result.getString(3));}
   statement.execute("INSERT INTO "+schema+".app_user(id,first_name,last_name,email,phone_number,password_hash) VALUES('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb','New','Farmer','new@example.invalid','+27000000000','new-password-hash')");
   try(var result=statement.executeQuery("SELECT email_verified FROM "+schema+".app_user WHERE email='new@example.invalid'")){assertTrue(result.next());assertFalse(result.getBoolean(1));}
  }
 }
}
