# AgriNexus live data integrations

The existing Spring Boot / PostgreSQL API and React dashboard remain in place. The dashboard extension sources are in `frontend/src`; `npm run sync:assets` copies them and the locally installed MapLibre library into the prebuilt frontend. `predev` and `prepreview` run this automatically. The original React source is absent from this checkout, so the existing compiled React bundle is retained.

## Run locally

From `frontend/`:

```bash
docker start agrinexus-database-1
cd ../backend
mvn test package
cd ../frontend
npm run api
```

In a second terminal:

```bash
npm ci
npm run dev
```

Open http://localhost:5174. The API runs on 8081 to avoid the unrelated service on 8080. Java 21+ is required. PostgreSQL connection settings remain `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. `AGRINEXUS_API_TARGET` overrides the frontend proxy target. Do not expose the preview server publicly.

Register or log in, select a farm, and save its latitude and longitude in Settings. Farm weather and the farm marker use these saved coordinates, never a geocoded municipality. The optional live GPS control can also display your device's current position. Existing farms without coordinates stay intact and display a location-required message. Coordinates accept valid global ranges (including South African offshore territories), require a complete pair, and must be entered accurately by the farmer; numerical validation cannot establish land ownership or boundary accuracy.

## Weather

Open-Meteo forecast API: https://open-meteo.com/en/docs

- Current forecast temperature, humidity, precipitation, wind speed/direction, weather code; seven daily forecasts, daily precipitation probabilities, and reference evapotranspiration.
- Provider observation time is distinct from the last successful retrieval time. Missing values are displayed as unavailable, not zero.
- Database snapshots persist across restarts and expire after 30 minutes. Failed requests retain the last good snapshot with a stale label. Requests have connection/response deadlines and bounded response size. Rate limits respect numeric Retry-After (60 seconds to 24 hours); other failures back off for five minutes.
- Refresh while viewing the dashboard follows `nextRefreshAt`; manual refresh respects the same backend cache/backoff, rather than bypassing provider limits. Forecast and current fields are fetched together. This is model forecast data, not a farm weather-station measurement.
- Threshold advisories: daily rain ≥30 mm, maximum ≥35°C, minimum ≤2°C, wind ≥50 km/h, seven dry forecast days (<1 mm/day), reference evapotranspiration minus rain ≥4 mm/day. These are planning prompts, not agronomic prescriptions or official drought/severe-weather declarations. Official warnings are not supplied by this connector; a SAWS link is shown.

The public Open-Meteo endpoint is for **noncommercial use** (currently 10,000 calls/day; no SLA). Commercial deployment requires a customer subscription and `OPEN_METEO_API_KEY` on the backend. The key selects the customer endpoint and is never returned to the browser. Override `AGRINEXUS_WEATHER_BASE_URL` for controlled provider testing/self-hosting. Weather data requires CC BY 4.0 attribution, shown in the UI. Review current terms before launch: https://open-meteo.com/en/pricing

OpenWeather is an alternative requiring an account/API key and product-specific plan; its warning coverage varies. SAWS offers official warnings and commercial data services, but a public unrestricted API/reuse entitlement was not established. Neither alternative is silently substituted.

## Verified reference market prices

**No licensed production price feed is configured by default.** Prices from the old seed migration are retained in the database but excluded from verified feeds. Demo seller listings are also retained and excluded from active seller results. No farmer records are deleted.

Implemented connectors:

1. **Joburg Market published daily report:** https://joburgmarket.co.za/jhb-market/dailyprices.php . The source says it updates between noon and 1pm on weekdays. The parser retains the report's actual publication date and derives a clearly labelled weighted average ZAR/kg: total value sold / total kilograms sold. MTD totals and zero-volume rows are excluded. This is not a variety/grade-specific quote or a real-time trade. The connector checks at most once per six-hour cache window (the backend scheduler checks for due work every 15 minutes). No documented public API or redistribution licence was verified. Enable only after obtaining applicable reuse permission: `MARKET_JOBURG_ENABLED=true` and `MARKET_DATA_LICENSE_CONFIRMED=true` on the backend. The HTML parser is deliberately fail-closed if the date/table format changes.
2. **Licensed JSON feed adapter:** set `MARKET_FEED_URL`, optional server-side bearer `MARKET_FEED_TOKEN`, and `MARKET_DATA_LICENSE_CONFIRMED=true`. This is an adapter contract, not an assertion that JSE or another provider exposes this format. Your authorised vendor gateway must supply the schema below. Fetching is cached for one hour; actual publication dates remain unchanged. Only trusted deployment administrators can configure the URL. Credentials are not exposed in responses.

