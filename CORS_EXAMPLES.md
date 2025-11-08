# CORS Configuration Examples

## Example 1: Development - Allow All Origins

```yaml
# application-dev.yaml
app:
  cors:
    # Allows ALL Origins with Credentials
    allowed-origin-patterns:
      - "*"
    allow-credentials: true
    allowed-methods: [ GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD ]
    allowed-headers: [ "*" ]
    exposed-headers: [ Authorization ]
    max-age: 3600
```

```bash
# Or via environment variable
export APP_CORS_ALLOWED_ORIGIN_PATTERNS=*
```

---

## Example 2: Production - Specific Origins

```yaml
# application-prod.yaml
app:
  cors:
    # Exact origins (no wildcard)
    allowed-origins:
      - https://app.example.com
      - https://admin.example.com
    allow-credentials: true
    allowed-methods: [ GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD ]
    allowed-headers: [ "*" ]
    exposed-headers: [ Authorization ]
    max-age: 3600
```

```bash
# Or via environment variable
export APP_CORS_ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com
```

## Example 3: Mixed - Origins and Patterns

```yaml
# application.yaml
app:
  cors:
    # Specific known origins
    allowed-origins:
      - https://app.example.com
      - https://admin.example.com

    # Additional wildcard patterns
    # WARNING: allowed-origin-patterns takes precedence and overrides allowed-origins!
    # Use EITHER allowed-origins OR allowed-origin-patterns
    allowed-origin-patterns: [ ]

    allow-credentials: true
    allowed-methods: [ GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD ]
    allowed-headers: [ "*" ]
    exposed-headers: [ Authorization ]
    max-age: 3600
```

**Important**: If `allowed-origin-patterns` is set (even if empty), it takes precedence!

---

## Common Errors and Solutions

### Error: "CORS policy: The value of the 'Access-Control-Allow-Origin' header must not be the wildcard '*'"

**Solution**: Use `allowed-origin-patterns: ["*"]` instead of `allowed-origins: ["*"]`

### Error: "CORS policy: Credentials flag is 'true', but the 'Access-Control-Allow-Credentials' header is ''"

**Solution**: Set `allow-credentials: true` in the configuration

### Error: "Response to preflight request doesn't pass access control check"

**Solution**: Make sure that `OPTIONS` is included in `allowed-methods`

### Frontend does not receive JWT Token

**Solution**: Add `Authorization` to `exposed-headers`
