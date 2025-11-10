# Coffee Bar Mobile App - Backend Components Analysis & Recommendations

## 📊 Current Implementation Analysis

### ✅ **Implemented Components**

1. **Authentication Infrastructure**
   - Firebase Authentication setup
   - Firebase token verification
   - Custom Firebase auth provider for Ktor
   - ⚠️ **Issue**: Routes are currently public (disabled for testing)

2. **Menu Management**
   - Categories table/model
   - MenuItems table/model (with single/double pricing)
   - Menu retrieval endpoint (`GET /menu-items`)
   - Image support (external URL-based)

3. **Order Management**
   - Orders table/model
   - OrderItems table/model
   - Order creation with payment integration
   - Order retrieval by ID
   - Order status tracking (PENDING_PAYMENT, PAID, FAILED, CANCELLED)

4. **Payment Integration**
   - M-Pesa/Daraja STK Push integration
   - Payment status polling
   - Payment callback handling
   - Payment status updates

5. **Database**
   - PostgreSQL with Exposed ORM
   - Schema creation on startup
   - Transaction management

6. **Infrastructure**
   - Ktor server setup
   - JSON serialization (Jackson)
   - Logging
   - Health check endpoints

---

## 🚨 **Critical Missing Components**

### 1. **User Profile Management** ⭐ HIGH PRIORITY
**Current State**: Only dummy endpoint exists
**Required**:
- User profiles table/model
- User registration/creation endpoint
- Profile retrieval endpoint
- Profile update endpoint (name, phone, preferences)
- Address management
- Default payment method storage

**Suggested Structure**:
```kotlin
object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val firebaseUid = varchar("firebase_uid", 128).uniqueIndex()
    val email = varchar("email", 255).nullable()
    val name = varchar("name", 255).nullable()
    val phoneNumber = varchar("phone_number", 32).nullable()
    val preferredPaymentMethod = varchar("preferred_payment_method", 32).nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object UserAddresses : Table("user_addresses") {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users.id)
    val addressLine1 = varchar("address_line_1", 255)
    val addressLine2 = varchar("address_line_2", 255).nullable()
    val city = varchar("city", 100)
    val postalCode = varchar("postal_code", 20).nullable()
    val isDefault = bool("is_default").default(false)
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(id)
}
```

**Endpoints Needed**:
- `POST /users` - Create user profile
- `GET /users/me` - Get current user profile
- `PUT /users/me` - Update user profile
- `GET /users/me/addresses` - Get user addresses
- `POST /users/me/addresses` - Add address
- `PUT /users/me/addresses/{id}` - Update address
- `DELETE /users/me/addresses/{id}` - Delete address

---

### 2. **Order History & Management** ⭐ HIGH PRIORITY
**Current State**: Can only get single order by ID
**Required**:
- List all orders for a user
- Order filtering (by status, date range)
- Order cancellation endpoint
- Order tracking (with status updates)
- Order receipt generation

**Endpoints Needed**:
- `GET /orders` - List user's orders (with pagination)
  - Query params: `status`, `fromDate`, `toDate`, `page`, `limit`
- `PUT /orders/{id}/cancel` - Cancel an order
- `GET /orders/{id}/track` - Get detailed order tracking info
- `GET /orders/{id}/receipt` - Get order receipt (PDF or JSON)

**Model Updates**:
```kotlin
object Orders : Table("orders") {
    // ... existing fields ...
    val cancelledAt = datetime("cancelled_at").nullable()
    val cancelledReason = varchar("cancelled_reason", 255).nullable()
    val estimatedReadyTime = datetime("estimated_ready_time").nullable()
    val readyAt = datetime("ready_at").nullable()
    val completedAt = datetime("completed_at").nullable()
}
```

---

### 3. **Authentication & Authorization** ⭐ HIGH PRIORITY
**Current State**: Routes are public (disabled for testing)
**Required**:
- Enable authentication on protected routes
- Role-based access control (Customer, Admin, Staff)
- Refresh token handling
- Session management

**Suggested Implementation**:
```kotlin
enum class UserRole {
    CUSTOMER, ADMIN, STAFF
}

object Users : Table("users") {
    // ... existing fields ...
    val role = varchar("role", 20).default(UserRole.CUSTOMER.name)
}
```

**Route Protection**:
```kotlin
route("/orders") {
    authenticate("firebase-auth") {
        post { orderController.createOrder(call) }
        get { orderController.getUserOrders(call) }
    }
}
```

---

### 4. **Shopping Cart Management** ⭐ MEDIUM PRIORITY
**Current State**: Cart logic likely in mobile app only
**Required**:
- Server-side cart persistence
- Cart synchronization across devices
- Cart to order conversion

