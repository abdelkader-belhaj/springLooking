# Guide Postman complet - Module Auth + User

Ce document contient tout ce qu'il faut pour tester rapidement l'API:
- methodes HTTP
- URLs
- headers
- bodies JSON
- scripts Postman pour copier token, userId, adminId
- ordre de test recommande

## 1) Base URL et variables d'environnement

Creer un Environment Postman avec:
- baseUrl = http://localhost:8080
- tokenAdmin =
- tokenUser =
- adminId =
- userId =

Headers communs:
- Content-Type: application/json
- Authorization: Bearer {{tokenAdmin}} ou Bearer {{tokenUser}} (pour routes protegees)

## 2) Ordre de test recommande

1. Register Admin
2. Register User
3. Login Admin
4. Login User
5. Tester routes /api/users
6. Tester cas d'erreur

---

## 3) AUTH - Requetes

### A. Register Admin
- Methode: POST
- URL: {{baseUrl}}/api/auth/register
- Header: Content-Type: application/json
- Body:

```json
{
  "username": "admin1",
  "email": "admin1@test.com",
  "password": "123456",
  "role": "ADMIN"
}
```

Tests Postman (copie token + id admin):

```javascript
let res = pm.response.json();
pm.environment.set("tokenAdmin", res.data.token);
pm.environment.set("adminId", res.data.user.id);
```

### B. Register User
- Methode: POST
- URL: {{baseUrl}}/api/auth/register
- Header: Content-Type: application/json
- Body:

```json
{
  "username": "client1",
  "email": "client1@test.com",
  "password": "123456",
  "role": "CLIENT_TOURISTE"
}
```

Tests Postman (copie token + id user):

```javascript
let res = pm.response.json();
pm.environment.set("tokenUser", res.data.token);
pm.environment.set("userId", res.data.user.id);
```

### C. Login Admin
- Methode: POST
- URL: {{baseUrl}}/api/auth/login
- Header: Content-Type: application/json
- Body:

```json
{
  "email": "admin1@test.com",
  "password": "123456"
}
```

Tests Postman:

```javascript
let res = pm.response.json();
pm.environment.set("tokenAdmin", res.data.token);
pm.environment.set("adminId", res.data.user.id);
```

### D. Login User
- Methode: POST
- URL: {{baseUrl}}/api/auth/login
- Header: Content-Type: application/json
- Body:

```json
{
  "email": "client1@test.com",
  "password": "123456"
}
```

Tests Postman:

```javascript
let res = pm.response.json();
pm.environment.set("tokenUser", res.data.token);
pm.environment.set("userId", res.data.user.id);
```

---

## 4) USER - Requetes

Important: ces routes demandent Authorization Bearer token.

### A. Get all users
- Methode: GET
- URL: {{baseUrl}}/api/users
- Header:
  - Authorization: Bearer {{tokenAdmin}}

### B. Get user by id
- Methode: GET
- URL: {{baseUrl}}/api/users/{{userId}}
- Header:
  - Authorization: Bearer {{tokenUser}}

### C. Update user
- Methode: PUT
- URL: {{baseUrl}}/api/users/{{userId}}
- Headers:
  - Content-Type: application/json
  - Authorization: Bearer {{tokenUser}}
- Body:

```json
{
  "username": "client1_updated",
  "email": "client1_updated@test.com"
}
```

### D. Change password
- Methode: PATCH
- URL: {{baseUrl}}/api/users/{{userId}}/password
- Headers:
  - Content-Type: application/json
  - Authorization: Bearer {{tokenUser}}
- Body:

```json
{
  "oldPassword": "123456",
  "newPassword": "abcdef",
  "confirmPassword": "abcdef"
}
```

Apres changement password, refaire Login User avec nouveau password.

### E. Change role (ADMIN only)
- Methode: PATCH
- URL: {{baseUrl}}/api/users/{{userId}}/role?role=HEBERGEUR
- Header:
  - Authorization: Bearer {{tokenAdmin}}

### F. Toggle enabled (ADMIN only)
- Methode: PATCH
- URL: {{baseUrl}}/api/users/{{userId}}/toggle
- Header:
  - Authorization: Bearer {{tokenAdmin}}

### G. Get users by role
- Methode: GET
- URL: {{baseUrl}}/api/users/role/HEBERGEUR
- Header:
  - Authorization: Bearer {{tokenAdmin}}

### H. Delete user (ADMIN only)
- Methode: DELETE
- URL: {{baseUrl}}/api/users/{{userId}}
- Header:
  - Authorization: Bearer {{tokenAdmin}}

---

## 5) Roles valides

- ADMIN
- CLIENT_TOURISTE
- HEBERGEUR
- TRANSPORTEUR
- AIRLINE_PARTNER
- ORGANISATEUR
- VENDEUR_ARTI
- SOCIETE

---

## 6) Erreurs frequentes et cause

### 403 Forbidden
Cause la plus frequente:
- token absent
- token invalide/expire
- mauvais header Authorization
- role insuffisant sur endpoints admin

Verifier:
- Authorization: Bearer <token>
- pas d'espace en trop
- token recopie depuis login recent

### 400 Bad Request
- body invalide
- oldPassword faux
- newPassword != confirmPassword
- email deja utilise

### 401 Unauthorized
- credentials login invalides

---

## 7) Mini checklist rapide

1. Login et recuperer token
2. Mettre Authorization Bearer token
3. Tester GET /api/users
4. Tester PATCH /api/users/{id}/password
5. Refaire login avec nouveau password
6. Tester endpoints admin avec tokenAdmin

Fin du guide.
