# CORS Konfiguration Beispiele

## Beispiel 1: Entwicklung - Alle Origins erlauben

```yaml
# application-dev.yaml
app:
  cors:
    # Erlaubt ALLE Origins mit Credentials
    allowed-origin-patterns:
      - "*"
    allow-credentials: true
    allowed-methods: [GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD]
    allowed-headers: ["*"]
    exposed-headers: [Authorization]
    max-age: 3600
```

```bash
# Oder via Umgebungsvariable
export APP_CORS_ALLOWED_ORIGIN_PATTERNS=*
```

---

## Beispiel 2: Production - Spezifische Origins

```yaml
# application-prod.yaml
app:
  cors:
    # Exakte Origins (kein Wildcard)
    allowed-origins:
      - https://app.example.com
      - https://admin.example.com
    allow-credentials: true
    allowed-methods: [GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD]
    allowed-headers: ["*"]
    exposed-headers: [Authorization]
    max-age: 3600
```

```bash
# Oder via Umgebungsvariable
export APP_CORS_ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com
```

---

## Beispiel 3: Multi-Tenant - Wildcard Subdomains

```yaml
# application.yaml
app:
  cors:
    # Erlaubt alle Subdomains von example.com
    allowed-origin-patterns:
      - https://*.example.com
      - https://*.staging.example.com
      - http://localhost:*
    allow-credentials: true
    allowed-methods: [GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD]
    allowed-headers: ["*"]
    exposed-headers: [Authorization]
    max-age: 3600
```

```bash
# Oder via Umgebungsvariable
export APP_CORS_ALLOWED_ORIGIN_PATTERNS=https://*.example.com,http://localhost:*
```

---

## Beispiel 4: Docker Compose Development

```yaml
version: '3.8'

services:
  backend:
    build: ./essencium-backend-development
    ports:
      - "8098:8098"
    environment:
      # CORS: Alle Origins erlauben
      APP_CORS_ALLOWED_ORIGIN_PATTERNS: "*"
      APP_CORS_ALLOW_CREDENTIALS: "true"
      
      # Database
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/essencium
      SPRING_DATASOURCE_USERNAME: essencium
      SPRING_DATASOURCE_PASSWORD: essencium
      
      # App
      APP_DOMAIN: localhost
      APP_URL: http://localhost:8098
      APP_DEFAULT_LOGOUT_REDIRECT_URL: http://localhost:3000/login
      APP_ALLOWED_LOGOUT_REDIRECT_URLS: http://localhost:3000/*
  
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: essencium
      POSTGRES_USER: essencium
      POSTGRES_PASSWORD: essencium
    ports:
      - "5432:5432"
  
  frontend:
    image: node:20
    working_dir: /app
    volumes:
      - ./frontend:/app
    command: npm run dev
    ports:
      - "3000:3000"
    environment:
      VITE_API_URL: http://localhost:8098
```

---

## Beispiel 5: Docker Compose Production

```yaml
version: '3.8'

services:
  backend:
    image: essencium-backend:1.0.0
    ports:
      - "8098:8098"
    environment:
      # CORS: Nur spezifische Origins
      APP_CORS_ALLOWED_ORIGINS: "https://app.example.com,https://admin.example.com"
      APP_CORS_ALLOW_CREDENTIALS: "true"
      APP_CORS_EXPOSED_HEADERS: "Authorization"
      
      # Database
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/essencium
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      
      # App
      APP_DOMAIN: api.example.com
      APP_URL: https://api.example.com
      APP_DEFAULT_LOGOUT_REDIRECT_URL: https://app.example.com/login
      APP_ALLOWED_LOGOUT_REDIRECT_URLS: https://app.example.com/*,https://admin.example.com/*
      
      # JWT
      APP_AUTH_JWT_ACCESS_TOKEN_EXPIRATION: 900  # 15 minutes
      APP_AUTH_JWT_REFRESH_TOKEN_EXPIRATION: 2592000  # 30 days
    restart: unless-stopped
  
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: essencium
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped

volumes:
  postgres_data:
```

---

## Beispiel 6: Kubernetes Deployment

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: essencium-backend-config
data:
  # CORS Configuration
  APP_CORS_ALLOWED_ORIGIN_PATTERNS: "https://*.example.com"
  APP_CORS_ALLOW_CREDENTIALS: "true"
  APP_CORS_EXPOSED_HEADERS: "Authorization"
  APP_CORS_MAX_AGE: "3600"
  
  # App Configuration
  APP_DOMAIN: "api.example.com"
  APP_URL: "https://api.example.com"

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: essencium-backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: essencium-backend
  template:
    metadata:
      labels:
        app: essencium-backend
    spec:
      containers:
      - name: backend
        image: essencium-backend:1.0.0
        ports:
        - containerPort: 8098
        envFrom:
        - configMapRef:
            name: essencium-backend-config
        env:
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: essencium-secrets
              key: db-password
```

---

## Beispiel 7: Gemischt - Origins und Patterns

```yaml
# application.yaml
app:
  cors:
    # Spezifische bekannte Origins
    allowed-origins:
      - https://app.example.com
      - https://admin.example.com
    
    # Zusätzliche Wildcard Patterns
    # ACHTUNG: allowed-origin-patterns hat Vorrang und überschreibt allowed-origins!
    # Verwende ENTWEDER allowed-origins ODER allowed-origin-patterns
    allowed-origin-patterns: []
    
    allow-credentials: true
    allowed-methods: [GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD]
    allowed-headers: ["*"]
    exposed-headers: [Authorization]
    max-age: 3600
```

**Wichtig**: Wenn `allowed-origin-patterns` gesetzt ist (auch wenn leer), hat es Vorrang!

---

## Testen der CORS-Konfiguration

### Mit curl

```bash
# Preflight Request
curl -X OPTIONS http://localhost:8098/api/users \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization" \
  -v

# Erwartete Response Headers:
# Access-Control-Allow-Origin: http://localhost:3000
# Access-Control-Allow-Credentials: true
# Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD
# Access-Control-Allow-Headers: Authorization
# Access-Control-Max-Age: 3600
```

### Mit Browser Console

```javascript
// In der Browser Console (z.B. von http://localhost:3000)
fetch('http://localhost:8098/api/users', {
  credentials: 'include',
  headers: {
    'Authorization': 'Bearer YOUR_TOKEN'
  }
})
.then(r => r.json())
.then(data => console.log(data))
.catch(err => console.error('CORS Error:', err));
```

---

## Häufige Fehler und Lösungen

### Fehler: "CORS policy: The value of the 'Access-Control-Allow-Origin' header must not be the wildcard '*'"

**Lösung**: Verwende `allowed-origin-patterns: ["*"]` statt `allowed-origins: ["*"]`

### Fehler: "CORS policy: Credentials flag is 'true', but the 'Access-Control-Allow-Credentials' header is ''"

**Lösung**: Setze `allow-credentials: true` in der Konfiguration

### Fehler: "Response to preflight request doesn't pass access control check"

**Lösung**: Stelle sicher, dass `OPTIONS` in `allowed-methods` enthalten ist

### Frontend erhält JWT Token nicht

**Lösung**: Füge `Authorization` zu `exposed-headers` hinzu

