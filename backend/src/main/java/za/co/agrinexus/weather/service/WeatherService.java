/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Service
 *  org.springframework.web.client.RestClient
 *  org.springframework.web.client.RestClient$Builder
 *  org.springframework.web.util.UriComponentsBuilder
 */
package za.co.agrinexus.weather.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import za.co.agrinexus.farm.model.Farm;
import za.co.agrinexus.farm.service.FarmService;

@Service
public class WeatherService {
    private final FarmService farms;
    private final RestClient client;
    private final String baseUrl;
    private final String geocodingBaseUrl;

    public WeatherService(
            FarmService farms,
            RestClient.Builder builder,
            @Value("${agrinexus.weather.base-url:https://api.open-meteo.com/v1/forecast}") String base,
            @Value("${agrinexus.weather.geocoding-base-url:https://geocoding-api.open-meteo.com/v1/search}") String geocodingBase) {
        this.farms = farms;
        this.client = builder.build();
        this.baseUrl = base;
        this.geocodingBaseUrl = geocodingBase;
    }

    public WeatherResponse weather(String email, UUID farmId) {
        Farm f = this.farms.findOwned(email, farmId);
        try {
            Coordinates coordinates = this.resolveCoordinates(f);
            if (coordinates == null) {
                return WeatherResponse.unavailable(
                        "Weather location could not be found for the selected municipality.",
                        this.locationLabel(f));
            }
            String uri = UriComponentsBuilder.fromUriString(this.baseUrl)
                    .queryParam("latitude", coordinates.latitude())
                    .queryParam("longitude", coordinates.longitude())
                    .queryParam("current", "temperature_2m,relative_humidity_2m,wind_speed_10m")
                    .queryParam("daily", "temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum")
                    .queryParam("timezone", "auto")
                    .queryParam("forecast_days", 1)
                    .build().toUriString();
            ProviderResponse p = (ProviderResponse)this.client.get().uri(uri, new Object[0]).retrieve().body(ProviderResponse.class);
            if (p == null || p.current() == null) {
                return WeatherResponse.unavailable("Weather information is temporarily unavailable.", coordinates.label());
            }
            Daily d = p.daily();
            return new WeatherResponse(true, p.current().temperature_2m(), this.first(d == null ? null : d.temperature_2m_min()), this.first(d == null ? null : d.temperature_2m_max()), this.firstInt(d == null ? null : d.precipitation_probability_max()), this.first(d == null ? null : d.precipitation_sum()), p.current().relative_humidity_2m(), p.current().wind_speed_10m(), "Open-Meteo", Instant.now(), coordinates.label(), null);
        }
        catch (RuntimeException ex) {
            return WeatherResponse.unavailable("Live weather is temporarily unavailable.", this.locationLabel(f));
        }
    }

    private Coordinates resolveCoordinates(Farm farm) {
        if (farm.getLatitude() != null && farm.getLongitude() != null) {
            return new Coordinates(farm.getLatitude(), farm.getLongitude(), this.locationLabel(farm));
        }
        if (farm.getMunicipality() == null || farm.getMunicipality().isBlank()) {
            return null;
        }

        String place = farm.getMunicipality()
                .replaceFirst("(?i)\\s+(Local|District|Metropolitan) Municipality$", "")
                .replaceFirst("(?i)\\s+Municipality$", "")
                .trim();
        String uri = UriComponentsBuilder.fromUriString(this.geocodingBaseUrl)
                .queryParam("name", place + ", " + farm.getProvince())
                .queryParam("countryCode", "ZA")
                .queryParam("count", 10)
                .queryParam("language", "en")
                .build().encode().toUriString();
        GeocodingResponse response = this.client.get().uri(uri).retrieve().body(GeocodingResponse.class);
        if (response == null || response.results() == null || response.results().isEmpty()) {
            return null;
        }
        GeocodingResult result = response.results().stream()
                .filter(candidate -> farm.getProvince().equalsIgnoreCase(candidate.admin1()))
                .findFirst()
                .orElse(response.results().getFirst());
        return new Coordinates(
                BigDecimal.valueOf(result.latitude()),
                BigDecimal.valueOf(result.longitude()),
                this.locationLabel(farm));
    }

    private String locationLabel(Farm farm) {
        if (farm.getMunicipality() == null || farm.getMunicipality().isBlank()) {
            return farm.getProvince();
        }
        return farm.getMunicipality() + ", " + farm.getProvince();
    }

    private Double first(List<Double> x) {
        return x == null || x.isEmpty() ? null : x.getFirst();
    }

    private Integer firstInt(List<Integer> x) {
        return x == null || x.isEmpty() ? null : x.getFirst();
    }

    public record WeatherResponse(boolean available, Double temperatureC, Double minimumC, Double maximumC, Integer rainProbability, Double rainfallMm, Double humidityPercent, Double windSpeedKph, String provider, Instant retrievedAt, String location, String message) {
        static WeatherResponse unavailable(String message, String location) {
            return new WeatherResponse(false, null, null, null, null, null, null, null, "Open-Meteo", Instant.now(), location, message);
        }
    }

    private record ProviderResponse(Current current, Daily daily) {
    }

    private record Current(Double temperature_2m, Double relative_humidity_2m, Double wind_speed_10m) {
    }

    private record Daily(List<Double> temperature_2m_max, List<Double> temperature_2m_min, List<Integer> precipitation_probability_max, List<Double> precipitation_sum) {
    }

    private record Coordinates(BigDecimal latitude, BigDecimal longitude, String label) {
    }

    private record GeocodingResponse(List<GeocodingResult> results) {
    }

    private record GeocodingResult(Double latitude, Double longitude, String admin1) {
    }
}