```json
[
  {
    "commodity": "Provider commodity name",
    "market": "Provider market name / contract",
    "province": "National",
    "unit": "ton",
    "currency": "ZAR",
    "priceZar": 0,
    "publishedOn": "YYYY-MM-DD",
    "source": "Licensed provider name",
    "sourceUrl": "https://provider.example/publication"
  }
]
```

The schema illustration is not ingested or displayed as market data. Feed records require real dates (not future dates), nonnegative ZAR prices, provenance, and units. Futures contracts must be named separately by contract/expiry, and must not be presented as spot prices. Production adapters should also enforce each vendor's instrument identifiers and contractual entitlements.

Latest prices are separated by commodity, source, market, province, and unit. Changes compare the previous available publication for the same series. Daily snapshots accumulate up to a year of chartable history; the chart is empty until at least two publications exist. No synthetic history is generated. Filter by province, market, or commodity, click a commodity for an interactive chart, and use the slider or focus chart points to explore dates. Older publications and provider failures are labelled. Source links, publication dates, and retrieval timestamps are retained.

### Investigated sources and restrictions

| Source | Finding / integration decision |
| --- | --- |
| JSE / SAFEX | Derivatives market data is subject to data agreements, licensing, and possible fees. No free API assumed. Connect only through an entitled feed/vendor. https://www.jse.co.za/sites/default/files/media/documents/jse-market-data-policies/JSE%20Market%20Data%20Policies%20v5.0.pdf and https://marketdata.jse.co.za/ |
| SAGIS | Publishes grain delivery/trade statistics and weekly bulletins, not a verified unrestricted real-time commodity-price API. Weekly deliveries and bulletins have their own publication schedules. Linked as official information, not converted into invented prices. https://www.sagis.org.za/sagis-weekly-data/ and https://www.sagis.org.za/swb/ |
| Department of Agriculture / NAMC | Official reports and market intelligence are available; no documented unrestricted all-commodity price API established. Publication-specific permission and formats need confirmation. https://www.namc.co.za/ and https://www.elsenburg.com/programmes/agricultural-economic-services-agricultural-statistics-macro-economics-resource-economics-production-economics-marketing-and-agribusiness-support-agro-processing-support/marketing-agribusiness-support/market-information-2/ |
| ARC | Agroclimate portal / AgriCloud research services; external API access has historically required approval. Not a substitute for live spot prices. Confirm current access with ARC. https://www.agroclimate.agric.za/WP/WP/ |
| Joburg / Tshwane fresh produce markets | Public daily statistics pages exist; API availability and reuse rights are not established by public visibility. Joburg connector is gated behind explicit rights confirmation. https://joburgmarket.co.za/daily-price-list/ and https://tfpm.tshwane.gov.za/ViewDailyStats.aspx |

Livestock/grains and produce beyond the configured providers remain unavailable until licensed feeds are supplied. Current provider prices and contracts must be confirmed directly; there are no credentials or subscriptions bundled with this work.

## Farmer listings

Farmer listings stay in PostgreSQL and remain separate from market reference prices. The dashboard Market prices entry opens the marketplace panel.

- Authenticated users create/edit their listings; owner-scoped updates enforce seller identity on the database mutation.
- Title, category, quantity, unit, ZAR price, province, public pickup location, buyer contact, active status, created/updated timestamps.
- JPEG/PNG upload ≤2 MB and ≤4096 pixels per side; image decoding and re-encoding strips metadata, including EXIF GPS. Photos are stored in the database and served with authentication. Listing text/contact is public, as in the existing marketplace API; UI explains this before saving. Private farm GPS is never copied to listings.
- Active listings, seller contact, province/product search, and a My listings section for editing and changing availability. Seller contact is displayed; there is no new messaging/payment system or distance filter.
- Listings refresh every five minutes while the panel is open, and immediately after edits. Existing listing endpoints retain their response fields.

## Maps and location privacy

