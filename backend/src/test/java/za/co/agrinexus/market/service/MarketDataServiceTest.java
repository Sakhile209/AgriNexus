package za.co.agrinexus.market.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MarketDataServiceTest {
 @Test void derivesWeightedAverageAndRetainsActualPublication(){
  var html="This information is for 9 November 2026<table><tr><td>APPLES</td><td>R100.00<br>MTD: R900</td><td>5</td><td>20<br>MTD: 90</td><td>8</td></tr><tr><td>NO SALES</td><td>R0</td><td>0</td><td>0</td></tr></table>";
  var values=MarketDataService.parseJoburg(html);assertEquals(1,values.size());var p=values.getFirst();assertEquals("5.00",p.priceZar().toPlainString());assertEquals("2026-11-09",p.publishedOn().toString());assertEquals(MarketDataService.JOBURG,p.sourceUrl());assertEquals("kg (weighted average)",p.unit());
 }
 @Test void rejectsMissingDateAndBrokenMarketRows(){assertThrows(IllegalArgumentException.class,()->MarketDataService.parseJoburg("<table></table>"));assertThrows(IllegalArgumentException.class,()->MarketDataService.parseJoburg("This information is for 9 November 2026<table></table>"));}
 @Test void rejectsFeedsWithoutSourceCurrencyOrPublication()throws Exception{
  var json=new ObjectMapper();assertThrows(IllegalArgumentException.class,()->MarketDataService.parseFeed(json.readTree("[{\"priceZar\":123}]")));assertThrows(IllegalArgumentException.class,()->MarketDataService.parseFeed(json.readTree("[]")));
 }
}
