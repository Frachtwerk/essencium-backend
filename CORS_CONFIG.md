# CORS Konfiguration - Quick Reference

## ✨ Alle Origins erlauben (mit Credentials)

**Ja, das ist möglich!** Verwende `allowed-origin-patterns` statt `allowed-origins`:

```bash
# Alle Origins erlauben (nur für Entwicklung!)
export APP_CORS_ALLOWED_ORIGIN_PATTERNS=*
export APP_CORS_ALLOW_CREDENTIALS=true
```

```yaml
app:
  cors:
    allowed-origin-patterns:
      - "*"
    allow-credentials: true
```

## Umgebungsvariablen für Production

### Spezifische Origins (empfohlen)

```bash
# Erforderlich: Frontend URLs
export APP_CORS_ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com

# Optional (wenn von Defaults abweichend)
export APP_CORS_ALLOW_CREDENTIALS=true
export APP_CORS_EXPOSED_HEADERS=Authorization
export APP_CORS_MAX_AGE=3600
```

### Wildcard Patterns

```bash
# Alle Subdomains
export APP_CORS_ALLOWED_ORIGIN_PATTERNS=https://*.example.com

# Mehrere Patterns
export APP_CORS_ALLOWED_ORIGIN_PATTERNS=https://*.example.com,http://localhost:*
```

## Docker Compose

### Development (alle Origins)

```yaml
environment:
  APP_CORS_ALLOWED_ORIGIN_PATTERNS: "*"
  APP_CORS_ALLOW_CREDENTIALS: "true"
```

### Production (spezifisch)

```yaml
environment:
  APP_CORS_ALLOWED_ORIGINS: "https://app.example.com"
  APP_CORS_ALLOW_CREDENTIALS: "true"
```

## Kubernetes

```yaml
env:
  - name: APP_CORS_ALLOWED_ORIGIN_PATTERNS
    value: "https://*.example.com"
  - name: APP_CORS_ALLOW_CREDENTIALS
    value: "true"
```

## Defaults (application.yaml)

```yaml
app:
  cors:
    allowed-origins:
      - http://localhost:3000
      - http://localhost:5173
      - http://localhost:8098
    allowed-origin-patterns: []  # Optional: ["*"] für alle Origins
    allow-credentials: true
    allowed-methods: [GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD]
    allowed-headers: ["*"]
    exposed-headers: [Authorization]
    max-age: 3600
```

## ⚠️ Wichtig

- **Origin Patterns haben Vorrang**: Wenn `allowed-origin-patterns` gesetzt ist, werden `allowed-origins` ignoriert
- **Wildcard `*` = ALLE Origins**: Nur in Entwicklung verwenden!
- **Credentials = true** ist erforderlich für Cookie-basierte Refresh Tokens und JWT Bearer Tokens
- **Authorization Header** muss in `exposed-headers` sein (für JWT)

## Empfohlene Konfiguration

| Umgebung    | Konfiguration                                           |
|-------------|---------------------------------------------------------|
| Development | `allowed-origin-patterns: ["*"]`                        |
| Staging     | `allowed-origins: ["https://staging.example.com"]`      |
| Production  | `allowed-origins: ["https://app.example.com"]`          |
| Multi-Tenant| `allowed-origin-patterns: ["https://*.example.com"]`    |

Vollständige Dokumentation: [CORS Configuration Guide](docs/pages/devguide/cors-configuration.mdx)

