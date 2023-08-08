[![Quality Gate Status](http://sonarqube.dev.frachtwerk.de/api/project_badges/measure?project=de.frachtwerk%3Aspring-starter&metric=alert_status)](http://sonarqube.dev.frachtwerk.de/dashboard?id=de.frachtwerk%3Aspring-starter)

# Essencium Backend

To be used together with:

- Essencium Frontend (recommended)
- [web-starter/frontend](https://git.frachtwerk.de/fw-dev/web-starter/frontend) (version >= `1.0.0`, not recommended
  anymore)

## Requirements

- JDK 17
- Maven >= 3.x
- PostgreSQL >= 14.x (recommended) or H2 (for development purposes only)

If you are having problems resolving Maven dependencies, check the [troubleshooting](doc/troubleshooting.md) section.

## Project Structure

This repository contains a multi-module Maven project, involving:

- [`essencium-backend`](essencium-backend): Base functionality, including user management, authentication, security
  configuration, i18n, etc. This module is used as a Maven dependency in all newly generated essencium projects.
- [`essencium-backend-development`](essencium-backend-development): Minimalist example project
  using `essencium-backend-sequence-model` as library. Only used for development purposes, i.e. to quickly review and
  debug changes to the library module.
- [`essencium-backend-identity-model`](essencium-backend-identity-model): Implementation of `essencium-backend` using
  a `Long` as the primary key following Hibernates `identity` strategy.
- [`essencium-backend-sequence-model`](essencium-backend-sequence-model): Implementation of `essencium-backend` using
  a `Long` as the primary key following Hibernates `sequence` strategy.
- [`essencium-backend-uuid-model`](essencium-backend-uuid-model): Implementation of `essencium-backend` using a `Long`
  as the primary key.

## Development

### Setup

1. Import project in IntelliJ
1. Install IntelliJ plugins (`Settings -> Plugins`)
    - [Lombok](https://plugins.jetbrains.com/plugin/6317-lombok)
    - [google-java-format](https://plugins.jetbrains.com/plugin/8527-google-java-format)
1. Optional: Start Postgres database using
   the [`docker-compose.yml`](docker/database/docker-compose.yml) file

> Due to the fact that the 'git-pre-commit-hook' currently ignores the integration tests directory for correct
> formatting of the code, the following command must be executed manually after changes to the integration tests:
> `mvn org.codehaus.mojo:build-helper-maven-plugin:add-test-source@add-integration-test-sources git-code-format:format-code`

### Run

1. Run the [main class](essencium-backend-development/src/main/java/de/frachtwerk/essencium/backend/SpringBootApp.java)
   in your IDE **or** execute `mvn spring-boot:run`
1. Access the backend at http://localhost:8098

## Initial Data

> See [here](doc/initial_data.md) for documentation about initial default data.

## Custom `User` models

> See [here](doc/custom_user.md) for documentation on how to extend the default user entity.

## Security

> See [here](doc/security.md) for extensive documentation about security, authentication and authorization.

## Method-level access management & advanced permission checks

> See [here](doc/access_management.md) for extensive documentation and examples on how to restrict certain users from
> accessing specific entities or entity properties on controller method level.

> See [here](doc/advanced_permission_checks.md) for documentation and examples on how to perform fine-grained access
> control on controller method level.

## JPA Specifications

> See [here](doc/jpa_specifications.md) for details on how to write custom JPA `Specification`s for data querying.

## Postman Collection

> See [here](postman/README.md) for details on how to use our pre-built Postman collection, containing request examples
> for all backend endpoints.

## Deployment

> See [here](doc/deployment.md) for details on how to best deploy essencium projects.

## Spring Profiles

Different Spring profiles are available to provide default configuration for certain technical aspects.

* `development`: A profile to be used during development. Includes debug logging, fake e-mail service, local URLs, etc.
* `h2`: A profile for using H2 as a database backend (alternative to `postgres`, recommended for development purposes)
* `postgres`: A profile for using PostgreSQL as a database backend (alternative to `h2`, recommended in production)
* `ldap`: A profile to enable and configure LDAP-based user authentication
* `oauth`: A profile to enable and configure OAuth 2 / OpenID Connect-based user authentication

Profiles can be activated by setting `SPRING_PROFILES_ACTIVE` environment variable (comma-separated values)
or `spring.profiles.active` in YAML. By default, `h2` and `development` are active. See the section below on how to
configure individual properties, including database credentials, OAuth endpoints, etc.

## Configuration Options

The following configuration options are available and can be set either in the corresponding `application-*.yml` config
file or as environment variables. This list is not complete, but rather contains the most important variables. Special
attention shall be placed on such marked with "⚠️", as these variables are either security-critical or do not have a
meaningful default value.

Usually, Essencium-based applications will be deployed as a Docker stack. In this case, it is a good practice to specify
all configuration as environment variables inside `docker-compose.yml`.

|    | YAML Key                                         | Environment Variable                             | Default                                                       | Description                                                                                                                                                                                                                                                                                                   |
|----|--------------------------------------------------|--------------------------------------------------|---------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ⚠️ | -                                                | `APP_URL`                                        | `http://localhost:8098` in `dev` profile, undefined otherwise | Public base URL of the application (without trailing slash!)                                                                                                                                                                                                                                                  |
|    | `spring.profiles.active`                         | `SPRING_PROFILES_ACTIVE`                         | `development,h2`                                              | Which Spring profiles to use (recommended for production: `production,postgres`)                                                                                                                                                                                                                              | 
| ⚠️ | `mail.host`                                      | `MAIL_HOST`                                      | `localhost`                                                   | Mail server hostname                                                                                                                                                                                                                                                                                          |
| ⚠️ | `mail.port`                                      | `MAIL_PORT`                                      | `587`                                                         | Mail server port                                                                                                                                                                                                                                                                                              |
| ⚠️ | `mail.username`                                  | `MAIL_USERNAME`                                  | -                                                             | Mail server username                                                                                                                                                                                                                                                                                          |
| ⚠️ | `mail.password`                                  | `MAIL_PASSWORD`                                  | -                                                             | Mail server password                                                                                                                                                                                                                                                                                          |
|    | `mail.smtp.start-tls`                            | `MAIL_SMTP_START_TLS`                            | `true`                                                        | Whether to use STARTTLS for SMTP server connection                                                                                                                                                                                                                                                            |
|    | `mail.branding.*`                                | `MAIL_BRANDING_*`                                | -                                                             | Styling / CI for mail templates, see [application.yaml](essencium-backend-development/src/main/resources/application.yaml) for more                                                                                                                                                                           |
|    | `sentry.api_url`                                 | `SENTRY_API_URL`                                 | `https://sentry.frachtwerk.de/api/0/`                         | URL of Sentry instance to use                                                                                                                                                                                                                                                                                 |
|    | `sentry.organization`                            | `SENTRY_ORGANIZATION`                            | `frachtwerk`                                                  | Sentry organization                                                                                                                                                                                                                                                                                           |
| ⚠️ | `sentry.project`                                 | `SENTRY_PROJECT`                                 | -                                                             | Sentry project                                                                                                                                                                                                                                                                                                |
| ⚠️ | `sentry.token`                                   | `SENTRY_TOKEN`                                   | -                                                             | Sentry API token (for forwarding feedback requests) (obtain from at https://sentry.frachtwerk.de/settings/frachtwerk/api-keys/)                                                                                                                                                                               |
| ⚠️ | `sentry.dsn`                                     | `SENTRY_DSN`                                     | -                                                             | Sentry project DSN for error reporting                                                                                                                                                                                                                                                                        |
|    | `sentry.enable-tracing`                          | `SENTRY_ENABLE_TRACING`                          | `true`                                                        | Whether to log request traces to Sentry's performance monitoring                                                                                                                                                                                                                                              |
|    | `sentry.traces-sample-rate`                      | `SENTRY_TRACES_SAMPLE_RATE`                      | `0.1`                                                         | Percentage of requests to trace                                                                                                                                                                                                                                                                               |
|    | `app.auth.jwt.expiration`                        | `APP_AUTH_JWT_EXPIRATION`                        | `86400`                                                       | Validity of issued JWT tokens in seconds                                                                                                                                                                                                                                                                      |
| ⚠️ | `app.auth.jwt.secret`                            | `APP_AUTH_JWT_SECRET`                            | -                                                             | Secret to use for signing JWT tokens                                                                                                                                                                                                                                                                          |
|    | `app.cors.allow`                                 | `APP_CORS_ALLOW`                                 | `false`                                                       | Whether to allow CORS requests (all or nothing)                                                                                                                                                                                                                                                               |
| ⚠️ | `spring.datasource.url`                          | `SPRING_DATASOURCE_URL`                          | -                                                             | Database connection string (see [application-h2.yaml](essencium-backend-development/src/main/resources/application-h2.yaml) and [application-postres.yaml](essencium-backend-development/src/main/resources/application-postgres.yaml) for more)                                                              |
| ⚠️ | `spring.datasource.username`                     | `SPRING_DATASOURCE_USERNAME`                     | -                                                             | Database user                                                                                                                                                                                                                                                                                                 |
| ⚠️ | `spring.datasource.password`                     | `SPRING_DATASOURCE_PASSWORD`                     | -                                                             | Database password                                                                                                                                                                                                                                                                                             |
|    | `app.security.max-failed-logins`                 | `APP_SECURITY_MAX_FAILED_LOGINS`                 | `10`                                                          | Maximum amount of wrong user/password events before the user account is blocked! For LDAP / oAuth Login may a much higher limit than 5 useful.                                                                                                                                                                |
|    | `spring.security.oauth2.client.*`                | `SPRING_SECURITY_OAUTH2_CLIENT_*`                | -                                                             | OAuth 2 / OpenID Connection configuration, see [application-oauth.yaml](essencium-backend-development/src/main/resources/application-oauth.yaml) and [oauth2.md](doc/oauth2.md)                                                                                                                                  |
|    | `app.auth.ldap.*`                                | `APP_AUTH_LDAP_*`                                | -                                                             | LDAP configuration, see [application-ldap.yaml](essencium-backend-development/src/main/resources/application-ldap.yaml)                                                                                                                                                                                          |
|    | `essencium-backend.jpa.table-prefix`             | `ESSENCIUM_BACKEND_JPA_TABLE_PREFIX`             | -                                                             | Defines a prefix for te names of the database tables. `FW_` was hardcoded default in previous Starter-Versions. To support databases build on essencium-backend-versions < v2.0.0 `FW_` has to be set here.                                                                                                             |
|    | `essencium-backend.jpa.camel-case-to-underscore` | `ESSENCIUM_BACKEND_JPA_CAMEL_CASE_TO_UNDERSCORE` | -                                                             | Since Hibernate changed it's default column naming strategy this parameter was introduced to restore the old behavior. Setting this parameter to `true` hibernates current default is overwritten and default behavior is restored. This is necessary on al deployments that initially used essencium-backend < v2.x.x. |

## Tooling

- When using the default embedded H2 database the **H2 console** can be accessed at http://localhost:8098/h2-console
- An auto-generated **Swagger API documentation** can be accessed at http://localhost:8098/swagger-ui/ (trailing slash
  required)
- A **health endpoint** is provided at http://localhost:8098/actuator/health

## Testing

The backend lib comprises both unit- and integration tests.

- **Unit Tests:** `mvn -f essencium-backend/pom.xml test`
- **Integration Tests:** `mvn -f essencium-backend/pom.xml failsafe:integration-test`

### Mails

Integration Tests are able to send mails through [Ethereal](https://ethereal.email/). You
can [log in](https://ethereal.email/login) and see the messages sent using the following credentials:
> user: geo.sipes8@ethereal.email \
> pass: Du5aaBMb7VEZUjCP9M

## Translations

Translations can be defined at the time of development using `.properties` files as seeds as well as during run time via
API calls.

### Seed Files

This backend lib comes with a set of default translations for German and English, which are defined
in `essencium-backend/src/main/resources/default_translation/*.properties`.

In addition, a user can bring custom, project-specific translations by providing files of the naming
scheme `*-xx.properties` (where `xx` refers to a certain locale) in the classpath
directory `src/main/resources/translation` of the respective project.

On startup, matching files in this directory are loaded into the database. Project-specific translations take precedence
over default translations, i.e. custom translations override default translations with the same key.

Translations, which are present in a seed file, but not in the database are added. However, translations present in the
database, which are missing in the seed files, will still not get deleted from the database.

### UI

Translations can also be added at runtime via the Admin-UI.

---

Frachtwerk GmbH
