# Local AgriNexus setup

Start the existing database container:

```bash
docker start agrinexus-database-1
```

From this directory, run the API and frontend in separate terminals:

```bash
npm run api
```

```bash
npm run dev
```

Open http://localhost:5174/. The API uses port 8081 to avoid the other application on port 8080. The frontend proxies `/api` and `/actuator` to that API.

To connect to an AgriNexus API at a different address, set `AGRINEXUS_API_TARGET` when starting the frontend:

```bash
AGRINEXUS_API_TARGET=http://localhost:8082 npm run dev
```

The API command requires Java and the existing backend JAR in `../backend/target/`.

See [LIVE_DATA.md](LIVE_DATA.md) for weather, maps, seller listings, source licensing, configuration, and verification instructions.
