const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { URL } = require('url');

const rootDir = __dirname;
const port = process.env.PORT || 3000;

function hashPassword(value) {
  return crypto.createHash('sha256').update(value).digest('hex');
}

function serializeUser(user) {
  const payload = { ...user };
  delete payload.passwordHash;
  return payload;
}

function buildDashboard() {
  return {
    totalLivestock: state.livestock.length,
    totalCrops: state.crops.length,
    upcomingVaccinations: 3,
    weatherAlerts: state.notifications.filter((item) => /weather/i.test(item.title)).length,
    marketPriceUpdates: state.products.length,
    recentFarmActivities: state.activities.slice(-3)
  };
}

function normalizeEmail(value) {
  return (value || '').trim().toLowerCase();
}

function getFarmerFarms(userEmail) {
  const normalizedEmail = normalizeEmail(userEmail);
  return state.farms.filter((item) => normalizeEmail(item.ownerEmail) === normalizedEmail);
}

function buildFarmerProfile(user) {
  const farm = state.farms.find((item) => normalizeEmail(item.ownerEmail) === normalizeEmail(user.email)) || null;
  return {
    user: serializeUser(user),
    farm,
    farms: getFarmerFarms(user.email),
    dashboard: buildDashboard(),
    notificationPrefs: user.notificationPrefs || { email: true, sms: true, push: false },
    farmingActivities: user.farmingActivities || ''
  };
}

const state = {
  users: [
    {
      id: 1,
      firstName: 'Lerato',
      lastName: 'Mokoena',
      name: 'Lerato Mokoena',
      email: 'lerato@example.com',
      phone: '+27 82 555 1234',
      role: 'farmer',
      passwordHash: hashPassword('password123'),
      farmName: 'Mokopane Orchard',
      farmLocation: 'Limpopo, Capricorn',
      province: 'Limpopo',
      district: 'Capricorn',
      gpsCoordinates: '24.12, 29.45',
      farmingType: 'Mixed Farming',
      farmSizeHa: 24,
      profilePhoto: 'lerato.jpg',
      emailVerified: true,
      accountStatus: 'Active',
      notificationPrefs: { email: true, sms: true, push: false },
      createdAt: '2026-01-10'
    }
  ],
  farms: [
    {
      id: 1,
      name: 'Mokopane Orchard',
      location: 'Limpopo, Capricorn',
      sizeHa: 24,
      owner: 'Lerato Mokoena',
      ownerEmail: 'lerato@example.com',
      gpsCoordinates: '24.12, 29.45',
      farmingType: 'Mixed Farming'
    }
  ],
  livestock: [
    { id: 1, name: 'Cattle herd A', type: 'Cattle', status: 'Healthy', farm: 'Mokopane Orchard' }
  ],
  healthRecords: [
    { id: 1, animal: 'Cattle herd A', condition: 'Vaccination due', severity: 'Medium' }
  ],
  crops: [
    { id: 1, name: 'Maize', stage: 'Flowering', farm: 'Mokopane Orchard' }
  ],
  products: [
    { id: 1, name: 'Fresh maize', price: 42, seller: 'Lerato Mokoena', category: 'Crops' },
    { id: 2, name: 'Livestock feed', price: 180, seller: 'AgriCoop', category: 'Inputs' }
  ],
  orders: [],
  notifications: [
    { id: 1, title: 'Vaccination reminder', body: 'Cattle herd A needs a vaccine check this week.' },
    { id: 2, title: 'Weather alert', body: 'Rain expected across the eastern belt tomorrow.' }
  ],
  activities: [
    { id: 1, title: 'Vaccination reminder', detail: 'Cattle herd A scheduled for review' },
    { id: 2, title: 'Crop inspection', detail: 'Maize field checked after rainfall' },
    { id: 3, title: 'Market update', detail: 'Fresh produce price posted for local buyers' }
  ]
};

function sendJson(res, statusCode, payload) {
  res.writeHead(statusCode, { 'Content-Type': 'application/json; charset=utf-8' });
  res.end(JSON.stringify(payload));
}

