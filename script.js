document.addEventListener('DOMContentLoaded', () => {
  const links = document.querySelectorAll('.nav-links a');
  links.forEach((link) => {
    link.addEventListener('click', () => {
      links.forEach((item) => item.classList.remove('active'));
      link.classList.add('active');
    });
  });

  const apiBase = '/api';

  async function fetchJson(url, options = {}) {
    const response = await fetch(apiBase + url, {
      headers: { 'Content-Type': 'application/json' },
      ...options
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(data.message || 'Request failed');
    }
    return data;
  }

  async function loadDashboard() {
    try {
      const summary = await fetchJson('/dashboard-summary');
      document.getElementById('dashboard-farmers').textContent = summary.farmers;
      document.getElementById('dashboard-farms').textContent = summary.farms;
      document.getElementById('dashboard-livestock').textContent = summary.livestock;
      document.getElementById('dashboard-orders').textContent = summary.orders;
      document.getElementById('dashboard-alerts').textContent = summary.alerts;
    } catch (error) {
      console.error('Unable to load dashboard', error);
    }
  }

  async function loadMarketplace() {
    try {
      const products = await fetchJson('/products');
      const container = document.getElementById('marketplace-items');
      if (container) {
        container.innerHTML = products.map((product) => `
          <article class="market-card">
            <h3>${product.name}</h3>
            <p>${product.category}</p>
            <strong>R ${product.price}</strong>
            <span>Seller: ${product.seller}</span>
          </article>
        `).join('');
      }
    } catch (error) {
      console.error('Unable to load marketplace', error);
    }
  }

  async function loadAlerts() {
    try {
      const alerts = await fetchJson('/notifications');
      const list = document.getElementById('alerts-list');
      if (list) {
        list.innerHTML = alerts.map((item) => `<li><strong>${item.title}</strong><br>${item.body}</li>`).join('');
      }
    } catch (error) {
      console.error('Unable to load alerts', error);
    }
  }

  function setStatus(elementId, message, isError = false) {
    const element = document.getElementById(elementId);
    if (element) {
      element.textContent = message;
      element.style.color = isError ? '#b42318' : 'var(--muted)';
    }
  }

  function showProfile(user) {
    const profilePanel = document.getElementById('profile-panel');
    if (!profilePanel || !user) return;

    document.getElementById('profile-name').textContent = user.name;
    document.getElementById('profile-email').textContent = user.email;
    document.getElementById('profile-role').textContent = `Role: ${user.role}`;
    document.getElementById('profile-status').textContent = 'You have been signed in and can view your profile.';
    profilePanel.hidden = false;
  }

  function restoreProfile() {
    const savedUser = localStorage.getItem('agriNexusUser');
    if (!savedUser) return;

    try {
      const parsedUser = JSON.parse(savedUser);
      showProfile(parsedUser);
    } catch (error) {
      console.error('Unable to restore saved profile', error);
    }
  }

  function renderFarmerProfile(profile) {
    if (!profile || !profile.user) return;
    const user = profile.user;
    const cardName = document.getElementById('profile-card-name');
    const cardFarm = document.getElementById('profile-card-farm');
    const cardEmail = document.getElementById('profile-card-email');
    const cardStatus = document.getElementById('profile-card-status');

    if (cardName) cardName.textContent = `${user.firstName || user.name || ''} ${user.lastName || ''}`.trim();
    if (cardFarm) cardFarm.textContent = user.farmName || 'Farm profile pending';
    if (cardEmail) cardEmail.textContent = user.email || '—';
    if (cardStatus) cardStatus.textContent = `Account status: ${user.accountStatus || 'Pending Review'}`;

    const profileForm = document.getElementById('farmer-profile-form');
    if (profileForm) {
      document.getElementById('profile-first-name').value = user.firstName || '';
      document.getElementById('profile-last-name').value = user.lastName || '';
      document.getElementById('profile-phone').value = user.phone || '';
      document.getElementById('profile-farm-name').value = user.farmName || '';
      document.getElementById('profile-farm-location').value = user.farmLocation || '';
      document.getElementById('profile-farming-type').value = user.farmingType || 'Mixed Farming';
      document.getElementById('profile-farm-size').value = user.farmSizeHa || '';
      document.getElementById('profile-gps').value = user.gpsCoordinates || '';
      document.getElementById('notify-email').checked = user.notificationPrefs?.email !== false;
      document.getElementById('notify-sms').checked = user.notificationPrefs?.sms !== false;
      document.getElementById('notify-push').checked = Boolean(user.notificationPrefs?.push);
    }
  }

  async function renderFarmerDashboard() {
    try {
      const dashboard = await fetchJson('/farmers/dashboard');
      document.getElementById('dash-livestock').textContent = dashboard.totalLivestock;
      document.getElementById('dash-crops').textContent = dashboard.totalCrops;
      document.getElementById('dash-vaccinations').textContent = dashboard.upcomingVaccinations;
      document.getElementById('dash-weather').textContent = dashboard.weatherAlerts;
      document.getElementById('dash-market').textContent = dashboard.marketPriceUpdates;
      const list = document.getElementById('recent-activities');
      if (list && dashboard.recentFarmActivities) {
        list.innerHTML = dashboard.recentFarmActivities.map((item) => `<li><strong>${item.title}</strong><br>${item.detail}</li>`).join('');
      }
    } catch (error) {
      console.error('Unable to load farmer dashboard', error);
    }
  }

  async function restoreFarmerSession() {
    const stored = localStorage.getItem('agriNexusFarmer');
    if (!stored) return;

    try {
      const parsed = JSON.parse(stored);
      const profileResponse = await fetchJson(`/farmers/profile?email=${encodeURIComponent(parsed.email)}`);
      if (profileResponse.success) {
        renderFarmerProfile(profileResponse.profile);
      }
    } catch (error) {
      console.error('Unable to restore farmer session', error);
    }
  }

  async function bindForms() {
    const registerForm = document.getElementById('register-form');
    if (registerForm) {
      registerForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        try {
          const payload = {
            name: document.getElementById('register-name').value,
            email: document.getElementById('register-email').value,
            role: document.getElementById('register-role').value
          };
          const result = await fetchJson('/auth/register', { method: 'POST', body: JSON.stringify(payload) });
          if (result.success) {
            localStorage.setItem('agriNexusUser', JSON.stringify(result.user));
            showProfile(result.user);
            document.getElementById('register-status').textContent = 'Registration complete. You are now signed in.';
          } else {
            document.getElementById('register-status').textContent = result.message || 'Registration could not be completed.';
          }
        } catch (error) {
          document.getElementById('register-status').textContent = error.message || 'Registration could not be completed.';
        }
      });
    }

    const farmerRegisterForm = document.getElementById('farmer-register-form');
    if (farmerRegisterForm) {
      farmerRegisterForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        const password = document.getElementById('farmer-password').value;
        const confirm = document.getElementById('farmer-confirm-password').value;
        if (password.length < 6) {
          setStatus('farmer-register-status', 'Password must be at least 6 characters.', true);
          return;
        }
        if (password !== confirm) {
          setStatus('farmer-register-status', 'Passwords do not match.', true);
          return;
        }

        try {
          const payload = {
            firstName: document.getElementById('farmer-first-name').value,
            lastName: document.getElementById('farmer-last-name').value,
            email: document.getElementById('farmer-email').value,
            phone: document.getElementById('farmer-phone').value,
            password,
            confirmPassword: confirm,
            farmName: document.getElementById('farmer-farm-name').value,
            province: document.getElementById('farmer-province').value,
            district: document.getElementById('farmer-district').value,
            farmLocation: `${document.getElementById('farmer-province').value}, ${document.getElementById('farmer-district').value}`,
            gpsCoordinates: document.getElementById('farmer-gps').value,
            farmingType: document.getElementById('farmer-farming-type').value,
            farmSizeHa: document.getElementById('farmer-farm-size').value,
            profilePhoto: document.getElementById('farmer-profile-photo').files[0] ? document.getElementById('farmer-profile-photo').files[0].name : 'default.jpg',
            role: 'farmer'
          };
          const result = await fetchJson('/farmers/register', { method: 'POST', body: JSON.stringify(payload) });
          if (result.success) {
            localStorage.setItem('agriNexusFarmer', JSON.stringify(result.user));
            renderFarmerProfile(result.profile);
            setStatus('farmer-register-status', 'Welcome aboard. Your profile is ready for management.');
          } else {
            setStatus('farmer-register-status', result.message || 'Registration failed.', true);
          }
        } catch (error) {
          setStatus('farmer-register-status', error.message || 'Registration failed.', true);
        }
      });
    }

    const farmerLoginForm = document.getElementById('farmer-login-form');
    if (farmerLoginForm) {
      farmerLoginForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        try {
          const payload = {
            email: document.getElementById('farmer-login-email').value,
            password: document.getElementById('farmer-login-password').value,
            rememberMe: document.getElementById('farmer-remember').checked
          };
          const result = await fetchJson('/auth/login', { method: 'POST', body: JSON.stringify(payload) });
          if (result.success) {
            localStorage.setItem('agriNexusFarmer', JSON.stringify(result.user));
            renderFarmerProfile(result.profile);
            setStatus('farmer-login-status', 'Signed in successfully.');
          } else {
            setStatus('farmer-login-status', result.message || 'Unable to sign in.', true);
          }
        } catch (error) {
          setStatus('farmer-login-status', error.message || 'Unable to sign in.', true);
        }
      });
    }

    const forgotButton = document.getElementById('farmer-forgot-password');
    if (forgotButton) {
      forgotButton.addEventListener('click', async () => {
        const email = document.getElementById('farmer-login-email').value;
        const result = await fetchJson('/auth/forgot-password', { method: 'POST', body: JSON.stringify({ email }) });
        setStatus('farmer-login-status', result.message || 'Reset instructions sent.');
      });
    }

    const verifyEmailButton = document.getElementById('farmer-verify-email');
    if (verifyEmailButton) {
      verifyEmailButton.addEventListener('click', async () => {
        const email = document.getElementById('farmer-login-email').value;
        const result = await fetchJson('/auth/verify-email', { method: 'POST', body: JSON.stringify({ email }) });
        if (result.success) {
          setStatus('farmer-login-status', 'Email verification completed.');
        } else {
          setStatus('farmer-login-status', result.message || 'Verification failed.', true);
        }
      });
    }

    const profileForm = document.getElementById('farmer-profile-form');
    if (profileForm) {
      profileForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        const stored = JSON.parse(localStorage.getItem('agriNexusFarmer') || '{}');
        const payload = {
          email: stored.email,
          firstName: document.getElementById('profile-first-name').value,
          lastName: document.getElementById('profile-last-name').value,
          phone: document.getElementById('profile-phone').value,
          farmName: document.getElementById('profile-farm-name').value,
          farmLocation: document.getElementById('profile-farm-location').value,
          farmingType: document.getElementById('profile-farming-type').value,
          farmSizeHa: document.getElementById('profile-farm-size').value,
          gpsCoordinates: document.getElementById('profile-gps').value,
          profilePhoto: document.getElementById('profile-photo').files[0] ? document.getElementById('profile-photo').files[0].name : stored.profilePhoto || 'default.jpg',
          notificationPrefs: {
            email: document.getElementById('notify-email').checked,
            sms: document.getElementById('notify-sms').checked,
            push: document.getElementById('notify-push').checked
          }
        };
        const result = await fetchJson('/farmers/profile', { method: 'PUT', body: JSON.stringify(payload) });
        if (result.success) {
          localStorage.setItem('agriNexusFarmer', JSON.stringify(result.user));
          renderFarmerProfile(result.profile);
          setStatus('farmer-profile-status', 'Profile updated successfully.');
        } else {
          setStatus('farmer-profile-status', result.message || 'Profile update failed.', true);
        }
      });
    }

    const addFarmForm = document.getElementById('add-farm-form');
    if (addFarmForm) {
      addFarmForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        const stored = JSON.parse(localStorage.getItem('agriNexusFarmer') || '{}');
        const payload = {
          name: document.getElementById('farm-name').value,
          location: document.getElementById('farm-location').value,
          sizeHa: document.getElementById('farm-size').value,
          owner: stored.name || 'Farmer',
          ownerEmail: stored.email,
          gpsCoordinates: document.getElementById('farm-gps').value,
          farmingType: document.getElementById('farm-type').value
        };
        const result = await fetchJson('/farms', { method: 'POST', body: JSON.stringify(payload) });
        if (result.success) {
          setStatus('farm-status', 'Additional farm added to your profile.');
        } else {
          setStatus('farm-status', result.message || 'Unable to add farm.', true);
        }
      });
    }

    const weatherButton = document.getElementById('check-weather-button');
    if (weatherButton) {
      weatherButton.addEventListener('click', async () => {
        try {
          const weatherData = await fetchJson('/weather');
          const weather = Array.isArray(weatherData) ? weatherData[0] : weatherData;
          document.getElementById('weather-output').textContent = `${weather.alert || 'Weather outlook'} • ${weather.temperatureC}°C • Rain chance ${weather.chanceOfRain}%`;
        } catch (error) {
          document.getElementById('weather-output').textContent = error.message || 'Unable to load weather.';
        }
      });
    }

    const aiForm = document.getElementById('ai-form');
    if (aiForm) {
      aiForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        const payload = {
          question: document.getElementById('ai-question').value
        };
        const result = await fetchJson('/ai-assistant', { method: 'POST', body: JSON.stringify(payload) });
        document.getElementById('ai-response').textContent = result.response;
      });
    }
  }

  loadDashboard();
  loadMarketplace();
  loadAlerts();
  renderFarmerDashboard();
  bindForms();
  restoreProfile();
  restoreFarmerSession();
});
