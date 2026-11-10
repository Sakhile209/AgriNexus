package za.co.agrinexus.integration;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
@Repository
public class SnapshotRepository {
 private final JdbcClient db;private final ObjectMapper json;
 public SnapshotRepository(JdbcClient db,ObjectMapper json){this.db=db;this.json=json;}
 public ProviderCache.Snapshot find(String key){
  return db.sql("SELECT * FROM external_snapshot WHERE cache_key=:key").param("key",key).query((rs,n)->{
   try{return new ProviderCache.Snapshot(json.readTree(rs.getString("payload")),rs.getTimestamp("retrieved_at").toInstant(),rs.getTimestamp("expires_at").toInstant(),false,null);}catch(java.io.IOException e){throw new IllegalStateException("Unreadable snapshot",e);}
  }).optional().orElse(null);
 }
 public void save(String key,ProviderCache.Snapshot value){
  db.sql("INSERT INTO external_snapshot(cache_key,payload,retrieved_at,expires_at) VALUES(:key,:payload,:at,:expires) ON CONFLICT(cache_key) DO UPDATE SET payload=EXCLUDED.payload,retrieved_at=EXCLUDED.retrieved_at,expires_at=EXCLUDED.expires_at")
   .param("key",key).param("payload",value.data().toString()).param("at",Timestamp.from(value.retrievedAt())).param("expires",Timestamp.from(value.expiresAt())).update();
 }
}