function titleCase(value) {
  return value
    .split(/\s+/)
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
    .join(' ');
}

async function getWeatherData(location) {
  const requestedLocation = (location || 'Limpopo').toString().trim();
  const apiKey = process.env.OPENWEATHER_API_KEY;

  if (apiKey) {
    try {
      const url = new URL('https://api.openweathermap.org/data/2.5/weather');
      url.searchParams.set('q', requestedLocation);
      url.searchParams.set('units', 'metric');
      url.searchParams.set('appid', apiKey);

      const response = await fetch(url);
      if (response.ok) {
        const data = await response.json();
        const condition = data.weather?.[0]?.description || 'Clear skies';
        const temperatureC = Math.round(data.main?.temp ?? 0);
        const chanceOfRain = Math.max(0, Math.min(100, Math.round((data.clouds?.all ?? 40) * 0.8)));
        return [{
          alert: `Weather update: ${titleCase(condition)}`,
          temperatureC,
          chanceOfRain,
          region: data.name || requestedLocation,
          condition,
          humidity: data.main?.humidity ?? 0,
          windSpeedKph: Math.round((data.wind?.speed ?? 0) * 3.6),
          source: 'OpenWeather'
        }];
      }
    } catch (error) {
      console.error('Weather lookup failed', error.message);
    }
  }

  const fallbackProfiles = {
    limpopo: { temperatureC: 23, chanceOfRain: 72, condition: 'showers', region: 'Limpopo' },
    gauteng: { temperatureC: 20, chanceOfRain: 40, condition: 'partly cloudy', region: 'Gauteng' },
    westerncape: { temperatureC: 18, chanceOfRain: 35, condition: 'clear skies', region: 'Western Cape' },
    kwazulu: { temperatureC: 22, chanceOfRain: 65, condition: 'cloudy', region: 'KwaZulu-Natal' },
    default: { temperatureC: 21, chanceOfRain: 48, condition: 'pleasant weather', region: requestedLocation }
  };

  const key = requestedLocation.toLowerCase();
  const profile = fallbackProfiles[key] || fallbackProfiles.default;

  return [{
    alert: `Farmer weather outlook for ${profile.region}`,
    temperatureC: profile.temperatureC,
    chanceOfRain: profile.chanceOfRain,
    region: profile.region,
    condition: titleCase(profile.condition),
    humidity: 58,
    windSpeedKph: 18,
    source: 'Local sample'
  }];
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let body = '';
    req.on('data', (chunk) => {
      body += chunk;
    });
    req.on('end', () => {
      if (!body) {
        resolve({});
        return;
      }
      try {
        resolve(JSON.parse(body));
      } catch (error) {
        reject(error);
      }
    });
    req.on('error', reject);
  });
}

function serveStaticFile(res, filePath) {
  const extension = path.extname(filePath).toLowerCase();
  const contentTypes = {
    '.html': 'text/html; charset=utf-8',
    '.css': 'text/css; charset=utf-8',
    '.js': 'application/javascript; charset=utf-8',
    '.svg': 'image/svg+xml',
    '.json': 'application/json; charset=utf-8',
    '.md': 'text/markdown; charset=utf-8',
    '.txt': 'text/plain; charset=utf-8',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.jpeg': 'image/jpeg',
    '.ico': 'image/x-icon'
  };

  const fullPath = path.join(rootDir, filePath);
  fs.readFile(fullPath, (error, content) => {
    if (error) {
      res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('Not found');
      return;
    }
    res.writeHead(200, { 'Content-Type': contentTypes[extension] || 'application/octet-stream' });
    res.end(content);
  });
}

