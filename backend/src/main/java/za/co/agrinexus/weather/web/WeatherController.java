/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.security.core.Authentication
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package za.co.agrinexus.weather.web;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.agrinexus.weather.service.WeatherService;

@RestController
@RequestMapping(value={"/api/v1/farms/{farmId}/weather"})
public class WeatherController {
    private final WeatherService service;

    public WeatherController(WeatherService s) {
        this.service = s;
    }

    @GetMapping
    WeatherService.WeatherResponse get(Authentication a, @PathVariable UUID farmId) {
        return this.service.weather(a.getName(), farmId);
    }
}
