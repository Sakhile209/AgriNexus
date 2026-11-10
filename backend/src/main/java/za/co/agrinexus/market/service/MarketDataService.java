package za.co.agrinexus.market.service;
import com.fasterxml.jackson.databind.*;
import java.math.*;
import java.time.*;
import java.time.format.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.*;
import za.co.agrinexus.integration.*;

@Service
@EnableScheduling
public class MarketDataService {
 public static final String JOBURG="https://joburgmarket.co.za/jhb-market/dailyprices.php";
 private final ProviderHttp http;private final ProviderCache cache;private final ObjectMapper json;private final JdbcClient db;
 private final boolean enabled,licensed;private final String feed,token;
 public MarketDataService(ProviderHttp http,ProviderCache cache,ObjectMapper json,JdbcClient db,
 @Value("${MARKET_JOBURG_ENABLED:false}") boolean enabled,@Value("${MARKET_DATA_LICENSE_CONFIRMED:false}") boolean licensed,
 @Value("${MARKET_FEED_URL:}") String feed,@Value("${MARKET_FEED_TOKEN:}") String token){this.http=http;this.cache=cache;this.json=json;this.db=db;this.enabled=enabled;this.licensed=licensed;this.feed=feed;this.token=token;}
 public record Price(UUID id,String commodity,String market,String province,String unit,BigDecimal priceZar,String source,Instant capturedAt,LocalDate publishedOn,String sourceUrl,BigDecimal changePercent,boolean stale) {}
 public record Status(String source,String state,String message,Instant retrievedAt,Instant nextRefreshAt) {}
 public record Feed(List<Price> prices,List<Status> sources) {}
 @Scheduled(fixedDelay=900000,initialDelay=60000)
 public void refreshPublishedPrices(){ latest(); }
 public Feed latest() {
  List<Status> statuses=new ArrayList<>();
  if(enabled&&licensed) {
   var s=cache.get("market:joburg:v1",Duration.ofHours(6),()->{var records=parseJoburg(http.text(JOBURG,null));store(records);return json.valueToTree(records);});
   statuses.add(new Status("Joburg Market",s.data()==null?"unavailable":s.stale()?"stale":"available",s.message(),s.retrievedAt(),s.expiresAt()));
  } else statuses.add(new Status("Joburg Market","not_configured","Public daily report available. Automated ingestion requires confirmed reuse permission and MARKET_JOBURG_ENABLED.",null,null));
  if(!feed.isBlank()&&licensed) {
   var s=cache.get("market:licensed:v1",Duration.ofHours(1),()->{
    try{JsonNode p=json.readTree(http.text(feed,token));List<Price> records=parseFeed(p);store(records);return p;}
    catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalArgumentException("Invalid licensed feed",e);}
   });
   statuses.add(new Status("Licensed market feed",s.data()==null?"unavailable":s.stale()?"stale":"available",s.message(),s.retrievedAt(),s.expiresAt()));
  } else statuses.add(new Status("JSE / SAFEX and other licensed feeds","not_configured","No licensed feed configured. Grain, livestock, and other unavailable prices are not estimated.",null,null));
  List<Price> prices=db.sql("SELECT DISTINCT ON (commodity,market,province,unit,source) * FROM market_price WHERE verified=TRUE AND published_on IS NOT NULL ORDER BY commodity,market,province,unit,source,published_on DESC,captured_at DESC").query(this::row).list();
  if(statuses.stream().anyMatch(s->s.state().equals("stale")||s.state().equals("unavailable"))) prices=prices.stream().map(p->new Price(p.id(),p.commodity(),p.market(),p.province(),p.unit(),p.priceZar(),p.source(),p.capturedAt(),p.publishedOn(),p.sourceUrl(),p.changePercent(),true)).toList();
  return new Feed(prices,statuses);
 }
 private Price row(java.sql.ResultSet rs,int n)throws java.sql.SQLException{
  String commodity=rs.getString("commodity"),market=rs.getString("market"),source=rs.getString("source");LocalDate date=rs.getDate("published_on").toLocalDate();BigDecimal price=rs.getBigDecimal("price_zar");
  var previous=db.sql("SELECT price_zar FROM market_price WHERE verified=TRUE AND commodity=:c AND market=:m AND province=:p AND unit=:u AND source=:s AND published_on<:d ORDER BY published_on DESC LIMIT 1")
   .param("c",commodity).param("m",market).param("p",rs.getString("province")).param("u",rs.getString("unit")).param("s",source).param("d",date).query(BigDecimal.class).optional();
  BigDecimal change=previous.filter(v->v.signum()>0).map(v->price.subtract(v).multiply(BigDecimal.valueOf(100)).divide(v,2,RoundingMode.HALF_UP)).orElse(null);
  return new Price(rs.getObject("id",UUID.class),commodity,market,rs.getString("province"),rs.getString("unit"),price,source,rs.getTimestamp("captured_at").toInstant(),date,rs.getString("source_url"),change,date.isBefore(LocalDate.now(ZoneId.of("Africa/Johannesburg")).minusDays(1)));
 }
 public List<Price> history(String commodity,String market,String source,String unit,String province) {
  return db.sql("SELECT * FROM market_price WHERE verified=TRUE AND commodity=:c AND market=:m AND source=:s AND unit=:u AND province=:p AND published_on>=CURRENT_DATE-365 ORDER BY published_on LIMIT 366")
   .param("c",commodity).param("m",market).param("s",source).param("u",unit).param("p",province).query(this::row).list();
 }
 private void store(List<Price> records) {
  for(var p:records) db.sql("INSERT INTO market_price(id,commodity,market,province,unit,price_zar,source,captured_at,published_on,source_url,verified) VALUES(:id,:c,:m,:p,:u,:price,:s,:at,:date,:url,TRUE) ON CONFLICT(commodity,market,province,unit,source,published_on) DO UPDATE SET price_zar=EXCLUDED.price_zar,captured_at=EXCLUDED.captured_at,source_url=EXCLUDED.source_url")
   .param("id",UUID.randomUUID()).param("c",p.commodity()).param("m",p.market()).param("p",p.province()).param("u",p.unit()).param("price",p.priceZar()).param("s",p.source()).param("at",Timestamp.from(Instant.now())).param("date",p.publishedOn()).param("url",p.sourceUrl()).update();
 }
 public static List<Price> parseFeed(JsonNode payload) {
  if(!payload.isArray()||payload.size()<1||payload.size()>2000)throw new IllegalArgumentException("Expected price records");
  List<Price> out=new ArrayList<>();
  for(var p:payload){
   for(String f:List.of("commodity","market","province","unit","source","sourceUrl","publishedOn","currency")) if(!p.path(f).isTextual()||p.path(f).asText().isBlank())throw new IllegalArgumentException("Missing provenance or unit");
   if(!p.path("currency").asText().equals("ZAR")||!p.path("priceZar").isNumber()||p.path("priceZar").decimalValue().signum()<0)throw new IllegalArgumentException("Expected ZAR price");
   LocalDate date=LocalDate.parse(p.path("publishedOn").asText());
   if(date.isAfter(LocalDate.now(ZoneId.of("Africa/Johannesburg"))))throw new IllegalArgumentException("Future publication");
   if(!p.path("sourceUrl").asText().startsWith("https://"))throw new IllegalArgumentException("Invalid source URL");
   out.add(new Price(null,p.path("commodity").asText(),p.path("market").asText(),p.path("province").asText(),p.path("unit").asText(),p.path("priceZar").decimalValue(),p.path("source").asText(),null,date,p.path("sourceUrl").asText(),null,false));
  }return out;
 }
 public static List<Price> parseJoburg(String html) {
  String plain=html.replaceAll("(?is)<[^>]+>"," ").replace("&nbsp;"," ").replaceAll("\\s+"," ");
  Matcher dateMatcher=Pattern.compile("(?i)This information is for\\s+(\\d{1,2} [A-Za-z]+ \\d{4})").matcher(plain);
  if(!dateMatcher.find()) throw new IllegalArgumentException("Missing publication date");
  LocalDate date=LocalDate.parse(dateMatcher.group(1),DateTimeFormatter.ofPattern("d MMMM uuuu",Locale.ENGLISH));
  if(date.isAfter(LocalDate.now(ZoneId.of("Africa/Johannesburg"))))throw new IllegalArgumentException("Future publication");
  List<Price> result=new ArrayList<>();Matcher rows=Pattern.compile("(?is)<tr\\b[^>]*>(.*?)</tr>").matcher(html);
  while(rows.find()) {
   List<String> cells=new ArrayList<>();Matcher td=Pattern.compile("(?is)<td\\b[^>]*>(.*?)</td>").matcher(rows.group(1));
   while(td.find()) cells.add(td.group(1).replaceAll("(?is)<[^>]+>"," ").replace("&nbsp;"," ").split("(?i)MTD:")[0].trim());
   if(cells.size()<4||cells.get(0).isBlank()||cells.get(0).toLowerCase().contains("total"))continue;
   try{BigDecimal value=decimal(cells.get(1)),kg=decimal(cells.get(3));if(kg.signum()<=0||value.signum()<0)continue;
    result.add(new Price(null,cells.get(0),"Joburg Market","Gauteng","kg (weighted average)",value.divide(kg,2,RoundingMode.HALF_UP),"Joburg Market daily report",null,date,JOBURG,null,false));
   }catch(NumberFormatException ignored){}
  }
  if(result.isEmpty())throw new IllegalArgumentException("No valid market rows");return result;
 }
 private static BigDecimal decimal(String s){return new BigDecimal(s.replaceAll("[R,\\s]",""));}
}
