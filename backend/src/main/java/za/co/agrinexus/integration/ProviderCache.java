package za.co.agrinexus.integration;

import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.sql.Timestamp;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** Durable last-good snapshots; failures never overwrite usable data. */
@Service
public class ProviderCache {
 private final SnapshotRepository repository;
 private final ConcurrentHashMap<String, Instant> retries = new ConcurrentHashMap<>();
 public ProviderCache(SnapshotRepository repository) { this.repository=repository; }
 public record Snapshot(JsonNode data, Instant retrievedAt, Instant expiresAt, boolean stale, String message) {}
 public synchronized Snapshot get(String key, Duration ttl, Supplier<JsonNode> fetch) {
  Instant now=Instant.now();
  Snapshot old=repository.find(key);
  if(old!=null && old.expiresAt().isAfter(now)) return old;
  if(retries.getOrDefault(key,Instant.EPOCH).isAfter(now)) return fallback(old,"Provider temporarily unavailable or rate limited; retry is delayed.",retries.get(key));
  try {
   JsonNode data=fetch.get();
   if(data==null || data.isNull()) throw new IllegalArgumentException("Empty provider response");
   Instant at=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS), expires=at.plus(ttl);
   repository.save(key,new Snapshot(data,at,expires,false,null));
   retries.remove(key);
   return new Snapshot(data,at,expires,false,null);
  } catch(Exception e) {
   long seconds=e instanceof ProviderHttp.RateLimited r?r.seconds:300;
   retries.put(key,Instant.now().plusSeconds(seconds));
   return fallback(old,"Provider unavailable, rate limited, or returned invalid data. Retry after the displayed refresh time.",retries.get(key));
  }
 }
 private Snapshot fallback(Snapshot old,String message,Instant retryAt) { return new Snapshot(old==null?null:old.data(),old==null?null:old.retrievedAt(),retryAt,true,message); }
}
