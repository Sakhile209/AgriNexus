package za.co.agrinexus.weather.service;

import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import za.co.agrinexus.farm.service.FarmService;
import za.co.agrinexus.integration.*;

@Service
public class WeatherService {
 private final FarmService farms; private final ProviderHttp http; private final ProviderCache cache; private final ObjectMapper json;
 private final String base,key;
 public WeatherService(FarmService farms,ProviderHttp http,ProviderCache cache,ObjectMapper json,
  @Value("${agrinexus.weather.base-url:https://api.open-meteo.com/v1/forecast}") String base,
  @Value("${OPEN_METEO_API_KEY:}") String key) {this.farms=farms;this.http=http;this.cache=cache;this.json=json;this.base=base;this.key=key;}
 public record Forecast(String date,Double minimumC,Double maximumC,Double rainfallMm,Integer rainProbability,Double windSpeedKph,Double evapotranspirationMm) {}
 public record Advisory(String type,String message,String origin) {}
 public record WeatherResponse(boolean available,Double temperatureC,Double minimumC,Double maximumC,Integer rainProbability,Double rainfallMm,Double humidityPercent,Double windSpeedKph,String provider,Instant retrievedAt,String location,String message,
  String status,String observedAt,Double windDirectionDegrees,Double currentRainfallMm,Integer weatherCode,List<Forecast> forecast,List<Advisory> advisories,Instant nextRefreshAt,String sourceUrl,boolean officialWarningsSupported) {}
 public WeatherResponse weather(String email,UUID farmId) {
  // Ownership must be checked before cache lookup, including for shared coordinates.
  var farm=farms.findOwned(email,farmId);
  String location=farm.getName()+" · "+farm.getProvince();
  if(farm.getLatitude()==null||farm.getLongitude()==null) return unavailable(location,"Save both farm GPS coordinates in Settings to retrieve weather.");
  String lat=farm.getLatitude().stripTrailingZeros().toPlainString(),lon=farm.getLongitude().stripTrailingZeros().toPlainString();
  var snapshot=cache.get("weather:v2:"+lat+":"+lon,Duration.ofMinutes(30),()->{
   var uri=UriComponentsBuilder.fromUriString(key.isBlank()?base:base.replace("api.open-meteo.com","customer-api.open-meteo.com"))
    .queryParam("latitude",lat).queryParam("longitude",lon)
    .queryParam("current","temperature_2m,relative_humidity_2m,wind_speed_10m,wind_direction_10m,precipitation,weather_code")
    .queryParam("daily","temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum,wind_speed_10m_max,et0_fao_evapotranspiration")
    .queryParam("timezone","Africa/Johannesburg").queryParam("forecast_days",7);
   if(!key.isBlank()) uri.queryParam("apikey",key);
   try {JsonNode p=json.readTree(http.text(uri.build().toUriString(),null));validate(p);return p;}
   catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalArgumentException("Invalid weather response",e);}
  });
  if(snapshot.data()==null) return new WeatherResponse(false,null,null,null,null,null,null,null,"Open-Meteo",null,location,snapshot.message(),"unavailable",null,null,null,null,List.of(),List.of(),snapshot.expiresAt(),"https://open-meteo.com/",false);
  JsonNode p=snapshot.data(),c=p.path("current"),d=p.path("daily");
  List<Forecast> forecast=new ArrayList<>();
  for(int i=0;i<d.path("time").size();i++) forecast.add(new Forecast(d.path("time").get(i).asText(),number(d.path("temperature_2m_min").path(i)),number(d.path("temperature_2m_max").path(i)),number(d.path("precipitation_sum").path(i)),integer(d.path("precipitation_probability_max").path(i)),number(d.path("wind_speed_10m_max").path(i)),number(d.path("et0_fao_evapotranspiration").path(i))));
  Forecast today=forecast.getFirst();
  String observed=c.path("time").asText();
  boolean stale=snapshot.stale()||LocalDateTime.parse(observed).atOffset(ZoneOffset.ofHours(2)).toInstant().isBefore(Instant.now().minus(Duration.ofHours(2)));
  return new WeatherResponse(true,number(c.path("temperature_2m")),today.minimumC(),today.maximumC(),today.rainProbability(),today.rainfallMm(),number(c.path("relative_humidity_2m")),number(c.path("wind_speed_10m")),"Open-Meteo",snapshot.retrievedAt(),location,stale?"Cached or outdated forecast. Check its observation time before planning.":null,stale?"stale":"current",observed,number(c.path("wind_direction_10m")),number(c.path("precipitation")),integer(c.path("weather_code")),forecast,advisories(forecast),snapshot.expiresAt(),"https://open-meteo.com/",false);
 }
 public static void validate(JsonNode p) {
  JsonNode c=p.path("current"),d=p.path("daily");
  range(c,"temperature_2m",-95,65);range(c,"relative_humidity_2m",0,100);range(c,"wind_speed_10m",0,500);range(c,"wind_direction_10m",0,360);range(c,"precipitation",0,2000);
  LocalDateTime.parse(c.path("time").asText());
  int count=d.path("time").size();
  if(count<1||count>16) throw new IllegalArgumentException("Missing forecast dates");
  for(String field:List.of("temperature_2m_min","temperature_2m_max","precipitation_probability_max","precipitation_sum","wind_speed_10m_max","et0_fao_evapotranspiration")) {
   if(!d.path(field).isArray()||d.path(field).size()!=count) throw new IllegalArgumentException("Incomplete forecast");
   for(JsonNode value:d.path(field)) if(!value.isNull()&&(!value.isNumber()||!Double.isFinite(value.asDouble())))throw new IllegalArgumentException("Invalid forecast value");
  }
  for(int i=0;i<count;i++) {
   LocalDate.parse(d.path("time").get(i).asText());
   Double probability=number(d.path("precipitation_probability_max").get(i));
   if(probability!=null&&(probability<0||probability>100)) throw new IllegalArgumentException("Invalid rain probability");
   for(String field:List.of("precipitation_sum","wind_speed_10m_max","et0_fao_evapotranspiration")) if(number(d.path(field).get(i))!=null&&number(d.path(field).get(i))<0) throw new IllegalArgumentException("Invalid forecast value");
  }
 }
 private static void range(JsonNode p,String name,double min,double max) {var v=p.path(name);if(!v.isNumber()||!Double.isFinite(v.asDouble())||v.asDouble()<min||v.asDouble()>max)throw new IllegalArgumentException("Invalid current weather");}
 private static Double number(JsonNode p) {return p!=null&&p.isNumber()?p.doubleValue():null;}
 private static Integer integer(JsonNode p) {return p!=null&&p.isNumber()?p.intValue():null;}
 public static List<Advisory> advisories(List<Forecast> days) {
  List<Advisory> out=new ArrayList<>();
  if(days.stream().anyMatch(d->d.rainfallMm()!=null&&d.rainfallMm()>=30)) out.add(new Advisory("heavy_rain","Forecast rainfall ≥30 mm/day. Check drainage and protect exposed soil.","AgriNexus advisory"));
  if(days.stream().anyMatch(d->d.maximumC()!=null&&d.maximumC()>=35)) out.add(new Advisory("heat","Forecast temperature ≥35°C. Check livestock water and heat stress.","AgriNexus advisory"));
  if(days.stream().anyMatch(d->d.minimumC()!=null&&d.minimumC()<=2)) out.add(new Advisory("frost","Forecast minimum ≤2°C. Sensitive crops may face frost risk.","AgriNexus advisory"));
  if(days.stream().anyMatch(d->d.windSpeedKph()!=null&&d.windSpeedKph()>=50)) out.add(new Advisory("wind","Forecast winds ≥50 km/h. Secure equipment and review spraying plans.","AgriNexus advisory"));
  if(days.size()>=7&&days.stream().allMatch(d->d.rainfallMm()!=null&&d.rainfallMm()<1)) out.add(new Advisory("dry_spell","Seven forecast days with <1 mm rain/day. Review water reserves; this is not a drought declaration.","AgriNexus advisory"));
  if(days.stream().limit(3).anyMatch(d->d.evapotranspirationMm()!=null&&d.rainfallMm()!=null&&d.evapotranspirationMm()-d.rainfallMm()>=4)) out.add(new Advisory("irrigation","Forecast reference evaporation exceeds rain by ≥4 mm/day. Check soil moisture and crop needs before irrigating.","AgriNexus advisory"));
  return out;
 }
 private WeatherResponse unavailable(String location,String message) {return new WeatherResponse(false,null,null,null,null,null,null,null,"Open-Meteo",null,location,message,"missing_location",null,null,null,null,List.of(),List.of(),null,"https://open-meteo.com/",false);}
}
