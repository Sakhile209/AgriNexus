package za.co.agrinexus.farm.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record FarmRequest(@NotBlank @Size(max=150) String name,@NotBlank @Size(max=80) String farmType,
 @NotBlank @Pattern(regexp="Eastern Cape|Free State|Gauteng|KwaZulu-Natal|Limpopo|Mpumalanga|North West|Northern Cape|Western Cape",message="Select a South African province") String province,
 @Size(max=150) String municipality,
 @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
 @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
 @Positive BigDecimal sizeValue,@Size(max=20) String sizeUnit,@Size(max=500) String mainActivities) {
 @AssertTrue(message="Save both latitude and longitude, or leave both empty")
 public boolean isCoordinatePairValid(){return (latitude==null)==(longitude==null);}
}
