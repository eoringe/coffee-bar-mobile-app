# Testing Guide for Coffee Bar Backend API

## Starting the Backend Server

From the project root, run:
```bash
cd backend
gradlew.bat run
```

Or if you're in the backend directory:
```bash
gradlew.bat run
```

The server will start on `http://localhost:8080`

---

## Available Endpoints

### 🌍 Public Endpoints (No Authentication Required)

#### 1. Health Check
**GET** `http://localhost:8080/`
```bash
curl http://localhost:8080/
```
Expected response:
```json
{
  "message": "Coffee Bar API is running!"
}
```

#### 2. Health Status
**GET** `http://localhost:8080/health`
```bash
curl http://localhost:8080/health
```
Expected response:
```json
{
  "status": "OK"
}
```

#### 3. Get Menu Items
**GET** `http://localhost:8080/menu-items`
```bash
curl http://localhost:8080/menu-items
```
Expected response: Array of menu items

---

### 🔒 Protected Endpoints (Firebase Authentication Required)

These endpoints require a valid Firebase token in the `Authorization` header.

#### 4. Create Order
**POST** `http://localhost:8080/orders`
```bash
curl -X POST http://localhost:8080/orders \
  -H "Authorization: Bearer YOUR_FIREBASE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "menuItemId": 1,
        "size": "single",
        "quantity": 2
      }
    ],
    "phoneNumber": "254712345678"
  }'
```

#### 5. Get Order by ID
**GET** `http://localhost:8080/orders/{id}`
```bash
curl -H "Authorization: Bearer YOUR_FIREBASE_TOKEN" \
  http://localhost:8080/orders/1
```

#### 6. Get User Profile
**GET** `http://localhost:8080/user/profile`
```bash
curl -H "Authorization: Bearer YOUR_FIREBASE_TOKEN" \
  http://localhost:8080/user/profile
```

---

## Testing with Postman

1. **Import Collection**
   - Create a new collection called "Coffee Bar API"
   
2. **Public Endpoints** (Create these requests):
   - GET `http://localhost:8080/`
   - GET `http://localhost:8080/health`
   - GET `http://localhost:8080/menu-items`

3. **Protected Endpoints** (Create these requests):
   - POST `http://localhost:8080/orders`
     - Headers: 
       - `Authorization: Bearer YOUR_FIREBASE_TOKEN`
       - `Content-Type: application/json`
     - Body (raw JSON):
     ```json
     {
       "items": [
         {
           "menuItemId": 1,
           "size": "single",
           "quantity": 1
         }
       ],
       "phoneNumber": "254712345678"
     }
     ```
   
   - GET `http://localhost:8080/orders/{id}`
     - Headers: `Authorization: Bearer YOUR_FIREBASE_TOKEN`
   
   - GET `http://localhost:8080/user/profile`
     - Headers: `Authorization: Bearer YOUR_FIREBASE_TOKEN`

---

## Getting a Firebase Token (for Protected Endpoints)

1. **Using the Mobile App**: Login in your mobile app and get the Firebase token
2. **Using Postman**: You'll need to implement a login endpoint or use Firebase SDK
3. **For Testing**: You might want to add a test/dev endpoint that doesn't require auth

---

## Testing Payment Endpoints

### STK Push (Public Testing Endpoint)
**POST** `http://localhost:8080/payments/stk-push`
```bash
curl -X POST http://localhost:8080/payments/stk-push \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "254712345678",
    "amount": 100,
    "accountReference": "TEST123",
    "transactionDesc": "Test Payment"
  }'
```

### Daraja Callback (Expected from M-Pesa)
**POST** `http://localhost:8080/daraja/callback`
This is called by M-Pesa automatically when payment is processed.

---

## Quick Test Script

Create a file `test-api.sh` (or `test-api.bat` for Windows):

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"

echo "Testing Health Check..."
curl "$BASE_URL/health"
echo -e "\n\n"

echo "Testing Root Endpoint..."
curl "$BASE_URL/"
echo -e "\n\n"

echo "Testing Menu Items..."
curl "$BASE_URL/menu-items"
echo -e "\n"
```

---

## Common Issues

1. **Port Already in Use**: Make sure no other application is using port 8080
2. **Database Connection**: Ensure PostgreSQL is running and configured correctly
3. **Firebase Token Expired**: Token expires after 1 hour, get a new one
4. **CORS Issues**: For browser testing, ensure CORS is properly configured

---

## Next Steps

1. Start the server: `cd backend && gradlew.bat run`
2. Test health endpoint: `curl http://localhost:8080/health`
3. Get menu items: `curl http://localhost:8080/menu-items`
4. Test order creation (requires Firebase token)


