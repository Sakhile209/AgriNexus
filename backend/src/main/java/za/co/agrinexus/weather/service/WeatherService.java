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

    public WeatherService(FarmService farms, RestClient.Builder builder, @Value(value="${agrinexus.weather.base-url:https://api.open-meteo.com/v1/forecast}") String base) {
        this.farms = farms;
        this.client = builder.build();
        this.baseUrl = base;
    }

    public WeatherResponse weather(String email, UUID farmId) {
        Farm f = this.farms.findOwned(email, farmId);
        if (f.getLatitude() == null || f.getLongitude() == null) {
            return WeatherResponse.unavailable("Add an approximate farm location to view weather.");
        }
        try {
            String uri = UriComponentsBuilder.fromUriString((String)this.baseUrl).queryParam("latitude", new Object[]{f.getLatitude()}).queryParam("longitude", new Object[]{f.getLongitude()}).queryParam("current", new Object[]{"temperature_2m,relative_humidity_2m,wind_speed_10m"}).queryParam("daily", new Object[]{"temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum"}).queryParam("timezone", new Object[]{"auto"}).build().toUriString();
            ProviderResponse p = (ProviderResponse)this.client.get().uri(uri, new Object[0]).retrieve().body(ProviderResponse.class);
            if (p == null || p.current() == null) {
                return WeatherResponse.unavailable("Weather information is temporarily unavailable.");
            }
            Daily d = p.daily();
            return new WeatherResponse(true, p.current().temperature_2m(), this.first(d == null ? null : d.temperature_2m_min()), this.first(d == null ? null : d.temperature_2m_max()), this.firstInt(d == null ? null : d.precipitation_probability_max()), this.first(d == null ? null : d.precipitation_sum()), p.current().relative_humidity_2m(), p.current().wind_speed_10m(), "Open-Meteo", Instant.now(), null);
        }
        catch (RuntimeException ex) {
            return WeatherResponse.unavailable("Weather information is temporarily unavailable.");
        }
    }

    private Double first(List<Double> x) {
        return x == null || x.isEmpty() ? null : x.getFirst();
    }

    private Integer firstInt(List<Integer> x) {
        return x == null || x.isEmpty() ? null : x.getFirst();
    }

    public record WeatherResponse(boolean available, Double temperatureC, Double minimumC, Double maximumC, Integer rainProbability, Double rainfallMm, Double humidityPercent, Double windSpeedKph, String provider, Instant retrievedAt, String message) {
        static WeatherResponse unavailable(String m) {
            return new WeatherResponse(false, null, null, null, null, null, null, null, "Open-Meteo", Instant.now(), m);
        }
    }

    private record ProviderResponse(Current current, Daily daily) {
    }

    private record Current(Double temperature_2m, Double relative_humidity_2m, Double wind_speed_10m) {
    }

    private record Daily(List<Double> temperature_2m_max, List<Double> temperature_2m_min, List<Integer> precipitation_probability_max, List<Double> precipitation_sum) {
    }
}
