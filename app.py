from flask import Flask, jsonify, request, send_from_directory
import hashlib

app = Flask(__name__, static_folder='.', static_url_path='')


def hash_password(value):
    return hashlib.sha256(value.encode('utf-8')).hexdigest()


def serialize_user(user):
    payload = dict(user)
    payload.pop('passwordHash', None)
    return payload


def build_dashboard():
    return {
        "totalLivestock": len(state['livestock']),
        "totalCrops": len(state['crops']),
        "upcomingVaccinations": 3,
        "weatherAlerts": len([item for item in state['notifications'] if 'weather' in item['title'].lower()]),
        "marketPriceUpdates": len(state['products']),
        "recentFarmActivities": state['activities'][-3:]
    }


def build_farmer_profile(user):
    farm = next((item for item in state['farms'] if item.get('ownerEmail') == user['email']), None)
    return {
        "user": serialize_user(user),
        "farm": farm,
        "dashboard": build_dashboard(),
        "notificationPrefs": user.get('notificationPrefs', {"email": True, "sms": True, "push": False})
    }


state = {
    "users": [
        {
            "id": 1,
            "firstName": "Lerato",
            "lastName": "Mokoena",
            "name": "Lerato Mokoena",
            "email": "lerato@example.com",
            "phone": "+27 82 555 1234",
            "role": "farmer",
            "passwordHash": hash_password("password123"),
            "farmName": "Mokopane Orchard",
            "farmLocation": "Limpopo, Capricorn",
            "province": "Limpopo",
            "district": "Capricorn",
            "gpsCoordinates": "24.12, 29.45",
            "farmingType": "Mixed Farming",
            "farmSizeHa": 24,
            "profilePhoto": "lerato.jpg",
            "emailVerified": True,
            "accountStatus": "Active",
            "notificationPrefs": {"email": True, "sms": True, "push": False},
            "createdAt": "2026-01-10"
        }
    ],
    "farms": [
        {
            "id": 1,
            "name": "Mokopane Orchard",
            "location": "Limpopo, Capricorn",
            "sizeHa": 24,
            "owner": "Lerato Mokoena",
            "ownerEmail": "lerato@example.com",
            "gpsCoordinates": "24.12, 29.45",
            "farmingType": "Mixed Farming"
        }
    ],
    "livestock": [
        {"id": 1, "name": "Cattle herd A", "type": "Cattle", "status": "Healthy", "farm": "Mokopane Orchard"}
    ],
    "healthRecords": [
        {"id": 1, "animal": "Cattle herd A", "condition": "Vaccination due", "severity": "Medium"}
    ],
    "crops": [
        {"id": 1, "name": "Maize", "stage": "Flowering", "farm": "Mokopane Orchard"}
    ],
    "products": [
        {"id": 1, "name": "Fresh maize", "price": 42, "seller": "Lerato Mokoena", "category": "Crops"},
        {"id": 2, "name": "Livestock feed", "price": 180, "seller": "AgriCoop", "category": "Inputs"}
    ],
    "orders": [],
    "notifications": [
        {"id": 1, "title": "Vaccination reminder", "body": "Cattle herd A needs a vaccine check this week."},
        {"id": 2, "title": "Weather alert", "body": "Rain expected across the eastern belt tomorrow."}
    ],
    "activities": [
        {"id": 1, "title": "Vaccination reminder", "detail": "Cattle herd A scheduled for review"},
        {"id": 2, "title": "Crop inspection", "detail": "Maize field checked after rainfall"},
        {"id": 3, "title": "Market update", "detail": "Fresh produce price posted for local buyers"}
    ]
}


@app.route('/')
def index():
    return send_from_directory('.', 'index.html')


@app.route('/api/health')
def health():
    return jsonify({"status": "ok", "message": "AgriNexus backend is running"})


@app.route('/api/dashboard-summary')
def dashboard_summary():
    return jsonify({
        "farmers": len(state['users']),
        "farms": len(state['farms']),
        "livestock": len(state['livestock']),
        "orders": len(state['orders']),
        "alerts": len(state['notifications'])
    })


