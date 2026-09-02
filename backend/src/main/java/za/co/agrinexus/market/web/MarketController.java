package za.co.agrinexus.market.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class MarketController {
    private final JdbcClient db;

    public MarketController(JdbcClient db) { this.db = db; }

    @GetMapping("/market-prices")
    public List<MarketPrice> prices() {
        return db.sql("SELECT id, commodity, market, province, unit, price_zar, source, captured_at FROM market_price ORDER BY captured_at DESC, commodity")
                .query((rs, row) -> new MarketPrice(rs.getObject("id", UUID.class), rs.getString("commodity"), rs.getString("market"), rs.getString("province"), rs.getString("unit"), rs.getBigDecimal("price_zar"), rs.getString("source"), rs.getTimestamp("captured_at").toInstant())).list();
    }

    @GetMapping("/marketplace/listings")
    public List<Listing> listings() {
        return db.sql("SELECT id, title, category, quantity, unit, price_zar, location, contact, created_at FROM marketplace_listing WHERE active = TRUE ORDER BY created_at DESC")
                .query((rs, row) -> new Listing(rs.getObject("id", UUID.class), rs.getString("title"), rs.getString("category"), rs.getBigDecimal("quantity"), rs.getString("unit"), rs.getBigDecimal("price_zar"), rs.getString("location"), rs.getString("contact"), rs.getTimestamp("created_at").toInstant())).list();
    }

    @PostMapping("/marketplace/listings")
    @Transactional
    public ResponseEntity<Listing> create(Authentication auth, @Valid @RequestBody ListingRequest request) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        db.sql("INSERT INTO marketplace_listing(id, seller_email, title, category, quantity, unit, price_zar, location, contact, created_at) VALUES (:id,:seller,:title,:category,:quantity,:unit,:price,:location,:contact,:created)")
                .param("id", id).param("seller", auth.getName()).param("title", request.title().trim()).param("category", request.category().trim()).param("quantity", request.quantity()).param("unit", request.unit().trim()).param("price", request.priceZar()).param("location", request.location().trim()).param("contact", request.contact().trim()).param("created", Timestamp.from(now)).update();
        Listing listing = new Listing(id, request.title().trim(), request.category().trim(), request.quantity(), request.unit().trim(), request.priceZar(), request.location().trim(), request.contact().trim(), now);
        return ResponseEntity.created(URI.create("/api/v1/marketplace/listings/" + id)).body(listing);
    }

    public record MarketPrice(UUID id, String commodity, String market, String province, String unit, BigDecimal priceZar, String source, Instant capturedAt) {}
    public record Listing(UUID id, String title, String category, BigDecimal quantity, String unit, BigDecimal priceZar, String location, String contact, Instant createdAt) {}
    public record ListingRequest(@NotBlank @Size(max=150) String title, @NotBlank @Size(max=80) String category, @NotNull @Positive BigDecimal quantity, @NotBlank @Size(max=40) String unit, @NotNull @PositiveOrZero BigDecimal priceZar, @NotBlank @Size(max=150) String location, @NotBlank @Size(max=120) String contact) {}
}
