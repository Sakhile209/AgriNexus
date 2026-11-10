package za.co.agrinexus.integration;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ProviderCacheTest {
 private final ObjectMapper json=new ObjectMapper();
 @Test void reusesFreshSnapshotWithoutNetwork()throws Exception{
  var repo=mock(SnapshotRepository.class);var now=Instant.now();var saved=new ProviderCache.Snapshot(json.readTree("{\"ok\":true}"),now,now.plusSeconds(1800),false,null);when(repo.find("farm")).thenReturn(saved);
  var result=new ProviderCache(repo).get("farm",Duration.ofMinutes(30),()->{fail("Network should not run");return null;});assertSame(saved,result);verify(repo,never()).save(anyString(),any());
 }
 @Test void keepsLastGoodDataAndBacksOffAfterFailure()throws Exception{
  var repo=mock(SnapshotRepository.class);var at=Instant.now().minusSeconds(3600);var saved=new ProviderCache.Snapshot(json.readTree("{\"temperature\":22}"),at,at.plusSeconds(1800),false,null);when(repo.find("farm")).thenReturn(saved);
  var cache=new ProviderCache(repo);var result=cache.get("farm",Duration.ofMinutes(30),()->{throw new IllegalStateException();});assertTrue(result.stale());assertEquals(at,result.retrievedAt());assertEquals(22,result.data().path("temperature").asInt());
  cache.get("farm",Duration.ofMinutes(30),()->{fail("Backoff should suppress request");return null;});verify(repo,never()).save(anyString(),any());
 }
 @Test void handlesNoPreviousDataAndRateLimit(){
  var repo=mock(SnapshotRepository.class);var cache=new ProviderCache(repo);var result=cache.get("key",Duration.ofHours(1),()->{throw new ProviderHttp.RateLimited(3600);});assertNull(result.data());assertNull(result.retrievedAt());assertTrue(result.expiresAt().isAfter(Instant.now().plusSeconds(3500)));
 }
 @Test void storesOnlySuccessfulPayloads()throws Exception{
  var repo=mock(SnapshotRepository.class);var cache=new ProviderCache(repo);var payload=json.readTree("{\"ok\":true}");var result=cache.get("key",Duration.ofHours(1),()->payload);assertFalse(result.stale());verify(repo).save(eq("key"),any());
 }
}
