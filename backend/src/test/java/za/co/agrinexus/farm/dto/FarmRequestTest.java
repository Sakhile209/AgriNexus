package za.co.agrinexus.farm.dto;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FarmRequestTest {
 @Test void validatesRangesProvinceAndPairs(){try(var factory=Validation.buildDefaultValidatorFactory()){
  var validator=factory.getValidator();
  assertTrue(validator.validate(request("KwaZulu-Natal","-27.75","29.93")).isEmpty());
  assertFalse(validator.validate(request("KwaZulu-Natal","91","29.93")).isEmpty());
  assertFalse(validator.validate(request("KwaZulu-Natal","-27.75","181")).isEmpty());
  assertFalse(validator.validate(request("Invalid","-27.75","29.93")).isEmpty());
  assertFalse(validator.validate(request("KwaZulu-Natal","-27.75",null)).isEmpty());
  assertTrue(validator.validate(request("KwaZulu-Natal",null,null)).isEmpty());
 }}
 private FarmRequest request(String province,String lat,String lon){return new FarmRequest("Farm","Mixed",province,"Newcastle",lat==null?null:new BigDecimal(lat),lon==null?null:new BigDecimal(lon),null,null,null);}
}