function createServer() {
  return http.createServer(async (req, res) => {
    const requestUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
    const pathname = decodeURIComponent(requestUrl.pathname);

    if (pathname === '/' || pathname === '/index.html') {
      serveStaticFile(res, 'index.html');
      return;
    }

    if (pathname === '/register.html') {
      serveStaticFile(res, 'register.html');
      return;
    }

    if (pathname === '/styles.css' || pathname === '/script.js' || pathname.startsWith('/public/') || pathname.startsWith('/docs/')) {
      const relativePath = pathname.replace(/^\//, '');
      serveStaticFile(res, relativePath);
      return;
    }

    if (pathname.startsWith('/api/')) {
      const apiPath = pathname.replace(/^\/api/, '');
      if (apiPath === '/health') {
        sendJson(res, 200, { status: 'ok', message: 'AgriNexus backend is running' });
        return;
      }

      if (apiPath === '/dashboard-summary') {
        sendJson(res, 200, {
          farmers: state.users.length,
          farms: state.farms.length,
          livestock: state.livestock.length,
          orders: state.orders.length,
          alerts: state.notifications.length
        });
        return;
      }

      if (apiPath === '/products') {
        sendJson(res, 200, state.products);
        return;
      }

      if (apiPath === '/notifications') {
        sendJson(res, 200, state.notifications);
        return;
      }

      if (apiPath === '/weather') {
        const location = requestUrl.searchParams.get('location');
        const weather = await getWeatherData(location);
        sendJson(res, 200, weather);
        return;
      }

      if (apiPath === '/auth/register') {
        if (req.method !== 'POST') {
          sendJson(res, 405, { success: false, message: 'Method not allowed' });
          return;
        }
        try {
          const payload = await readBody(req);
          const email = normalizeEmail(payload.email);
          const firstName = (payload.firstName || payload.name || '').trim();
          const lastName = (payload.lastName || '').trim();
          if (!email || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
            sendJson(res, 400, { success: false, message: 'Please provide a valid email address' });
            return;
          }
          if (!firstName || !lastName) {
            sendJson(res, 400, { success: false, message: 'First and last name are required' });
            return;
          }
          if (state.users.some((item) => normalizeEmail(item.email) === email)) {
            sendJson(res, 400, { success: false, message: 'Email already exists' });
            return;
          }

          const password = payload.password ?? payload.confirmPassword ?? 'agri123';
          const confirmPassword = payload.confirmPassword ?? payload.password ?? 'agri123';
          if (password.length < 6) {
            sendJson(res, 400, { success: false, message: 'Password must be at least 6 characters' });
            return;
          }
          if (password !== confirmPassword) {
            sendJson(res, 400, { success: false, message: 'Passwords do not match' });
            return;
          }

          const user = {
            id: state.users.length + 1,
            firstName,
            lastName,
            name: `${firstName} ${lastName}`.trim(),
            email,
            phone: payload.phone || '',
            role: payload.role || 'farmer',
            passwordHash: hashPassword(password),
            farmName: payload.farmName || 'New Farm',
            farmLocation: payload.farmLocation || payload.location || 'Unknown',
            farmingActivities: payload.farmingActivities || '',
            province: payload.province || 'Unknown',
            district: payload.district || 'Unknown',
            gpsCoordinates: payload.gpsCoordinates || '',
            farmingType: payload.farmingType || 'Mixed Farming',
            farmSizeHa: Number(payload.farmSizeHa || 0),
            profilePhoto: payload.profilePhoto || 'default.jpg',
            emailVerified: false,
            accountStatus: 'Pending Review',
            notificationPrefs: payload.notificationPrefs || { email: true, sms: true, push: false },
            createdAt: payload.createdAt || 'Today'
          };

          state.users.push(user);
          state.farms.push({
            id: state.farms.length + 1,
            name: user.farmName,
            location: user.farmLocation,
            sizeHa: user.farmSizeHa,
            owner: user.name,
            ownerEmail: user.email,
            gpsCoordinates: user.gpsCoordinates,
            farmingType: user.farmingType
          });
          state.activities.push({
            id: state.activities.length + 1,
            title: 'Profile created',
            detail: `${user.name} registered their farm profile`
          });

          sendJson(res, 200, {
            success: true,
            user: serializeUser(user),
            profile: buildFarmerProfile(user),
            token: `jwt-demo-${user.id}`
          });
        } catch (error) {
          sendJson(res, 400, { success: false, message: 'Invalid JSON' });
        }
        return;
      }

      if (apiPath === '/auth/login') {
        if (req.method !== 'POST') {
          sendJson(res, 405, { success: false, message: 'Method not allowed' });
          return;
        }
        try {
          const payload = await readBody(req);
          const email = normalizeEmail(payload.email);
          const password = payload.password || '';
          if (!email || !password) {
            sendJson(res, 400, { success: false, message: 'Email and password are required' });
            return;
          }
          const user = state.users.find((item) => normalizeEmail(item.email) === email);
          if (!user) {
            sendJson(res, 404, { success: false, message: 'User not found' });
            return;
          }
          if (user.passwordHash !== hashPassword(password)) {
            sendJson(res, 401, { success: false, message: 'Invalid password' });
            return;
          }
          sendJson(res, 200, {
            success: true,
            user: serializeUser(user),
            profile: buildFarmerProfile(user),
            token: `jwt-demo-${user.id}`
          });
        } catch (error) {
          sendJson(res, 400, { success: false, message: 'Invalid JSON' });
        }
        return;
      }

      if (apiPath === '/auth/forgot-password') {
        if (req.method !== 'POST') {
          sendJson(res, 405, { success: false, message: 'Method not allowed' });
          return;
        }
        try {
          const payload = await readBody(req);
          const email = (payload.email || '').trim().toLowerCase();
          const user = state.users.find((item) => item.email.toLowerCase() === email);
          if (!user) {
            sendJson(res, 404, { success: false, message: 'No account found' });
            return;
          }
          sendJson(res, 200, { success: true, message: 'Password reset instructions sent to your email.' });
        } catch (error) {
          sendJson(res, 400, { success: false, message: 'Invalid JSON' });
        }
        return;
      }

      if (apiPath === '/auth/verify-email') {
        if (req.method !== 'POST') {
          sendJson(res, 405, { success: false, message: 'Method not allowed' });
          return;
        }
        try {
          const payload = await readBody(req);
          const email = (payload.email || '').trim().toLowerCase();
          const user = state.users.find((item) => item.email.toLowerCase() === email);
          if (!user) {
            sendJson(res, 404, { success: false, message: 'User not found' });
            return;
          }
          user.emailVerified = true;
          user.accountStatus = 'Active';
          sendJson(res, 200, { success: true, user: serializeUser(user) });
        } catch (error) {
          sendJson(res, 400, { success: false, message: 'Invalid JSON' });
        }
        return;
      }

      if (apiPath === '/farmers/register') {
        if (req.method !== 'POST') {
          sendJson(res, 405, { success: false, message: 'Method not allowed' });
          return;
        }
        try {
          const payload = await readBody(req);
          const email = normalizeEmail(payload.email);
          if (!email || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
            sendJson(res, 400, { success: false, message: 'Please provide a valid email address' });
            return;
          }
          if (!payload.firstName || !payload.lastName) {
            sendJson(res, 400, { success: false, message: 'First and last name are required' });
            return;
          }
          const user = state.users.find((item) => normalizeEmail(item.email) === email);
          if (user) {
            sendJson(res, 400, { success: false, message: 'Email already exists' });
            return;
          }
          const password = payload.password || 'agri123';
          if (password.length < 6) {
            sendJson(res, 400, { success: false, message: 'Password must be at least 6 characters' });
            return;
          }
          const newUser = {
            id: state.users.length + 1,
            firstName: payload.firstName || 'New',
            lastName: payload.lastName || 'Farmer',
            name: `${payload.firstName || 'New'} ${payload.lastName || 'Farmer'}`.trim(),
            email,
            phone: payload.phone || '',
            role: 'farmer',
            passwordHash: hashPassword(password),
            farmName: payload.farmName || 'New Farm',
            farmLocation: payload.farmLocation || 'Unknown',
            province: payload.province || 'Unknown',
            district: payload.district || 'Unknown',
            gpsCoordinates: payload.gpsCoordinates || '',
            farmingType: payload.farmingType || 'Mixed Farming',
            farmSizeHa: Number(payload.farmSizeHa || 0),
            profilePhoto: payload.profilePhoto || 'default.jpg',
            emailVerified: false,
            accountStatus: 'Pending Review',
            notificationPrefs: payload.notificationPrefs || { email: true, sms: true, push: false },
            createdAt: new Date().toISOString().slice(0, 10)
          };
          state.users.push(newUser);
          state.farms.push({
            id: state.farms.length + 1,
            name: newUser.farmName,
            location: newUser.farmLocation,
            sizeHa: newUser.farmSizeHa,
            owner: newUser.name,
            ownerEmail: newUser.email,
            gpsCoordinates: newUser.gpsCoordinates,
            farmingType: newUser.farmingType
          });
          state.activities.push({ id: state.activities.length + 1, title: 'Farmer registered', detail: `${newUser.name} joined AgriNexus` });
          sendJson(res, 200, { success: true, user: serializeUser(newUser), profile: buildFarmerProfile(newUser), token: `jwt-demo-${newUser.id}` });
        } catch (error) {
          sendJson(res, 400, { success: false, message: 'Invalid JSON' });
        }
        return;
      }

      if (apiPath === '/farmers/dashboard') {
        sendJson(res, 200, buildDashboard());
        return;
      }

      if (apiPath === '/farmers/password') {
        if (req.method !== 'POST') {
          sendJson(res, 405, { success: false, message: 'Method not allowed' });
          return;
        }
        try {
          const payload = await readBody(req);
          const user = state.users.find((item) => normalizeEmail(item.email) === normalizeEmail(payload.email));
          if (!user) {
            sendJson(res, 404, { success: false, message: 'User not found' });
            return;
          }
          if (user.passwordHash !== hashPassword(payload.currentPassword || '')) {
            sendJson(res, 401, { success: false, message: 'Current password is incorrect' });
            return;
          }
          const newPassword = payload.newPassword || '';
          const confirmPassword = payload.confirmPassword || '';
          if (newPassword.length < 6 || newPassword !== confirmPassword) {
            sendJson(res, 400, { success: false, message: 'New passwords must match and be at least 6 characters' });
            return;
          }
          user.passwordHash = hashPassword(newPassword);
          sendJson(res, 200, { success: true, message: 'Password updated successfully', user: serializeUser(user) });
        } catch (error) {
          sendJson(res, 400, { success: false, message: 'Invalid JSON' });
        }
        return;
      }

      if (apiPath.startsWith('/farmers/profile')) {
        if (req.method === 'GET') {
          const email = requestUrl.searchParams.get('email') || '';
          const user = state.users.find((item) => normalizeEmail(item.email) === normalizeEmail(email));
          if (!user) {
            sendJson(res, 404, { success: false, message: 'User not found' });
            return;
          }
          sendJson(res, 200, { success: true, profile: buildFarmerProfile(user) });
          return;
        }
        if (req.method === 'PUT') {
          try {
            const payload = await readBody(req);
            const email = payload.email || '';
            const user = state.users.find((item) => normalizeEmail(item.email) === normalizeEmail(email));
            if (!user) {
              sendJson(res, 404, { success: false, message: 'User not found' });
              return;
            }
            user.firstName = payload.firstName || user.firstName;
            user.lastName = payload.lastName || user.lastName;
            user.name = `${user.firstName} ${user.lastName}`.trim();
            user.phone = payload.phone || user.phone;
            user.farmName = payload.farmName || user.farmName;
            user.farmLocation = payload.farmLocation || user.farmLocation;
            user.gpsCoordinates = payload.gpsCoordinates || user.gpsCoordinates;
            user.farmingType = payload.farmingType || user.farmingType;
            user.farmSizeHa = Number(payload.farmSizeHa || user.farmSizeHa);
            user.profilePhoto = payload.profilePhoto || user.profilePhoto;
            user.farmingActivities = payload.farmingActivities || user.farmingActivities || '';
            user.notificationPrefs = payload.notificationPrefs || user.notificationPrefs;
            const farm = state.farms.find((item) => item.ownerEmail === user.email);
            if (farm) {
              farm.name = user.farmName;
              farm.location = user.farmLocation;
              farm.sizeHa = user.farmSizeHa;
              farm.gpsCoordinates = user.gpsCoordinates;
              farm.farmingType = user.farmingType;
            }
            sendJson(res, 200, { success: true, user: serializeUser(user), profile: buildFarmerProfile(user) });
          } catch (error) {
            sendJson(res, 400, { success: false, message: 'Invalid JSON' });
          }
          return;
        }
      }

      if (apiPath === '/farms') {
        if (req.method === 'GET') {
          const ownerEmail = requestUrl.searchParams.get('email') || '';
          sendJson(res, 200, { success: true, farms: getFarmerFarms(ownerEmail) });
          return;
        }
        if (req.method === 'POST') {
          try {
            const payload = await readBody(req);
            const ownerEmail = payload.ownerEmail || '';
            const farm = {
              id: state.farms.length + 1,
              name: payload.name || 'New Farm',
              location: payload.location || 'Unknown',
              sizeHa: Number(payload.sizeHa || 0),
              owner: payload.owner || 'Farmer',
              ownerEmail,
              gpsCoordinates: payload.gpsCoordinates || '',
              farmingType: payload.farmingType || 'Mixed Farming'
            };
            state.farms.push(farm);
            state.activities.push({ id: state.activities.length + 1, title: 'Farm added', detail: `${farm.name} added to account` });
            sendJson(res, 200, { success: true, farm });
          } catch (error) {
            sendJson(res, 400, { success: false, message: 'Invalid JSON' });
          }
          return;
        }
        if (req.method === 'DELETE') {
          try {
            const farmId = Number(requestUrl.searchParams.get('id'));
            const ownerEmail = requestUrl.searchParams.get('email') || '';
            const index = state.farms.findIndex((item) => item.id === farmId && normalizeEmail(item.ownerEmail) === normalizeEmail(ownerEmail));
            if (index === -1) {
              sendJson(res, 404, { success: false, message: 'Farm not found' });
              return;
            }
            const removed = state.farms.splice(index, 1)[0];
            state.activities.push({ id: state.activities.length + 1, title: 'Farm removed', detail: `${removed.name} removed from your profile` });
            sendJson(res, 200, { success: true, removed });
          } catch (error) {
            sendJson(res, 400, { success: false, message: 'Invalid request' });
          }
          return;
        }
      }

      if (apiPath === '/ai-assistant') {
        if (req.method !== 'POST') {
          sendJson(res, 405, { success: false, message: 'Method not allowed' });
          return;
        }
        try {
          const payload = await readBody(req);
          const question = (payload.question || '').toLowerCase();
          let response = 'AgriNexus recommends regular monitoring, timely vaccinations, and local weather planning.';
          if (question.includes('vaccin')) {
            response = 'Schedule vaccination checks this week and confirm cold-chain storage for all doses.';
          } else if (question.includes('soil') || question.includes('crop')) {
            response = 'Review soil moisture, test for nutrient imbalance, and avoid overwatering during the current weather window.';
          } else if (question.includes('weather')) {
            response = 'Prepare for increased humidity and moderate rainfall by protecting feed stores and livestock shelter.';
          } else if (question.includes('livestock')) {
            response = 'Track body condition, inspect for respiratory symptoms, and keep vaccination records updated.';
          }
          sendJson(res, 200, { response });
        } catch (error) {
          sendJson(res, 400, { success: false, message: 'Invalid JSON' });
        }
        return;
      }

      sendJson(res, 404, { success: false, message: 'Endpoint not found' });
      return;
    }

    sendJson(res, 404, { success: false, message: 'Not found' });
  });
}

function startServer(portNumber = port) {
  const server = createServer();
  server.listen(portNumber, () => {
    console.log(`AgriNexus listening on http://localhost:${portNumber}`);
  });
  return server;
}

if (require.main === module) {
  startServer();
}

module.exports = { createServer, startServer };