MapLibre GL JS (BSD-3-Clause) renders a real map centered on the selected farm. It is locally vendored from the locked npm dependency, with its licence included. Standard raster tiles use OpenStreetMap, with visible attribution and no offline/prefetch functionality. OSM tiles are best-effort and subject to usage limits: https://operations.osmfoundation.org/policies/tiles/ . Choose a commercial/self-hosted tile provider for high traffic. Map requests necessarily reveal the viewed area to the tile provider; coordinates are available only through authenticated, owner-scoped AgriNexus farm endpoints.

Satellite view requires `MAPTILER_API_KEY` on the backend and a suitable MapTiler plan. The server proxies satellite tiles after checking farm ownership; its key never reaches the browser. The satellite option is disabled without configuration. Confirm current imagery attribution/plan rights: https://www.maptiler.com/cloud/pricing/ and https://docs.maptiler.com/guides/developer/credentials/api-key/ . The free MapTiler plan is intended for testing/personal/noncommercial usage. Google Maps was not selected because it adds billing/account requirements and product-specific usage terms: https://mapsplatform.google.com/pricing/ .

Changing the selected farm disposes old map requests/markers/timers and fetches the newly selected farm's saved data. No random markers or simulated farm coordinates are drawn. GPS editing remains in the existing Settings form. Device location is never requested automatically.

## API and database changes

Migration `V7__live_integrations.sql` is additive. It creates `external_snapshot`, adds publication/provenance flags to `market_price`, and adds province, update time, image bytes/type to `marketplace_listing`. Existing prices are unverified by default and existing records are retained. Do not edit earlier applied migrations.

New/extended endpoints:

- `GET /api/v1/farms/{id}/weather`: owner-authorized forecast, cache/refresh metadata, advisories.
- `GET /api/v1/farms/{id}/map-config` and `/map-tiles/{z}/{x}/{y}`: owner-authorized map configuration/satellite proxy.
- `GET /api/v1/market-prices`: backward-compatible price list, verified data only.
- `GET /api/v1/market-prices/status`: authenticated source status and latest prices.
- `GET /api/v1/market-prices/history?commodity=...&market=...&source=...&unit=...&province=...`: authenticated exact-series history.
- `GET /api/v1/marketplace/listings?province=...&q=...`: active public seller offers.
- `GET /api/v1/marketplace/mine`: authenticated user's listings, including inactive offers.
- `POST /api/v1/marketplace/listings`: create an offer.
- `PATCH /api/v1/marketplace/listings/{id}`: `{ "listing": { listing fields }, "active": true }`, owner only.
- `POST /api/v1/marketplace/listings/{id}/photo`: multipart `file`, owner only.
- `GET /api/v1/marketplace/listings/{id}/photo`: authenticated active-listing photo or owner's inactive photo.

No API keys belong in frontend environment files or Git. Production deployment still needs normal HTTPS, database backups, external credential management, provider entitlements, and application-wide abuse controls. The cache serializes provider refreshes within an API process; multi-replica distributed refresh coordination and archival retention jobs are not included.

## Verification

```bash
# frontend
npm test
npm run sync:assets
# backend
cd ../backend
mvn clean test package
```

Tests cover provider HTTP normalization, validation, missing weather, forecast advisories, ownership checks before cache access, cache reuse/failure/rate-limit cooldown, market report parsing/provenance, coordinate validation, frontend refresh scheduling, escaping, and same-origin proxy behaviour. Runtime/browser results and any outstanding provider access limitations are reported in the implementation handoff.

### Verified locally (10 November 2026)

- Clean backend build: **14 tests passed**.
- Frontend suite: **4 tests passed**.
- Isolated Chromium workflow: **10 checks passed**, including selected-farm weather/map changes, manual and scheduled weather refresh, satellite configuration gate, unavailable market feed state, seller create/edit availability, mobile map visibility, and no browser exceptions.
- Live local API smoke run: **8 checks passed**, including actual Open-Meteo retrieval for saved Newcastle coordinates, cache reuse, anonymous/cross-owner access rejection, coordinate validation, seller/photo ownership, photo upload, availability, and seed exclusion. Only unique smoke-test accounts/listings were created, and they were removed after verification.
- V7 migration applied successfully to the existing PostgreSQL database; no existing farmer records were overwritten or removed.
- Compatible tooling updates: Vite 8.3.7 and patched transitive dependencies; `npm audit` reported **0 vulnerabilities**.
- Paid/licensed market and satellite connectors were not enabled or tested against paid accounts. No official-warning feed or multi-replica refresh coordination is claimed.