@app.route('/api/auth/register', methods=['POST'])
def register_user():
    payload = request.get_json() or {}
    email = payload.get('email', '').strip().lower()
    if not email:
        return jsonify({"success": False, "message": "Email is required"}), 400
    if any(item['email'].lower() == email for item in state['users']):
        return jsonify({"success": False, "message": "Email already exists"}), 400

    password = payload.get('password')
    confirm_password = payload.get('confirmPassword')
    if password is None and confirm_password is None:
        password = 'agri123'
        confirm_password = 'agri123'
    elif password is None:
        password = confirm_password or 'agri123'
    elif confirm_password is None:
        confirm_password = password

    if password != confirm_password:
        return jsonify({"success": False, "message": "Passwords do not match"}), 400

    user = {
        "id": len(state['users']) + 1,
        "firstName": payload.get('firstName', payload.get('name', 'New')),
        "lastName": payload.get('lastName', 'Farmer'),
        "name": f"{payload.get('firstName', payload.get('name', 'New'))} {payload.get('lastName', 'Farmer')}".strip(),
        "email": email,
        "phone": payload.get('phone', ''),
        "role": payload.get('role', 'farmer'),
        "passwordHash": hash_password(password),
        "farmName": payload.get('farmName', 'New Farm'),
        "farmLocation": payload.get('farmLocation', payload.get('location', 'Unknown')),
        "province": payload.get('province', 'Unknown'),
        "district": payload.get('district', 'Unknown'),
        "gpsCoordinates": payload.get('gpsCoordinates', ''),
        "farmingType": payload.get('farmingType', 'Mixed Farming'),
        "farmSizeHa": float(payload.get('farmSizeHa', 0) or 0),
        "profilePhoto": payload.get('profilePhoto', 'default.jpg'),
        "emailVerified": False,
        "accountStatus": "Pending Review",
        "notificationPrefs": payload.get('notificationPrefs', {"email": True, "sms": True, "push": False}),
        "createdAt": payload.get('createdAt', 'Today')
    }

    state['users'].append(user)
    state['farms'].append({
        "id": len(state['farms']) + 1,
        "name": user['farmName'],
        "location": user['farmLocation'],
        "sizeHa": user['farmSizeHa'],
        "owner": user['name'],
        "ownerEmail": user['email'],
        "gpsCoordinates": user['gpsCoordinates'],
        "farmingType": user['farmingType']
    })
    state['activities'].append({
        "id": len(state['activities']) + 1,
        "title": "Profile created",
        "detail": f"{user['name']} registered their farm profile"
    })
    return jsonify({"success": True, "user": serialize_user(user), "profile": build_farmer_profile(user), "token": f"jwt-demo-{user['id']}"})


@app.route('/api/auth/login', methods=['POST'])
def login_user():
    payload = request.get_json() or {}
    email = payload.get('email', '').strip().lower()
    user = next((item for item in state['users'] if item['email'].lower() == email), None)
    if not user:
        return jsonify({"success": False, "message": "User not found"}), 404
    if user.get('passwordHash') != hash_password(payload.get('password', '')):
        return jsonify({"success": False, "message": "Invalid password"}), 401
    return jsonify({"success": True, "user": serialize_user(user), "profile": build_farmer_profile(user), "token": f"jwt-demo-{user['id']}"})


@app.route('/api/auth/forgot-password', methods=['POST'])
def forgot_password():
    payload = request.get_json() or {}
    email = payload.get('email', '').strip().lower()
    user = next((item for item in state['users'] if item['email'].lower() == email), None)
    if not user:
        return jsonify({"success": False, "message": "No account found"}), 404
    return jsonify({"success": True, "message": "Password reset instructions sent to your email."})


@app.route('/api/auth/verify-email', methods=['POST'])
def verify_email():
    payload = request.get_json() or {}
    email = payload.get('email', '').strip().lower()
    user = next((item for item in state['users'] if item['email'].lower() == email), None)
    if not user:
        return jsonify({"success": False, "message": "User not found"}), 404
    user['emailVerified'] = True
    user['accountStatus'] = 'Active'
    return jsonify({"success": True, "user": serialize_user(user)})


@app.route('/api/farmers/register', methods=['POST'])
def farmer_register():
    return register_user()


@app.route('/api/farmers/profile', methods=['GET', 'PUT'])
def farmer_profile():
    payload = request.get_json() or {}
    email = request.args.get('email') or payload.get('email')
    if not email:
        return jsonify({"success": False, "message": "Email is required"}), 400
    user = next((item for item in state['users'] if item['email'].lower() == email.lower()), None)
    if not user:
        return jsonify({"success": False, "message": "User not found"}), 404
    if request.method == 'PUT':
        for key in ['firstName', 'lastName', 'phone', 'farmName', 'farmLocation', 'province', 'district', 'gpsCoordinates', 'farmingType', 'farmSizeHa', 'profilePhoto', 'accountStatus', 'emailVerified']:
            if key in payload:
                user[key] = payload[key]
        if 'notificationPrefs' in payload:
            user['notificationPrefs'] = payload['notificationPrefs']
        user['name'] = f"{user.get('firstName', '')} {user.get('lastName', '')}".strip() or user['name']
        for farm in state['farms']:
            if farm.get('ownerEmail') == user['email']:
                farm['name'] = user.get('farmName', farm['name'])
                farm['location'] = user.get('farmLocation', farm['location'])
                farm['sizeHa'] = user.get('farmSizeHa', farm['sizeHa'])
                farm['gpsCoordinates'] = user.get('gpsCoordinates', farm['gpsCoordinates'])
                farm['farmingType'] = user.get('farmingType', farm['farmingType'])
        state['activities'].append({
            "id": len(state['activities']) + 1,
            "title": "Profile updated",
            "detail": f"{user['name']} updated their farmer profile"
        })
        return jsonify({"success": True, "user": serialize_user(user), "profile": build_farmer_profile(user)})
    return jsonify({"success": True, "user": serialize_user(user), "profile": build_farmer_profile(user)})


