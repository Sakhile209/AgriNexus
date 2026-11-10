package za.co.agrinexus.farm.web;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import za.co.agrinexus.farm.service.FarmService;
import za.co.agrinexus.integration.ProviderHttp;
@RestController
@RequestMapping("/api/v1/farms/{id}")
public class FarmMapController {
 private final FarmService farms;private final ProviderHttp http;private final String key;
 public FarmMapController(FarmService farms,ProviderHttp http,@Value("${MAPTILER_API_KEY:}")String key){this.farms=farms;this.http=http;this.key=key;}
 @GetMapping("/map-config") public Map<String,Object> config(Authentication auth,@PathVariable UUID id){
  var f=farms.findOwned(auth.getName(),id);
  return Map.of("satelliteAvailable",!key.isBlank(),"standardTiles","https://tile.openstreetmap.org/{z}/{x}/{y}.png","satelliteTiles","/api/v1/farms/"+id+"/map-tiles/{z}/{x}/{y}","attribution","© OpenStreetMap contributors","satelliteAttribution","© MapTiler © Airbus DS © OpenStreetMap contributors","hasCoordinates",f.getLatitude()!=null&&f.getLongitude()!=null);
 }
 @GetMapping("/map-tiles/{z}/{x}/{y}") public ResponseEntity<byte[]> tile(Authentication auth,@PathVariable UUID id,@PathVariable int z,@PathVariable int x,@PathVariable int y){
  farms.findOwned(auth.getName(),id);
  if(key.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Satellite imagery has not been configured");
  if(z<0||z>20||x<0||y<0||x>=(1<<z)||y>=(1<<z))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  try{return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).cacheControl(CacheControl.maxAge(java.time.Duration.ofHours(1)).cachePrivate()).body(http.bytes("https://api.maptiler.com/tiles/satellite-v2/"+z+"/"+x+"/"+y+".jpg?key="+java.net.URLEncoder.encode(key,java.nio.charset.StandardCharsets.UTF_8),null));}
  catch(RuntimeException ex){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Satellite tile unavailable");}
 }
}
