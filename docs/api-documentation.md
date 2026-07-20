# AgriNexus API Documentation

## Authentication
- POST /api/auth/register
- POST /api/auth/login
- POST /api/auth/refresh

## Farmers
- GET /api/farmers/{id}
- POST /api/farmers
- PUT /api/farmers/{id}
- GET /api/farmers/{id}/farms

## Farms and Livestock
- GET /api/farms
- POST /api/farms
- GET /api/farms/{id}/livestock
- POST /api/livestock
- POST /api/vaccinations
- POST /api/animal-health-records

## Crops and Weather
- GET /api/crops
- POST /api/crops
- GET /api/weather/forecast
- GET /api/market-prices

## Marketplace
- GET /api/products
- POST /api/products
- GET /api/orders
- POST /api/orders
- GET /api/transactions

## Notifications and Reports
- GET /api/notifications
- POST /api/notifications
- GET /api/reports

## Response Conventions
- Standard JSON success envelope
- Error responses use consistent status codes and error codes
- All write endpoints require authentication
