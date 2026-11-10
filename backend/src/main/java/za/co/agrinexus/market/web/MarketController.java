package za.co.agrinexus.market.web;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;
import org.springframework.http.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import za.co.agrinexus.market.service.MarketDataService;

@RestController
@RequestMapping("/api/v1")
public class MarketController {
 private final JdbcClient db;private final MarketDataService markets;
 public MarketController(JdbcClient db,MarketDataService markets){this.db=db;this.markets=markets;}
 @GetMapping("/market-prices") public List<MarketDataService.Price> prices(){return markets.latest().prices();}
 @GetMapping("/market-prices/status") public MarketDataService.Feed status(){return markets.latest();}
 @GetMapping("/market-prices/history") public List<MarketDataService.Price> history(@RequestParam String commodity,@RequestParam String market,@RequestParam String source,@RequestParam String unit,@RequestParam String province){return markets.history(commodity,market,source,unit,province);}
 private static final String COLUMNS="id,title,category,quantity,unit,price_zar,location,contact,created_at,province,updated_at,active,(photo IS NOT NULL) AS has_photo";
 @GetMapping("/marketplace/listings") public List<Listing> listings(@RequestParam(defaultValue="") String province,@RequestParam(defaultValue="") String q){
  return db.sql("SELECT "+COLUMNS+" FROM marketplace_listing WHERE active=TRUE AND seller_email<>'market@agrinexus.local' AND (:p='' OR province=:p) AND (LOWER(title) LIKE :q OR LOWER(category) LIKE :q) ORDER BY created_at DESC LIMIT 500").param("p",province).param("q","%"+q.toLowerCase(Locale.ROOT)+"%").query(this::row).list();
 }
 @GetMapping("/marketplace/mine") public List<Listing> mine(Authentication auth){return db.sql("SELECT "+COLUMNS+" FROM marketplace_listing WHERE seller_email=:email ORDER BY created_at DESC").param("email",auth.getName()).query(this::row).list();}
 private Listing row(java.sql.ResultSet rs,int n)throws java.sql.SQLException {return new Listing(rs.getObject("id",UUID.class),rs.getString("title"),rs.getString("category"),rs.getBigDecimal("quantity"),rs.getString("unit"),rs.getBigDecimal("price_zar"),rs.getString("location"),rs.getString("contact"),rs.getTimestamp("created_at").toInstant(),rs.getString("province"),rs.getTimestamp("updated_at").toInstant(),rs.getBoolean("active"),rs.getBoolean("has_photo"));}
 @PostMapping("/marketplace/listings") @Transactional public ResponseEntity<Listing> create(Authentication auth,@Valid @RequestBody ListingRequest request){
  UUID id=UUID.randomUUID();
  db.sql("INSERT INTO marketplace_listing(id,seller_email,title,category,quantity,unit,price_zar,location,contact,province) VALUES(:id,:seller,:title,:category,:quantity,:unit,:price,:location,:contact,:province)").param("id",id).param("seller",auth.getName()).param("title",request.title().trim()).param("category",request.category().trim()).param("quantity",request.quantity()).param("unit",request.unit().trim()).param("price",request.priceZar()).param("location",request.location().trim()).param("contact",request.contact().trim()).param("province",request.province()).update();
  return ResponseEntity.created(URI.create("/api/v1/marketplace/listings/"+id)).body(find(id));
 }
 @PatchMapping("/marketplace/listings/{id}") @Transactional public Listing update(Authentication auth,@PathVariable UUID id,@Valid @RequestBody ListingUpdate request){
  var p=request.listing();
  int count=db.sql("UPDATE marketplace_listing SET title=:title,category=:category,quantity=:qty,unit=:unit,price_zar=:price,location=:location,contact=:contact,province=:province,active=:active,updated_at=CURRENT_TIMESTAMP WHERE id=:id AND seller_email=:seller").param("title",p.title().trim()).param("category",p.category().trim()).param("qty",p.quantity()).param("unit",p.unit().trim()).param("price",p.priceZar()).param("location",p.location().trim()).param("contact",p.contact().trim()).param("province",p.province()).param("active",request.active()).param("id",id).param("seller",auth.getName()).update();
  if(count!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Listing not found");return find(id);
 }
 private Listing find(UUID id){return db.sql("SELECT "+COLUMNS+" FROM marketplace_listing WHERE id=:id").param("id",id).query(this::row).single();}
 @PostMapping("/marketplace/listings/{id}/photo") public Listing photo(Authentication auth,@PathVariable UUID id,@RequestParam("file") MultipartFile file)throws IOException {
  if(!db.sql("SELECT EXISTS(SELECT 1 FROM marketplace_listing WHERE id=:id AND seller_email=:seller)").param("id",id).param("seller",auth.getName()).query(Boolean.class).single())throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  if(file.isEmpty()||file.getSize()>2_000_000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a JPEG or PNG under 2 MB");
  byte[] bytes=file.getBytes();
  // Decode dimensions before allocating pixels, then re-encode to strip metadata.
  byte[] encoded;
  try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
   var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid image");
   var reader=readers.next();try{reader.setInput(input);String format=reader.getFormatName();
    if((!format.equalsIgnoreCase("JPEG")&&!format.equalsIgnoreCase("PNG"))||reader.getWidth(0)>4096||reader.getHeight(0)>4096)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use JPEG or PNG, up to 4096 pixels per side");
    var image=reader.read(0);var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);encoded=out.toByteArray();
    if(encoded.length>8_000_000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Image too large");
   }finally{reader.dispose();}
  }
  db.sql("UPDATE marketplace_listing SET photo=:photo,photo_type='image/png',updated_at=CURRENT_TIMESTAMP WHERE id=:id AND seller_email=:seller").param("photo",encoded).param("id",id).param("seller",auth.getName()).update();return find(id);
 }
 @GetMapping("/marketplace/listings/{id}/photo") public ResponseEntity<byte[]> photo(Authentication auth,@PathVariable UUID id){
  byte[] data=db.sql("SELECT photo FROM marketplace_listing WHERE id=:id AND photo IS NOT NULL AND (active=TRUE OR seller_email=:seller)").param("id",id).param("seller",auth.getName()).query((rs,n)->rs.getBytes(1)).optional().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).header("X-Content-Type-Options","nosniff").header("Cache-Control","private, max-age=300").body(data);
 }
 public record Listing(UUID id,String title,String category,BigDecimal quantity,String unit,BigDecimal priceZar,String location,String contact,Instant createdAt,String province,Instant updatedAt,boolean active,boolean hasPhoto){}
 public record ListingUpdate(@NotNull @Valid ListingRequest listing,boolean active){}
 public record ListingRequest(@NotBlank @Size(max=150) String title,@NotBlank @Size(max=80) String category,@NotNull @Positive BigDecimal quantity,@NotBlank @Size(max=40) String unit,@NotNull @PositiveOrZero BigDecimal priceZar,@NotBlank @Size(max=150) String location,@NotBlank @Size(max=120) String contact,
 @Pattern(regexp="Eastern Cape|Free State|Gauteng|KwaZulu-Natal|Limpopo|Mpumalanga|North West|Northern Cape|Western Cape") String province){}
}
