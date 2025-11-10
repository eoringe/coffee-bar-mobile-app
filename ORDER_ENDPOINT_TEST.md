# Testing the `/orders` Endpoint

## Endpoint Details
- **Method**: `POST`
- **URL**: `http://localhost:8080/orders`
- **Authentication**: Required (Firebase Bearer Token)
- **Content-Type**: `application/json`

---

## Request Body Parameters

### Required Fields:
1. **`items`** (Array) - List of order items
   - Each item must have:
     - `menuItemId` (Integer) - ID of the menu item
     - `size` (String) - Either `"single"` or `"double"`
     - `quantity` (Integer) - Number of items

2. **`phoneNumber`** (String) - M-Pesa phone number (format: `254XXXXXXXXX`)

---

## Complete Request Structure

```json
{
  "items": [
    {
      "menuItemId": 1,
      "size": "single",
      "quantity": 2
    },
    {
      "menuItemId": 2,
      "size": "double",
      "quantity": 1
    }
  ],
  "phoneNumber": "254712345678"
}
```

---

## Testing Examples

### Example 1: Single Item Order
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

### Example 2: Multiple Items Order
```json
{
  "items": [
    {
      "menuItemId": 1,
      "size": "single",
      "quantity": 2
    },
    {
      "menuItemId": 2,
      "size": "double",
      "quantity": 1
    },
    {
      "menuItemId": 3,
      "size": "single",
      "quantity": 3
    }
  ],
  "phoneNumber": "254712345678"
}
```

---

## cURL Commands

### Basic Test
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

### With PowerShell (Windows)
```powershell
$headers = @{
    "Authorization" = "Bearer YOUR_FIREBASE_TOKEN"
    "Content-Type" = "application/json"
}

$body = @{
    items = @(
        @{
            menuItemId = 1
            size = "single"
            quantity = 2
        }
    )
    phoneNumber = "254712345678"
} | ConvertTo-Json

Invoke-RestMethod -Method POST -Uri "http://localhost:8080/orders" -Headers $headers -Body $body
```

---

## Response Format

### Success Response (200 OK)
```json
{
  "orderId": 1,
  "checkoutRequestID": "ws_CO_DMZ_123456789",
  "merchantRequestID": "12345-123456789-1",
  "status": "PENDING_PAYMENT",
  "message": "Payment initiated. Awaiting confirmation"
}
```

### Error Responses

**401 Unauthorized** (No Firebase Token)
```json
{
  "error": "Unauthorized"
}
```

**400 Bad Request** (Invalid body format)
```json
{
  "error": "Invalid body"
}
```

**400 Bad Request** (Menu item not found)
```json
{
  "error": "Menu item not found: 999"
}
```

**400 Bad Request** (Invalid size)
```json
{
  "error": "Invalid size: medium"
}
```

**500 Internal Server Error**
```json
{
  "error": "Failed to initiate payment"
}
```

---

## Validation Rules

1. **`items` array**:
   - Cannot be empty
   - Each item must have valid `menuItemId`, `size`, and `quantity`

2. **`size` field**:
   - Must be exactly `"single"` or `"double"` (case-insensitive)
   - Any other value will result in an error

3. **`menuItemId`**:
   - Must reference an existing menu item in the database
   - If the ID doesn't exist, you'll get an error

4. **`phoneNumber`**:
   - Should follow format: `254XXXXXXXXX` (Kenyan format)
   - No spaces or special characters

5. **`quantity`**:
   - Must be a positive integer

---

## Testing Steps

1. **Get menu item IDs** first:
   ```bash
   curl http://localhost:8080/menu-items
   ```
   This will show you available menu items with their IDs.

2. **Get a valid Firebase ID token**:
   - **From Android App**: Use `FirebaseAuth.getInstance().currentUser?.getIdToken(false)` 
   - **Firebase ID tokens are JWTs** that are 800-1000+ characters long
   - **Token format**: Three parts separated by dots: `header.payload.signature`
   - **Tokens expire** after 1 hour, so refresh if needed
   - See "Getting Firebase ID Tokens" section below for details

3. **Create an order** with valid menu item IDs

4. **Check the order**:
   ```bash
   curl -H "Authorization: Bearer YOUR_FIREBASE_TOKEN" \
     http://localhost:8080/orders/1
   ```

---

## Notes

- The endpoint automatically calculates the total price based on the menu item prices
- The `size` field determines which price is used (`singlePrice` or `doublePrice`)
- The order initiates an STK push automatically to the provided phone number
- The order status starts as "PENDING_PAYMENT"

---

## Getting Firebase ID Tokens

### Important Notes About Firebase ID Tokens:
- **Firebase ID tokens are JWTs** (JSON Web Tokens)
- **Length**: Typically 800-1000+ characters (not short strings!)
- **Format**: Three parts separated by dots: `header.payload.signature`
- **Expiration**: Tokens expire after 1 hour
- **Usage**: Used to securely identify the signed-in user on your backend

### How Token Verification Works (Following Firebase Documentation):

1. **Client retrieves ID token** after successful Firebase sign-in
2. **Client sends token** to backend via HTTPS with `Authorization: Bearer <token>` header
3. **Backend verifies token** using Firebase Admin SDK's `verifyIdToken()` method
4. **Backend extracts UID** from the decoded token's `sub` (subject) claim

The backend automatically validates:
- ✅ Token format (JWT with 3 parts)
- ✅ Expiration (`exp` claim must be in future)
- ✅ Issuer (`iss` must be `https://securetoken.google.com/<projectId>`)
- ✅ Audience (`aud` must match your Firebase project ID)
- ✅ Signature (verified against Firebase's public keys)
- ✅ Issued-at time (`iat` must be in past)
- ✅ Authentication time (`auth_time` must be in past)

### Getting ID Token from Android App:

```kotlin
// In your Android app
val user = FirebaseAuth.getInstance().currentUser
user?.getIdToken(false)?.addOnCompleteListener { task ->
    if (task.isSuccessful) {
        val idToken = task.result?.token
        // idToken is the long JWT string (800+ characters)
        // Send this to your backend in the Authorization header
    }
}
```

Or with Kotlin coroutines:
```kotlin
val user = FirebaseAuth.getInstance().currentUser
val idToken = user?.getIdToken(false)?.await()?.token
```

### Testing Without Mobile App:

If you need to test without the mobile app, you have two options:

1. **Use the test endpoint** (no authentication required):
   ```
   POST http://127.0.0.1:8080/test/orders
   ```
   - This endpoint bypasses authentication for testing
   - Uses a test user UID: `test-user-123`
   - ⚠️ **Remove this endpoint in production!**

2. **Get a token from Firebase Console**:
   - Sign in to your Firebase project
   - Go to Authentication → Users
   - For testing, you can use Firebase's REST API to get a token
   - Or create a test script that authenticates and gets a token

### Common Token Issues:

1. **"Token too short"**: You're sending a fake/invalid token
   - Firebase ID tokens are 800+ characters
   - Check your token length in Postman

2. **"Invalid token format"**: Token is not a proper JWT
   - Must have 3 parts separated by dots
   - Format: `xxxxx.yyyyy.zzzzz`

3. **"Token expired"**: Token is older than 1 hour
   - Get a fresh token from the client app
   - Use `getIdToken(true)` to force refresh

4. **"Unauthorized" error**: Token verification failed
   - Check server logs for detailed error messages
   - Verify Firebase service account is configured correctly
   - Ensure the token's `aud` (audience) matches your Firebase project ID


