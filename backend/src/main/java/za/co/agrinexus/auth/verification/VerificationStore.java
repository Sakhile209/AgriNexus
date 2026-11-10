package za.co.agrinexus.auth.verification;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import za.co.agrinexus.auth.dto.RegisterRequest;
@Repository
public class VerificationStore {
 private final JdbcClient db;
 public VerificationStore(JdbcClient db){this.db=db;}
 public record Account(UUID id,String email,boolean verified,String status){}
 public record Token(UUID id,UUID userId,Instant expiresAt,Instant usedAt,Instant invalidatedAt){}
 public record Claim(UUID userId,UUID claimId,int generation,int attempts){}
 public Account account(String email){return db.sql("SELECT id,email,email_verified,account_status FROM app_user WHERE email=:email").param("email",email).query((rs,n)->new Account(rs.getObject("id",UUID.class),rs.getString("email"),rs.getBoolean("email_verified"),rs.getString("account_status"))).optional().orElse(null);}
 public Account lock(UUID id){return db.sql("SELECT id,email,email_verified,account_status FROM app_user WHERE id=:id FOR UPDATE").param("id",id).query((rs,n)->new Account(rs.getObject("id",UUID.class),rs.getString("email"),rs.getBoolean("email_verified"),rs.getString("account_status"))).optional().orElse(null);}
 public Account create(String email,RegisterRequest r,String passwordHash){
  db.sql("INSERT INTO app_user(id,first_name,last_name,email,phone_number,password_hash,email_verified) VALUES(:id,:first,:last,:email,:phone,:password,FALSE) ON CONFLICT(email) DO NOTHING")
   .param("id",UUID.randomUUID()).param("first",r.firstName().trim()).param("last",r.lastName().trim()).param("email",email).param("phone",r.phoneNumber().trim()).param("password",passwordHash).update();return account(email);
 }
 public void queue(UUID id){db.sql("INSERT INTO email_verification_delivery(user_id) VALUES(:id) ON CONFLICT(user_id) DO UPDATE SET requested_at=CURRENT_TIMESTAMP,next_attempt_at=CURRENT_TIMESTAMP,attempts=0,generation=email_verification_delivery.generation+1,last_error=NULL").param("id",id).update();}
 public void invalidate(UUID id){db.sql("UPDATE email_verification_token SET invalidated_at=CURRENT_TIMESTAMP WHERE user_id=:id AND used_at IS NULL AND invalidated_at IS NULL").param("id",id).update();}
 public void insertToken(UUID id,String hash,Instant expiresAt){db.sql("INSERT INTO email_verification_token(id,user_id,token_hash,expires_at) VALUES(:tokenId,:userId,:hash,:expires)").param("tokenId",UUID.randomUUID()).param("userId",id).param("hash",hash).param("expires",Timestamp.from(expiresAt)).update();}
 public Token token(String hash){return db.sql("SELECT id,user_id,expires_at,used_at,invalidated_at FROM email_verification_token WHERE token_hash=:hash").param("hash",hash).query((rs,n)->new Token(rs.getObject("id",UUID.class),rs.getObject("user_id",UUID.class),rs.getTimestamp("expires_at").toInstant(),instant(rs.getTimestamp("used_at")),instant(rs.getTimestamp("invalidated_at")))).optional().orElse(null);}
 public boolean consume(UUID id){return db.sql("UPDATE email_verification_token SET used_at=CURRENT_TIMESTAMP WHERE id=:id AND used_at IS NULL AND invalidated_at IS NULL AND expires_at>CURRENT_TIMESTAMP").param("id",id).update()==1;}
 public void verify(UUID id){db.sql("UPDATE app_user SET email_verified=TRUE,email_verified_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=:id").param("id",id).update();db.sql("DELETE FROM email_verification_delivery WHERE user_id=:id").param("id",id).update();}
 public Claim claim(){
  UUID claim=UUID.randomUUID();
  return db.sql("UPDATE email_verification_delivery SET claim_id=:claim,locked_until=CURRENT_TIMESTAMP+INTERVAL '90 seconds',attempts=attempts+1 WHERE user_id=(SELECT user_id FROM email_verification_delivery WHERE next_attempt_at<=CURRENT_TIMESTAMP AND attempts<5 AND (locked_until IS NULL OR locked_until<CURRENT_TIMESTAMP) ORDER BY requested_at FOR UPDATE SKIP LOCKED LIMIT 1) RETURNING user_id,generation,attempts")
   .param("claim",claim).query((rs,n)->new Claim(rs.getObject("user_id",UUID.class),claim,rs.getInt("generation"),rs.getInt("attempts"))).optional().orElse(null);
 }
 public void sent(Claim c){db.sql("DELETE FROM email_verification_delivery WHERE user_id=:id AND claim_id=:claim AND generation=:generation").param("id",c.userId()).param("claim",c.claimId()).param("generation",c.generation()).update();}
 public void failed(Claim c,long seconds){db.sql("UPDATE email_verification_delivery SET last_error='DELIVERY_FAILED',locked_until=NULL,next_attempt_at=:next WHERE user_id=:id AND claim_id=:claim AND generation=:generation").param("next",Timestamp.from(Instant.now().plusSeconds(seconds))).param("id",c.userId()).param("claim",c.claimId()).param("generation",c.generation()).update();}
 public boolean limit(String key,int count,int windowSeconds,int cooldownSeconds){
  return db.sql("INSERT INTO auth_request_limit(bucket_hash,window_started_at,last_request_at,request_count) VALUES(:key,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1) ON CONFLICT(bucket_hash) DO UPDATE SET request_count=CASE WHEN auth_request_limit.window_started_at<=CURRENT_TIMESTAMP-(:window * INTERVAL '1 second') THEN 1 ELSE auth_request_limit.request_count+1 END,window_started_at=CASE WHEN auth_request_limit.window_started_at<=CURRENT_TIMESTAMP-(:window * INTERVAL '1 second') THEN CURRENT_TIMESTAMP ELSE auth_request_limit.window_started_at END,last_request_at=GREATEST(auth_request_limit.last_request_at,CURRENT_TIMESTAMP) WHERE (auth_request_limit.window_started_at<=CURRENT_TIMESTAMP-(:window * INTERVAL '1 second') OR auth_request_limit.request_count<:count) AND (:cooldown=0 OR auth_request_limit.last_request_at<=CURRENT_TIMESTAMP-(:cooldown * INTERVAL '1 second')) RETURNING request_count")
   .param("key",VerificationTokens.hash(key)).param("window",windowSeconds).param("count",count).param("cooldown",cooldownSeconds).query(Integer.class).optional().isPresent();
 }
 private static Instant instant(Timestamp value){return value==null?null:value.toInstant();}
}
