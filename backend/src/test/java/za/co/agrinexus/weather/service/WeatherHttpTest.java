package za.co.agrinexus.weather.service;
import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import za.co.agrinexus.farm.model.Farm;
import za.co.agrinexus.farm.service.FarmService;
import za.co.agrinexus.integration.*;
class WeatherHttpTest {
 @Test void retrievesAndNormalizesForecastForSavedCoordinates()throws Exception{
  var json=new ObjectMapper();var p=json.createObjectNode();var current=p.putObject("current");
  current.put("time",LocalDateTime.now(ZoneId.of("Africa/Johannesburg")).withNano(0).toString());current.put("temperature_2m",23);current.put("relative_humidity_2m",65);current.put("wind_speed_10m",15);current.put("wind_direction_10m",180);current.put("precipitation",0.5);current.put("weather_code",3);
  var daily=p.putObject("daily");daily.putArray("time").add(LocalDate.now().toString());
  for(String field:new String[]{"temperature_2m_min","temperature_2m_max","precipitation_probability_max","precipitation_sum","wind_speed_10m_max","et0_fao_evapotranspiration"})daily.putArray(field).add(10);
  var request=new AtomicReference<String>();var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/forecast",exchange->{request.set(exchange.getRequestURI().getQuery());byte[] bytes=p.toString().getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();});server.start();
  try{
   var farms=mock(FarmService.class);var farm=mock(Farm.class);var id=UUID.randomUUID();when(farms.findOwned("owner",id)).thenReturn(farm);when(farm.getLatitude()).thenReturn(new BigDecimal("-27.75"));when(farm.getLongitude()).thenReturn(new BigDecimal("29.93"));when(farm.getProvince()).thenReturn("KwaZulu-Natal");when(farm.getName()).thenReturn("Test farm");
   var repo=mock(SnapshotRepository.class);var service=new WeatherService(farms,new ProviderHttp(),new ProviderCache(repo),json,"http://127.0.0.1:"+server.getAddress().getPort()+"/forecast","");
   var result=service.weather("owner",id);assertTrue(result.available());assertEquals(23,result.temperatureC());assertEquals(180,result.windDirectionDegrees());assertEquals(0.5,result.currentRainfallMm());assertEquals("current",result.status());assertFalse(result.officialWarningsSupported());assertTrue(request.get().contains("latitude=-27.75"));assertTrue(request.get().contains("longitude=29.93"));verify(repo).save(anyString(),any());
  }finally{server.stop(0);}
 }
}
