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

## Scan to open the website

Connect your phone to the same Wi-Fi network as the development computer, then scan this QR code to open AgriNexus at [http://192.168.0.234:5173](http://192.168.0.234:5173):

![QR code for the AgriNexus website](frontend/dist/agrinexus-website-qr.png)

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

## Android and iOS mobile app

The cross-platform Expo React Native application is in `mobile/`. It supports Android and iOS from one TypeScript codebase and connects to the existing Spring API.

Copy the example environment file and replace the address with the development computer's LAN IP when using a physical phone:

```bash
cd mobile
cp .env.example .env
npm install
npm start
```

Install Expo Go on the Android or iOS device and scan the QR code shown by Expo. The phone and development computer must be on the same trusted network, and the Spring API must be listening on port `8080`.

### Run on physical Android and iPhone devices from Linux

The recommended command on this Linux computer is:

```bash
npm run device
```

Install Expo Go on the phone, connect the phone and computer to the same Wi-Fi network, then scan the displayed QR code. This works for both Android and iOS without installing Android Studio or Xcode.

### Run with platform simulators

```bash
# From the repository root:
npm run android
npm run ios
```

`npm run android` requires Android Studio, the Android SDK, and `adb`. Set `ANDROID_HOME` to the installed SDK directory before using it. `npm run ios` requires macOS and Xcode; Apple does not provide the iOS Simulator for Linux.

These root commands forward to the Expo project in `mobile/`. If your terminal is already inside `mobile/`, the corresponding commands work there directly.

Use `http://10.0.2.2:8080/api/v1` for the Android emulator, `http://localhost:8080/api/v1` for the iOS simulator, or `http://<computer-ip-address>:8080/api/v1` for a physical device. Production builds should use an HTTPS API address.

## Demo capabilities

- Farmer registration
- Farm management
- Livestock tracking
- Health records
- Crop monitoring
- Marketplace browsing
- AI assistant questions
- Alerts and notifications
