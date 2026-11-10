package za.co.agrinexus.weather.service;
import com.fasterxml.jackson.databind.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import za.co.agrinexus.farm.model.Farm;
import za.co.agrinexus.farm.service.FarmService;
import za.co.agrinexus.integration.*;
class WeatherServiceTest {
 @Test void missingCoordinatesNeverFallsBackToMunicipality(){
  var farms=mock(FarmService.class);var farm=mock(Farm.class);var id=UUID.randomUUID();when(farms.findOwned("owner",id)).thenReturn(farm);var cache=mock(ProviderCache.class);var http=mock(ProviderHttp.class);
  var result=new WeatherService(farms,http,cache,new ObjectMapper(),"https://api.open-meteo.com/v1/forecast","").weather("owner",id);assertFalse(result.available());assertNull(result.retrievedAt());verifyNoInteractions(http,cache);
 }
 @Test void ownershipCheckedBeforeCacheAccess(){
  var farms=mock(FarmService.class);var id=UUID.randomUUID();when(farms.findOwned("intruder",id)).thenThrow(new IllegalArgumentException("Not owned"));var cache=mock(ProviderCache.class);var http=mock(ProviderHttp.class);
  assertThrows(IllegalArgumentException.class,()->new WeatherService(farms,http,cache,new ObjectMapper(),"https://api.open-meteo.com/v1/forecast","").weather("intruder",id));verifyNoInteractions(http,cache);
 }
 @Test void selectedCoordinatesDetermineCacheIdentity(){
  var farms=mock(FarmService.class);var cache=mock(ProviderCache.class);var http=mock(ProviderHttp.class);var farm=mock(Farm.class);var id=UUID.randomUUID();when(farms.findOwned("owner",id)).thenReturn(farm);when(farm.getLatitude()).thenReturn(new BigDecimal("-27.75"));when(farm.getLongitude()).thenReturn(new BigDecimal("29.93"));
  when(cache.get(anyString(),any(),any())).thenReturn(new ProviderCache.Snapshot(null,null,java.time.Instant.now(),true,"Unavailable"));var service=new WeatherService(farms,http,cache,new ObjectMapper(),"https://api.open-meteo.com/v1/forecast","");service.weather("owner",id);verify(cache).get(eq("weather:v2:-27.75:29.93"),any(),any());
 }
 @Test void rejectsMalformedAndMissingWeather()throws Exception{
  assertThrows(IllegalArgumentException.class,()->WeatherService.validate(new ObjectMapper().readTree("{}")));
  assertThrows(IllegalArgumentException.class,()->WeatherService.validate(new ObjectMapper().readTree("{\"current\":{\"temperature_2m\":999}}")));
 }
 @Test void advisoriesAreExplicitlyGeneratedAndCoverThresholds(){
  var day=new WeatherService.Forecast("2026-11-10",1.0,38.0,35.0,90,70.0,5.0);var alerts=WeatherService.advisories(List.of(day));assertEquals(4,alerts.size());assertTrue(alerts.stream().allMatch(a->a.origin().equals("AgriNexus advisory")));
  var dry=new WeatherService.Forecast("2026-11-10",12.0,28.0,0.0,0,10.0,5.0);var dryAlerts=WeatherService.advisories(Collections.nCopies(7,dry));assertTrue(dryAlerts.stream().anyMatch(a->a.type().equals("dry_spell")));assertTrue(dryAlerts.stream().anyMatch(a->a.type().equals("irrigation")));
 }
}