**Suggested Structure**:
```kotlin
object Carts : Table("carts") {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users.id)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object CartItems : Table("cart_items") {
    val id = integer("id").autoIncrement()
    val cartId = reference("cart_id", Carts.id)
    val menuItemId = reference("menu_item_id", MenuItems.id)
    val size = varchar("size", 16)
    val quantity = integer("quantity")
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(id)
}
```

**Endpoints Needed**:
- `GET /cart` - Get user's cart
- `POST /cart/items` - Add item to cart
- `PUT /cart/items/{id}` - Update cart item quantity
- `DELETE /cart/items/{id}` - Remove item from cart
- `DELETE /cart` - Clear cart
- `POST /cart/checkout` - Convert cart to order

---

### 5. **Reviews & Ratings** ⭐ MEDIUM PRIORITY
**Required**:
- Review submission
- Rating system (1-5 stars)
- Review moderation
- Average rating calculation

**Suggested Structure**:
```kotlin
object Reviews : Table("reviews") {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users.id)
    val menuItemId = reference("menu_item_id", MenuItems.id).nullable()
    val orderId = reference("order_id", Orders.id).nullable()
    val rating = integer("rating") // 1-5
    val comment = text("comment").nullable()
    val isApproved = bool("is_approved").default(false)
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(id)
}
```

**Endpoints Needed**:
- `POST /reviews` - Submit review
- `GET /menu-items/{id}/reviews` - Get reviews for menu item
- `GET /reviews/user` - Get user's reviews
- `PUT /reviews/{id}` - Update review
- `DELETE /reviews/{id}` - Delete review

---

### 6. **Favorites/Wishlist** ⭐ LOW PRIORITY
**Required**:
- Favorite menu items storage
- Quick reordering from favorites

**Suggested Structure**:
```kotlin
object Favorites : Table("favorites") {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users.id)
    val menuItemId = reference("menu_item_id", MenuItems.id)
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(id)
    init {
        uniqueIndex(userId, menuItemId)
    }
}
```

**Endpoints Needed**:
- `GET /favorites` - Get user's favorites
- `POST /favorites` - Add to favorites
- `DELETE /favorites/{menuItemId}` - Remove from favorites

---

### 7. **Inventory & Stock Management** ⭐ HIGH PRIORITY (For Admin)
**Required**:
- Real-time stock tracking
- Low stock alerts
- Portion availability updates
- Ingredient tracking (optional)

**Model Updates**:
```kotlin
object MenuItems : Table("menu_items") {
    // ... existing fields ...
    val stockQuantity = integer("stock_quantity").default(0)
    val lowStockThreshold = integer("low_stock_threshold").default(10)
    val lastRestockedAt = datetime("last_restocked_at").nullable()
}
```

**Endpoints Needed** (Admin only):
- `PUT /admin/menu-items/{id}/stock` - Update stock
- `GET /admin/menu-items/low-stock` - Get low stock items
- `POST /admin/menu-items/{id}/restock` - Restock item

---

### 8. **Notifications System** ⭐ MEDIUM PRIORITY
**Required**:
- Push notification support (FCM)
- Order status notifications
- Promotional notifications
- Notification preferences

**Suggested Structure**:
```kotlin
object UserDevices : Table("user_devices") {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users.id)
    val deviceToken = varchar("device_token", 512)
    val platform = varchar("platform", 20) // "android", "ios"
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(id)
}

object Notifications : Table("notifications") {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users.id)
    val title = varchar("title", 255)
    val message = text("message")
    val type = varchar("type", 50) // "order_status", "promotion", "system"
    val isRead = bool("is_read").default(false)
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(id)
}
```

**Endpoints Needed**:
- `POST /notifications/register-device` - Register FCM token
- `GET /notifications` - Get user notifications
- `PUT /notifications/{id}/read` - Mark as read
- `DELETE /notifications/{id}` - Delete notification

**Service Needed**:
- `NotificationService` - Handle FCM push notifications

---

### 9. **Promotions & Discounts** ⭐ MEDIUM PRIORITY
**Required**:
- Promo code system
- Discount application
- Promotional campaigns
- Usage tracking

