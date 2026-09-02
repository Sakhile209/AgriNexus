# AgriNexus
AgriNexus is a digital agriculture platform that helps farmers manage livestock, monitor crops, receive weather alerts, track market prices, and connect with buyers, suppliers, cooperatives, and government stakeholders.

## Recommended technology stack

| Layer | Technology |
| --- | --- |
| UI Design | Figma |
| Website | Next.js + React + Tailwind CSS |
| Mobile App | Flutter |
| Backend | Spring Boot |
| Database | PostgreSQL |
| Authentication | JWT + Spring Security |
| Charts | Recharts |
| Maps | Google Maps API |
| Weather | OpenWeather API |
| AI | OpenAI API |
| Version Control | Git + GitHub |
| Deployment | Azure |

## Run the current prototype locally

1. Install dependencies:
   `npm install`
2. Start the app:
   `npm start`
3. Open the frontend at:
   `http://localhost:3000/`

## Current React application hosts

The reconstructed React and Spring Boot application uses the following local hosts:

| Service | Host |
| --- | --- |
| React frontend | [http://localhost:5173](http://localhost:5173) |
| Spring Boot API | [http://localhost:8080](http://localhost:8080) |
| API status | [http://localhost:8080/api/v1/system/status](http://localhost:8080/api/v1/system/status) |
| Market prices API | [http://localhost:8080/api/v1/market-prices](http://localhost:8080/api/v1/market-prices) |
| Marketplace API | [http://localhost:8080/api/v1/marketplace/listings](http://localhost:8080/api/v1/marketplace/listings) |

To make the frontend accessible from other devices on the same network, bind it to all network interfaces:

```bash
cd frontend
npm run dev -- --host 0.0.0.0
```

Then open `http://<computer-ip-address>:5173` from the other device. Keep port `5173` private to trusted local networks; do not expose the development server directly to the public internet.

## Demo capabilities

- Farmer registration
- Farm management
- Livestock tracking
- Health records
- Crop monitoring
- Marketplace browsing
- AI assistant questions
- Alerts and notifications