@app.route('/api/farmers/dashboard')
def farmer_dashboard():
    return jsonify(build_dashboard())


@app.route('/api/farms', methods=['GET', 'POST'])
def farms():
    if request.method == 'POST':
        payload = request.get_json() or {}
        farm = {
            "id": len(state['farms']) + 1,
            "name": payload.get('name', 'New Farm'),
            "location": payload.get('location', 'Unknown'),
            "sizeHa": payload.get('sizeHa', 0),
            "owner": payload.get('owner', 'Demo Farmer'),
            "ownerEmail": payload.get('ownerEmail', 'demo@example.com'),
            "gpsCoordinates": payload.get('gpsCoordinates', ''),
            "farmingType": payload.get('farmingType', 'Mixed Farming')
        }
        state['farms'].append(farm)
        state['activities'].append({
            "id": len(state['activities']) + 1,
            "title": "Farm added",
            "detail": f"{farm['name']} added to the farmer profile"
        })
        return jsonify({"success": True, "farm": farm})
    return jsonify(state['farms'])


@app.route('/api/livestock', methods=['GET', 'POST'])
def livestock():
    if request.method == 'POST':
        payload = request.get_json()
        item = {
            "id": len(state['livestock']) + 1,
            "name": payload.get('name', 'New Livestock'),
            "type": payload.get('type', 'Animal'),
            "status": payload.get('status', 'Healthy'),
            "farm": payload.get('farm', 'Demo Farm')
        }
        state['livestock'].append(item)
        return jsonify({"success": True, "livestock": item})
    return jsonify(state['livestock'])


@app.route('/api/health-records', methods=['GET', 'POST'])
def health_records():
    if request.method == 'POST':
        payload = request.get_json()
        record = {
            "id": len(state['healthRecords']) + 1,
            "animal": payload.get('animal', 'Animal'),
            "condition": payload.get('condition', 'Routine check'),
            "severity": payload.get('severity', 'Low')
        }
        state['healthRecords'].append(record)
        return jsonify({"success": True, "record": record})
    return jsonify(state['healthRecords'])


@app.route('/api/crops', methods=['GET', 'POST'])
def crops():
    if request.method == 'POST':
        payload = request.get_json()
        crop = {
            "id": len(state['crops']) + 1,
            "name": payload.get('name', 'New Crop'),
            "stage": payload.get('stage', 'Planting'),
            "farm": payload.get('farm', 'Demo Farm')
        }
        state['crops'].append(crop)
        return jsonify({"success": True, "crop": crop})
    return jsonify(state['crops'])


@app.route('/api/weather')
def weather():
    return jsonify([{
        "region": "Limpopo",
        "temperatureC": 24,
        "chanceOfRain": 70,
        "alert": "Ideal conditions for planting"
    }])


@app.route('/api/market-prices')
def market_prices():
    return jsonify([
        {"commodity": "Maize", "price": 3200, "unit": "bag"},
        {"commodity": "Milk", "price": 22, "unit": "litre"}
    ])


@app.route('/api/products', methods=['GET', 'POST'])
def products():
    if request.method == 'POST':
        payload = request.get_json()
        product = {
            "id": len(state['products']) + 1,
            "name": payload.get('name', 'Demo Product'),
            "price": payload.get('price', 100),
            "seller": payload.get('seller', 'Demo Seller'),
            "category": payload.get('category', 'General')
        }
        state['products'].append(product)
        return jsonify({"success": True, "product": product})
    return jsonify(state['products'])


@app.route('/api/orders', methods=['GET', 'POST'])
def orders():
    if request.method == 'POST':
        payload = request.get_json()
        order = {
            "id": len(state['orders']) + 1,
            "product": payload.get('product', 'Fresh maize'),
            "quantity": payload.get('quantity', 1),
            "buyer": payload.get('buyer', 'Demo Buyer')
        }
        state['orders'].append(order)
        return jsonify({"success": True, "order": order})
    return jsonify(state['orders'])


@app.route('/api/notifications')
def notifications():
    return jsonify(state['notifications'])


@app.route('/api/ai-assistant', methods=['POST'])
def ai_assistant():
    payload = request.get_json()
    question = (payload.get('question') or '').lower()
    if 'vaccin' in question:
        advice = 'Vaccinate cattle before the next full moon and confirm the health record in your dashboard.'
    elif 'maize' in question or 'yellow' in question:
        advice = 'Check soil moisture and inspect for nitrogen deficiency before applying fertilizer.'
    elif 'weather' in question or 'rain' in question:
        advice = 'Prepare irrigation channels and protect young crops from heavy rainfall.'
    else:
        advice = 'Use the farm dashboard to track livestock health, crop growth, and weather alerts for better decisions.'
    return jsonify({"response": advice})


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
