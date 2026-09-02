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
package za.co.agrinexus.reporting.web;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.agrinexus.reporting.service.DashboardService;
import za.co.agrinexus.reporting.service.ReportService;

@RestController
@RequestMapping(value={"/api/v1/farms/{farmId}"})
public class ReportingController {
    private final DashboardService dashboard;
    private final ReportService reports;

    public ReportingController(DashboardService d, ReportService r) {
        this.dashboard = d;
        this.reports = r;
    }

    @GetMapping(value={"/dashboard"})
    DashboardService.Dashboard dashboard(Authentication a, @PathVariable UUID farmId) {
        return this.dashboard.dashboard(a.getName(), farmId);
    }

    @GetMapping(value={"/reports/livestock"})
    ReportService.LivestockReport livestock(Authentication a, @PathVariable UUID farmId) {
        return this.reports.livestock(a.getName(), farmId);
    }

    @GetMapping(value={"/reports/health"})
    ReportService.HealthReport health(Authentication a, @PathVariable UUID farmId) {
        return this.reports.health(a.getName(), farmId);
    }

    @GetMapping(value={"/reports/crops"})
    ReportService.CropReport crops(Authentication a, @PathVariable UUID farmId) {
        return this.reports.crops(a.getName(), farmId);
    }
}