Optional repeatable runtime checks (running API and existing Docker database required):

```bash
python3 scripts/verify-api.py
```

For the isolated browser script, install Playwright in a temporary tooling folder and point at your installed Chromium. Its API fixtures are explicitly test-only and never written to production records:

```bash
npm install --prefix /tmp/agrinexus-browser --no-package-lock playwright
PLAYWRIGHT_MODULE=/tmp/agrinexus-browser/node_modules/playwright/index.mjs \
CHROMIUM_PATH=/absolute/path/to/chrome \
node scripts/verify-live.mjs
```

## Live device GPS

Click **Use live location** on the dashboard map and grant the browser's location permission. The blue device marker follows fresh device readings; the status panel shows coordinates, reported accuracy, and update time. Click **Stop live location** to clear the watch. Switching farm, leaving the dashboard, or logging out also stops tracking. No location request starts automatically.

While physically at the farm, click **Save GPS as farm location** to persist the displayed GPS position for the selected farm. This explicit action retains the latest other farm details, saves coordinates through the existing authenticated owner-scoped API, and refreshes the farm map and weather. Device movement alone never changes farm records.

Location requires localhost or HTTPS and browser/device permission. HTTP over a LAN IP may not support geolocation. A desktop may provide a less precise network-based fix rather than GPS; reported accuracy is displayed rather than claiming exact positioning. Denied permission, unsupported browsers, unavailable fixes, and timeouts have retryable error messages. Reference: https://developer.mozilla.org/en-US/docs/Web/API/Geolocation/watchPosition .

Live-GPS follow-up verification: **6 frontend tests and 14 isolated browser checks passed**, including device movement, recovery from a temporary location-service interruption, explicit GPS saving, weather refresh after saving, and stopping tracking. Browser GPS was simulated only inside the test context; no real farm coordinates were changed by the browser tests.

Satellite preference: when `MAPTILER_API_KEY` is configured, satellite is now the initial map view. Choosing Standard or Satellite saves only that layer preference locally and preserves it across farm switches and refreshes. When satellite is unavailable, the map falls back to Standard. To activate imagery, obtain a suitable MapTiler key/plan, stop the running API, export `MAPTILER_API_KEY` in the terminal that starts `npm run api`, restart it, and refresh the browser. Keys remain on the backend. No imagery entitlement is implied by a successful mocked browser test.

## Reports reference layout

The Reports workspace now follows the supplied reference: a green sidebar theme scoped to Reports, summary cards, Overview/Livestock/Crops/Animal Health/Financial tabs, species and vaccination doughnut charts, a registration trend, health records, crop overview, and selected-farm weather. Other workspaces retain their existing appearance. Sources are `src/reports.js` and `src/reports.css`, synchronized to `dist/` by the existing asset script.

Counts and tables come from the existing owner-authorized farm, animal, field, health, vaccination, crop, and weather APIs. No schema changes were needed. Current livestock/crop totals are distinguished from dated health/vaccination records. Date filters cover 3, 6, 12 months or all records. The livestock trend counts current animal registrations by their recorded creation dates, not inferred historical herd sizes. The newest recorded dose per animal/vaccine determines overdue/upcoming/completed schedule buckets; old superseded due dates are not double-counted. A clinical health status is not inferred from symptoms: rows show whether a vet was contacted or a record was made.

Downloads include category-specific CSV, with spreadsheet formula neutralization, and the browser's Print / Save PDF option. Financial totals are explicitly unavailable because the current application has no finance API. Source failures show a retryable message and unavailable count rather than a successful zero. Weather uses saved farm coordinates and the existing provider/cache metadata.

Validation: 11 frontend tests passed, including report ranges, schedule deduplication, empty data, CSV escaping, and category-specific exports. The isolated Reports browser script (`scripts/verify-reports.mjs`) checks layout/counts/charts, health/weather, filters/tabs, financial unavailable state, CSV downloads, partial failures, farm switching, mobile overflow, and Add Crop navigation. Browser fixture records never alter real farmer records.