**Suggested Structure**:
```kotlin
object Promotions : Table("promotions") {
    val id = integer("id").autoIncrement()
    val code = varchar("code", 50).uniqueIndex()
    val description = text("description").nullable()
    val discountType = varchar("discount_type", 20) // "PERCENTAGE", "FIXED"
    val discountValue = integer("discount_value")
    val minOrderAmount = integer("min_order_amount").nullable()
    val maxDiscount = integer("max_discount").nullable()
    val validFrom = datetime("valid_from")
    val validUntil = datetime("valid_until")
    val usageLimit = integer("usage_limit").nullable()
    val usageCount = integer("usage_count").default(0)
    val isActive = bool("is_active").default(true)
    override val primaryKey = PrimaryKey(id)
}

object OrderPromotions : Table("order_promotions") {
    val id = integer("id").autoIncrement()
    val orderId = reference("order_id", Orders.id)
    val promotionId = reference("promotion_id", Promotions.id)
    val discountAmount = integer("discount_amount")
    override val primaryKey = PrimaryKey(id)
}
```

**Endpoints Needed**:
- `GET /promotions/active` - Get active promotions
- `POST /promotions/validate` - Validate promo code
- `POST /promotions/apply` - Apply promo to order

---

### 10. **Admin Dashboard & Management** ⭐ HIGH PRIORITY (For Admin)
**Required**:
- Admin authentication
- Dashboard statistics
- Order management
- Menu management
- User management
- Analytics

**Endpoints Needed** (Admin only):
- `GET /admin/dashboard/stats` - Dashboard statistics
- `GET /admin/orders` - List all orders (with filters)
- `PUT /admin/orders/{id}/status` - Update order status
- `POST /admin/menu-items` - Create menu item
- `PUT /admin/menu-items/{id}` - Update menu item
- `DELETE /admin/menu-items/{id}` - Delete menu item
- `GET /admin/users` - List users
- `GET /admin/analytics/revenue` - Revenue analytics
- `GET /admin/analytics/orders` - Order analytics
- `GET /admin/analytics/popular-items` - Popular items

---

### 11. **File/Image Upload Service** ⭐ MEDIUM PRIORITY
**Current State**: Images use external URLs
**Required**:
- Image upload endpoint
- Image storage (local or cloud)
- Image optimization
- Image deletion

**Suggested Implementation**:
- Use cloud storage (AWS S3, Google Cloud Storage, or Firebase Storage)
- Or local storage with proper file management

**Endpoints Needed**:
- `POST /upload/image` - Upload image
- `DELETE /upload/image/{id}` - Delete image

---

### 12. **Order Status Tracking** ⭐ MEDIUM PRIORITY
**Required**:
- Real-time order status updates
- Estimated ready time calculation
- Order preparation stages

**Model Updates**:
```kotlin
enum class OrderStatus {
    PENDING_PAYMENT,
    PAID,
    CONFIRMED,
    PREPARING,
    READY,
    COMPLETED,
    CANCELLED,
    FAILED
}
```

**Service Needed**:
- `OrderStatusService` - Handle status transitions
- WebSocket or Server-Sent Events for real-time updates

---

### 13. **Analytics & Reporting** ⭐ LOW PRIORITY (For Admin)
**Required**:
- Sales reports
- Popular items analysis
- Peak hours analysis
- Customer behavior analytics

**Endpoints Needed** (Admin only):
- `GET /admin/analytics/sales` - Sales report
- `GET /admin/analytics/items` - Popular items
- `GET /admin/analytics/peak-hours` - Peak hours analysis

---

### 14. **Error Handling & Logging** ⭐ HIGH PRIORITY
**Current State**: Basic error handling exists
**Required**:
- Centralized error handling
- Structured logging
- Error tracking (Sentry integration)
- Request/response logging middleware

**Suggested Implementation**:
```kotlin
// Global exception handler
install(StatusPages) {
    exception<AuthenticationException> { call, cause ->
        call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Unauthorized"))
    }
    exception<NotFoundException> { call, cause ->
        call.respond(HttpStatusCode.NotFound, ErrorResponse("Resource not found"))
    }
    exception<ValidationException> { call, cause ->
        call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message))
    }
    exception<Exception> { call, cause ->
        call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal server error"))
    }
}
```

---

### 15. **API Documentation** ⭐ MEDIUM PRIORITY
**Required**:
- OpenAPI/Swagger documentation
- API versioning
- Rate limiting

**Suggested Implementation**:
- Add Ktor OpenAPI plugin
- Document all endpoints
- Add API versioning (`/api/v1/...`)

---

### 16. **Testing Infrastructure** ⭐ MEDIUM PRIORITY
**Required**:
- Unit tests for services
- Integration tests for controllers
- Test database setup
- API endpoint tests

---

### 17. **Security Enhancements** ⭐ HIGH PRIORITY
**Required**:
- Rate limiting
- Input validation
- SQL injection prevention (already using Exposed ORM)
- CORS configuration
- HTTPS enforcement
- Request size limits

