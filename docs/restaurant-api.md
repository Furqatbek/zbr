# Restaurant API Documentation

Restaurant and menu management for the Food Delivery Platform. Provides endpoints for managing restaurants, menu categories, menu items, variants, and options.

## Base URLs

```
/api/v1/restaurants                           - Restaurant management
/api/v1/restaurants/{restaurantId}/menu       - Menu management
```

---

## Table of Contents

1. [Restaurant Management](#1-restaurant-management)
2. [Menu Categories](#2-menu-categories)
3. [Menu Items](#3-menu-items)
4. [Enumerations](#enumerations)
5. [Data Transfer Objects](#data-transfer-objects)

---

## 1. Restaurant Management

### Create Restaurant

**POST** `/api/v1/restaurants`

Create a new restaurant.

**Request Body:**
```json
{
  "name": "Pizza Palace",
  "description": "Authentic Italian pizzas made with love",
  "phone": "+12125551234",
  "email": "contact@pizzapalace.com",
  "addressLine1": "456 Food Street",
  "addressLine2": "Suite 100",
  "city": "New York",
  "state": "NY",
  "postalCode": "10002",
  "country": "USA",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "acceptsDelivery": true,
  "acceptsTakeaway": true,
  "acceptsDineIn": false,
  "minimumOrder": 15.00,
  "deliveryFee": 3.99,
  "deliveryRadiusKm": 10,
  "averagePrepTimeMinutes": 25,
  "opensAt": "10:00",
  "closesAt": "22:00"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | string | Yes | Restaurant name (max 200) |
| description | string | No | Description (max 1000) |
| phone | string | Yes | Phone number |
| email | string | No | Email address |
| addressLine1 | string | Yes | Street address (max 255) |
| addressLine2 | string | No | Address line 2 (max 255) |
| city | string | Yes | City (max 100) |
| state | string | No | State (max 100) |
| postalCode | string | No | Postal code (max 20) |
| country | string | No | Country (max 100) |
| latitude | BigDecimal | No | Latitude (-90 to 90) |
| longitude | BigDecimal | No | Longitude (-180 to 180) |
| acceptsDelivery | Boolean | No | Accepts delivery orders (default: true) |
| acceptsTakeaway | Boolean | No | Accepts takeaway orders (default: true) |
| acceptsDineIn | Boolean | No | Accepts dine-in orders (default: false) |
| minimumOrder | BigDecimal | No | Minimum order amount |
| deliveryFee | BigDecimal | No | Delivery fee |
| deliveryRadiusKm | Integer | No | Delivery radius in km (1-50) |
| averagePrepTimeMinutes | Integer | No | Average prep time (5-180) |
| opensAt | LocalTime | No | Opening time |
| closesAt | LocalTime | No | Closing time |

**Response (201):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "ownerId": 100,
    "name": "Pizza Palace",
    "slug": "pizza-palace",
    "status": "PENDING",
    "isOpen": false,
    ...
  }
}
```

**Roles:** RESTAURANT_OWNER, PLATFORM, ADMIN

---

### Get Restaurant by ID

**GET** `/api/v1/restaurants/{id}`

Get restaurant details by ID. Public endpoint.

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "ownerId": 100,
    "name": "Pizza Palace",
    "slug": "pizza-palace",
    "description": "Authentic Italian pizzas",
    "logoUrl": "https://example.com/logo.jpg",
    "coverImageUrl": "https://example.com/cover.jpg",
    "phone": "+12125551234",
    "email": "contact@pizzapalace.com",
    "fullAddress": "456 Food Street, Suite 100, New York, NY 10002",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "status": "ACTIVE",
    "featured": true,
    "acceptsDelivery": true,
    "acceptsTakeaway": true,
    "acceptsDineIn": false,
    "minimumOrder": 15.00,
    "deliveryFee": 3.99,
    "deliveryRadiusKm": 10,
    "averagePrepTimeMinutes": 25,
    "opensAt": "10:00",
    "closesAt": "22:00",
    "isOpen": true,
    "isCurrentlyOpen": true,
    "averageRating": 4.5,
    "totalRatings": 250,
    "totalOrders": 1500,
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

---

### Get Restaurant by Slug

**GET** `/api/v1/restaurants/slug/{slug}`

Get restaurant details by URL-friendly slug. Public endpoint.

---

### Get All Restaurants

**GET** `/api/v1/restaurants`

Get all restaurants with pagination. Public endpoint.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number |
| size | int | 20 | Page size |
| sort | string | - | Sort criteria |

---

### Get Active Restaurants

**GET** `/api/v1/restaurants/active`

Get only active restaurants. Public endpoint.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number |
| size | int | 20 | Page size |

---

### Search Restaurants

**GET** `/api/v1/restaurants/search`

Search restaurants by name or description. Public endpoint.

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| q | string | Yes | Search query |
| page | int | No | Page number (default: 0) |
| size | int | No | Page size (default: 20) |

---

### Get Featured Restaurants

**GET** `/api/v1/restaurants/featured`

Get featured restaurants. Public endpoint.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number |
| size | int | 10 | Page size |

---

### Get Nearby Restaurants

**GET** `/api/v1/restaurants/nearby`

Get restaurants near a location. Public endpoint.

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| lat | BigDecimal | Yes | - | Latitude |
| lng | BigDecimal | Yes | - | Longitude |
| radius | double | No | 10 | Radius in km |

**Response (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Pizza Palace",
      "distance": 1.5,
      ...
    }
  ]
}
```

---

### Get My Restaurants

**GET** `/api/v1/restaurants/my`

Get restaurants owned by authenticated user.

**Roles:** RESTAURANT_OWNER, PLATFORM, ADMIN

---

### Update Restaurant

**PUT** `/api/v1/restaurants/{id}`

Update restaurant details.

**Request Body:** Same as Create Restaurant

**Roles:** RESTAURANT_OWNER, RESTAURANT_STAFF, PLATFORM, ADMIN

---

### Update Restaurant Status

**PATCH** `/api/v1/restaurants/{id}/status`

Update restaurant status.

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| status | RestaurantStatus | Yes | New status |

**Roles:** PLATFORM, ADMIN

---

### Toggle Open Status

**PATCH** `/api/v1/restaurants/{id}/toggle-open`

Toggle restaurant open/closed status.

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| isOpen | Boolean | Yes | Open status |

**Roles:** RESTAURANT_OWNER, RESTAURANT_STAFF, PLATFORM, ADMIN

---

## 2. Menu Categories

### Get Full Menu

**GET** `/api/v1/restaurants/{restaurantId}/menu`

Get full menu with all categories and items. Public endpoint.

**Response (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "restaurantId": 1,
      "name": "Pizzas",
      "description": "Our famous wood-fired pizzas",
      "imageUrl": "https://example.com/pizzas.jpg",
      "sortOrder": 1,
      "active": true,
      "items": [...]
    }
  ]
}
```

---

### Get Menu Categories

**GET** `/api/v1/restaurants/{restaurantId}/menu/categories`

Get all menu categories. Public endpoint.

---

### Create Menu Category

**POST** `/api/v1/restaurants/{restaurantId}/menu/categories`

Create a new menu category.

**Request Body:**
```json
{
  "name": "Pizzas",
  "description": "Our famous wood-fired pizzas",
  "imageUrl": "https://example.com/pizzas.jpg",
  "sortOrder": 1
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | string | Yes | Category name (max 100) |
| description | string | No | Description (max 500) |
| imageUrl | string | No | Image URL |
| sortOrder | Integer | No | Display order |

**Roles:** RESTAURANT_OWNER, RESTAURANT_STAFF, PLATFORM, ADMIN

---

### Update Menu Category

**PUT** `/api/v1/restaurants/{restaurantId}/menu/categories/{categoryId}`

Update menu category.

**Roles:** RESTAURANT_OWNER, RESTAURANT_STAFF, PLATFORM, ADMIN

---

### Delete Menu Category

**DELETE** `/api/v1/restaurants/{restaurantId}/menu/categories/{categoryId}`

Soft delete menu category.

**Roles:** RESTAURANT_OWNER, RESTAURANT_STAFF, PLATFORM, ADMIN

---

## 3. Menu Items

### Get All Menu Items

**GET** `/api/v1/restaurants/{restaurantId}/menu/items`

Get all menu items with pagination. Public endpoint.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number |
| size | int | 20 | Page size |

---

### Get Featured Menu Items

**GET** `/api/v1/restaurants/{restaurantId}/menu/items/featured`

Get featured menu items. Public endpoint.

---

### Search Menu Items

**GET** `/api/v1/restaurants/{restaurantId}/menu/items/search`

Search menu items. Public endpoint.

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| q | string | Yes | Search query |
| page | int | No | Page number |
| size | int | No | Page size |

---

### Get Menu Item Details

**GET** `/api/v1/restaurants/{restaurantId}/menu/items/{itemId}`

Get menu item details including variants and options. Public endpoint.

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "categoryId": 1,
    "categoryName": "Pizzas",
    "name": "Margherita Pizza",
    "description": "Classic pizza with fresh mozzarella",
    "price": 14.99,
    "priceWithMargin": 16.49,
    "originalPrice": 16.99,
    "effectivePrice": 14.99,
    "onSale": true,
    "discountPercentage": 12,
    "imageUrl": "https://example.com/margherita.jpg",
    "inStock": true,
    "featured": true,
    "prepTimeMinutes": 20,
    "calories": 850,
    "vegetarian": true,
    "vegan": false,
    "glutenFree": false,
    "spicy": false,
    "allergens": "Gluten, Dairy",
    "variants": [
      {
        "id": 1,
        "name": "Small (10\")",
        "priceDelta": -3.00,
        "totalPrice": 11.99,
        "inStock": true
      },
      {
        "id": 2,
        "name": "Large (14\")",
        "priceDelta": 4.00,
        "totalPrice": 18.99,
        "inStock": true
      }
    ],
    "options": [
      {
        "id": 1,
        "groupName": "Extra Toppings",
        "name": "Extra Cheese",
        "priceDelta": 2.00,
        "isDefault": false,
        "maxSelections": 5,
        "required": false,
        "inStock": true
      }
    ]
  }
}
```

---

### Create Menu Item

**POST** `/api/v1/restaurants/{restaurantId}/menu/items`

Create a new menu item with variants and options.

**Request Body:**
```json
{
  "categoryId": 1,
  "name": "Margherita Pizza",
  "description": "Classic pizza with fresh mozzarella, tomatoes, and basil",
  "price": 14.99,
  "originalPrice": 16.99,
  "imageUrl": "https://example.com/margherita.jpg",
  "prepTimeMinutes": 20,
  "calories": 850,
  "vegetarian": true,
  "vegan": false,
  "glutenFree": false,
  "spicy": false,
  "allergens": "Gluten, Dairy",
  "featured": true,
  "sortOrder": 1,
  "variants": [
    {
      "name": "Small (10\")",
      "priceDelta": -3.00,
      "sortOrder": 1
    },
    {
      "name": "Large (14\")",
      "priceDelta": 4.00,
      "sortOrder": 2
    }
  ],
  "options": [
    {
      "groupName": "Extra Toppings",
      "name": "Extra Cheese",
      "priceDelta": 2.00,
      "isDefault": false,
      "maxSelections": 5,
      "required": false
    }
  ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| categoryId | Long | Yes | Category ID |
| name | string | Yes | Item name (max 200) |
| description | string | No | Description (max 1000) |
| price | BigDecimal | Yes | Base price (> 0) |
| originalPrice | BigDecimal | No | Original price if on sale |
| imageUrl | string | No | Image URL |
| prepTimeMinutes | Integer | No | Prep time (1-180) |
| calories | Integer | No | Calorie count |
| vegetarian | Boolean | No | Vegetarian flag |
| vegan | Boolean | No | Vegan flag |
| glutenFree | Boolean | No | Gluten-free flag |
| spicy | Boolean | No | Spicy flag |
| allergens | string | No | Allergen info (max 500) |
| featured | Boolean | No | Featured flag |
| sortOrder | Integer | No | Display order |
| variants | List | No | Size/variant options |
| options | List | No | Add-on options |

**Roles:** RESTAURANT_OWNER, RESTAURANT_STAFF, PLATFORM, ADMIN

---

### Update Menu Item

**PUT** `/api/v1/restaurants/{restaurantId}/menu/items/{itemId}`

Update menu item.

**Roles:** RESTAURANT_OWNER, RESTAURANT_STAFF, PLATFORM, ADMIN

---

### Update Menu Item Stock

**PATCH** `/api/v1/restaurants/{restaurantId}/menu/items/{itemId}/stock`

Update menu item stock status.

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| inStock | Boolean | Yes | Stock status |

**Roles:** RESTAURANT_OWNER, RESTAURANT_STAFF, PLATFORM, ADMIN

---

### Delete Menu Item

**DELETE** `/api/v1/restaurants/{restaurantId}/menu/items/{itemId}`

Soft delete menu item.

**Roles:** RESTAURANT_OWNER, RESTAURANT_STAFF, PLATFORM, ADMIN

---

### Upload Menu Item Image

**POST** `/api/v1/restaurants/{restaurantId}/menu/items/{itemId}/image`

Upload image for menu item.

**Content-Type:** multipart/form-data

**Form Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| file | File | Yes | Image file |

**Roles:** RESTAURANT_OWNER, RESTAURANT_STAFF, PLATFORM, ADMIN

---

### Delete Menu Item Image

**DELETE** `/api/v1/restaurants/{restaurantId}/menu/items/{itemId}/image`

Delete menu item image.

**Roles:** RESTAURANT_OWNER, RESTAURANT_STAFF, PLATFORM, ADMIN

---

## Enumerations

### RestaurantStatus

| Value | Description |
|-------|-------------|
| `PENDING` | Restaurant pending approval |
| `ACTIVE` | Restaurant active and can receive orders |
| `SUSPENDED` | Restaurant temporarily suspended |
| `CLOSED` | Restaurant closed permanently |
| `REJECTED` | Restaurant application rejected |

---

## Data Transfer Objects

### RestaurantDto

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Restaurant ID |
| ownerId | Long | Owner user ID |
| name | string | Restaurant name |
| slug | string | URL-friendly slug |
| description | string | Description |
| logoUrl | string | Logo image URL |
| coverImageUrl | string | Cover image URL |
| phone | string | Phone number |
| email | string | Email address |
| fullAddress | string | Full formatted address |
| latitude | BigDecimal | Latitude |
| longitude | BigDecimal | Longitude |
| status | RestaurantStatus | Current status |
| featured | Boolean | Featured flag |
| acceptsDelivery | Boolean | Accepts delivery |
| acceptsTakeaway | Boolean | Accepts takeaway |
| acceptsDineIn | Boolean | Accepts dine-in |
| minimumOrder | BigDecimal | Minimum order amount |
| deliveryFee | BigDecimal | Delivery fee |
| deliveryRadiusKm | Integer | Delivery radius |
| averagePrepTimeMinutes | Integer | Average prep time |
| opensAt | LocalTime | Opening time |
| closesAt | LocalTime | Closing time |
| isOpen | Boolean | Manual open status |
| isCurrentlyOpen | Boolean | Currently open (time-based) |
| averageRating | BigDecimal | Average rating (0-5) |
| totalRatings | Integer | Total ratings count |
| totalOrders | Integer | Total orders count |
| createdAt | datetime | Creation timestamp |

### MenuItemDto

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Item ID |
| categoryId | Long | Category ID |
| categoryName | string | Category name |
| name | string | Item name |
| description | string | Description |
| price | BigDecimal | Base price |
| priceWithMargin | BigDecimal | Price with platform margin |
| originalPrice | BigDecimal | Original price if on sale |
| effectivePrice | BigDecimal | Display price |
| onSale | Boolean | On sale flag |
| discountPercentage | Integer | Discount percentage |
| imageUrl | string | Image URL |
| inStock | Boolean | In stock flag |
| featured | Boolean | Featured flag |
| prepTimeMinutes | Integer | Prep time in minutes |
| calories | Integer | Calorie count |
| vegetarian | Boolean | Vegetarian flag |
| vegan | Boolean | Vegan flag |
| glutenFree | Boolean | Gluten-free flag |
| spicy | Boolean | Spicy flag |
| allergens | string | Allergen information |
| variants | List\<ItemVariantDto\> | Size variants |
| options | List\<ItemOptionDto\> | Add-on options |

---

## Role Requirements Summary

| Endpoint | Public | Owner | Staff | Platform | Admin |
|----------|--------|-------|-------|----------|-------|
| Create Restaurant | - | Yes | - | Yes | Yes |
| Get Restaurant | Yes | Yes | Yes | Yes | Yes |
| Get All/Active/Featured | Yes | Yes | Yes | Yes | Yes |
| Search/Nearby | Yes | Yes | Yes | Yes | Yes |
| Get My Restaurants | - | Yes | - | Yes | Yes |
| Update Restaurant | - | Yes | Yes | Yes | Yes |
| Update Status | - | - | - | Yes | Yes |
| Toggle Open | - | Yes | Yes | Yes | Yes |
| Get Menu/Categories/Items | Yes | Yes | Yes | Yes | Yes |
| Create/Update/Delete Category | - | Yes | Yes | Yes | Yes |
| Create/Update/Delete Item | - | Yes | Yes | Yes | Yes |
| Update Item Stock | - | Yes | Yes | Yes | Yes |
| Upload/Delete Image | - | Yes | Yes | Yes | Yes |

---

## Error Responses

### 404 Restaurant Not Found

```json
{
  "success": false,
  "message": "Restaurant not found"
}
```

### 404 Menu Item Not Found

```json
{
  "success": false,
  "message": "Menu item not found"
}
```

### 403 Forbidden

```json
{
  "success": false,
  "message": "Not authorized to manage this restaurant"
}
```

### 400 Invalid Request

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": ["Price must be greater than 0"]
}
```