**Suggested Implementation**:
```kotlin
install(RateLimiter) {
    // Configure rate limits
}

install(CORS) {
    allowCredentials = true
    allowHost("your-domain.com")
    allowMethods(HttpMethod.Get, HttpMethod.Post, HttpMethod.Put, HttpMethod.Delete)
}
```

---

### 18. **Order Refund System** ⭐ LOW PRIORITY
**Required** (if needed):
- Refund request handling
- Refund processing
- Refund history

---

## 📋 Implementation Priority Summary

### **Phase 1 - Core Functionality (Critical)**
1. ✅ User Profile Management
2. ✅ Enable Authentication on Routes
3. ✅ Order History & Management
4. ✅ Error Handling & Logging
5. ✅ Security Enhancements

### **Phase 2 - Enhanced Features (High Priority)**
6. ✅ Shopping Cart Management
7. ✅ Inventory & Stock Management
8. ✅ Admin Dashboard & Management
9. ✅ Notifications System
10. ✅ Order Status Tracking

### **Phase 3 - User Experience (Medium Priority)**
11. ✅ Reviews & Ratings
12. ✅ Promotions & Discounts
13. ✅ File/Image Upload Service
14. ✅ Favorites/Wishlist

### **Phase 4 - Advanced Features (Lower Priority)**
15. ✅ Analytics & Reporting
16. ✅ API Documentation
17. ✅ Testing Infrastructure
18. ✅ Order Refund System

---

## 🏗️ Suggested File Structure

```
backend/src/main/java/com/example/backend/
├── Application.kt
├── controllers/
│   ├── AuthController.kt (NEW)
│   ├── UserController.kt (NEW)
│   ├── CartController.kt (NEW)
│   ├── ReviewController.kt (NEW)
│   ├── NotificationController.kt (NEW)
│   ├── PromotionController.kt (NEW)
│   ├── AdminController.kt (NEW)
│   ├── OrderController.kt (EXISTING - needs updates)
│   ├── MenuController.kt (EXISTING - needs updates)
│   └── DarajaController.kt (EXISTING)
├── services/
│   ├── UserService.kt (NEW)
│   ├── CartService.kt (NEW)
│   ├── ReviewService.kt (NEW)
│   ├── NotificationService.kt (NEW)
│   ├── PromotionService.kt (NEW)
│   ├── AdminService.kt (NEW)
│   ├── OrderService.kt (EXISTING - needs updates)
│   └── DarajaService.kt (EXISTING)
├── models/
│   ├── Users.kt (NEW)
│   ├── UserAddresses.kt (NEW)
│   ├── Carts.kt (NEW)
│   ├── CartItems.kt (NEW)
│   ├── Reviews.kt (NEW)
│   ├── Favorites.kt (NEW)
│   ├── Notifications.kt (NEW)
│   ├── Promotions.kt (NEW)
│   ├── Orders.kt (EXISTING - needs updates)
│   ├── MenuItems.kt (EXISTING - needs updates)
│   └── Categories.kt (EXISTING)
├── dto/
│   ├── UserDTOs.kt (NEW)
│   ├── CartDTOs.kt (NEW)
│   ├── ReviewDTOs.kt (NEW)
│   ├── OrderDTOs.kt (EXISTING - needs updates)
│   └── DarajaDTOs.kt (EXISTING)
├── middleware/
│   ├── ErrorHandler.kt (NEW)
│   ├── RateLimiter.kt (NEW)
│   └── LoggingInterceptor.kt (NEW)
├── utils/
│   ├── ValidationUtils.kt (NEW)
│   └── ResponseUtils.kt (NEW)
└── plugins/
    ├── FirebaseAuth.kt (EXISTING)
    └── Configuration.kt (NEW)
```

---

## 🔧 Technical Recommendations

1. **Database Migrations**: Consider using Flyway or similar for version-controlled migrations
2. **Caching**: Add Redis for caching menu items, user sessions
3. **Background Jobs**: Add job scheduler for notifications, analytics
4. **API Versioning**: Implement `/api/v1/` prefix for all endpoints
5. **Pagination**: Implement pagination for all list endpoints
6. **Search**: Add search functionality for menu items
7. **Sorting & Filtering**: Add query parameters for sorting and filtering

---

## 📝 Notes

- All timestamps should use `LocalDateTime` consistently
- Consider adding soft deletes for important entities
- Add audit trails for admin actions
- Implement proper transaction management for critical operations
- Consider adding database indexes for frequently queried fields





