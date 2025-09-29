# Changelog

## Version `3.0.2`

### 🌟 Features

### 🐞 Bug Fixes

- Resolves an issue at application startup where all session tokens stored in the database were deleted. This occurred because the inheritance of `DataInitializer` was not properly checked within the surrounding aspect. With this release, if a call to `BaseUserRepository.*save*()`, `RoleRepository.*save*()`, or `RightRepository.*save*()` is made from a class within a package `initialization` that also inherits `de.frachtwerk.essencium.backend.configuration.initialization.DataInitializer`, the call is ignored. All other calls to `BaseUserRepository.*save*()`, `RoleRepository.*save*()`, or `RightRepository.*save*()` will continue to invalidate the associated SessionTokens (see version `3.0.0`).

### 🔨 Dependency Upgrades

- upgraded com.h2database:h2 from 2.3.232 to 2.4.240 (for testing & development)
- upgraded com.cosium.code:git-code-format-maven-plugin from 5.3 to 5.4
- upgraded com.cosium.code:google-java-format from 5.3 to 5.4

## Version `3.0.1`

### 🌟 Features

- removed deprecated methods finally (deprecated since version `2.5.0`)
    - `GET /v1/users/me/role"`
    - `GET /v1/users/me/role/rights"`
    - `RoleService -> getById(@NotNull final String id)`
    - `RoleService -> create(Role role)`
    - `RoleService -> update(@NotNull final String name, @NotNull final Role entity)`
    - `RoleService -> getRole(@NotNull final String roleName)`

### 🐞 Bug Fixes

- Fix the ClassCastException error that occurs when Long values are used as custom claims and then retrieved.

### 🔨 Dependency Upgrades

- upgraded io.sentry:sentry-spring-boot-starter-jakarta from 8.21.1 to 8.22.0.

## Version `3.0.0`

### 🌟 Features

- migrated to `EssenciumUserDetails` as the default authentication user type
- updated `JwtAuthenticationToken` to return `EssenciumUserDetails<ID>` instead of `User`
- changed all `getPrincipal()` usages to return `EssenciumUserDetails<ID>`
- updated `UserController` and `UserService` to support new authentication user type
- added support for custom claims via `getAdditionalClaims()` in `User` entity
- removed `nonce` column from `FW_USER` table

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- upgraded io.sentry:sentry-spring-boot-starter-jakarta from 8.19.1 to 8.21.1
- upgraded org.springdoc:springdoc-openapi-starter-webmvc-ui from 2.8.10 to 2.8.13
- upgraded net.kaczmarzyk:specification-arg-resolver from 3.1.1 to 3.2.1
    - new optional env-parameter `essencium.jpa.ignore-case-strategy` (default `net.kaczmarzyk.spring.data.jpa.domain.IgnoreCaseStrategy.DATABASE_UPPER`)
      ```yaml
      essencium:
        jpa:
          ignore-case-strategy: database_upper # `database_lower`, `application` (deprecated and not recommended by library provider, see https://github.com/tkaczmarzyk/specification-arg-resolver?tab=readme-ov-file#in-version-v320)
      ```
- upgraded org.springframework.boot:spring-boot-starter-parent from 3.5.5 to 3.5.6

## Version `2.12.0`

### 🌟 Features

- Add endpoint `POST /auth/logout` to terminate current Session
    - Deletes currently used SessionToken
    - redirects to `defaultLogoutRedirectUrl` set via `AppProperties` (`app.default-logout-redirect-url`, see [MIGRATION.md](MIGRATION.md))
    - Redirection can be overwritten by QueryParam `redirectUrl` (`POST /auth/logout?redirectUrl=https...`)
    - redirect Urls have to be whitelisted in `allowedLogoutRedirectUrls` (`app.allowed-logout-redirect-urls`). This list of Strings is defaulting to an empty list. Ensure `defaultLogoutRedirectUrl` is listed here. Regex-Matching using `*` is supported.
    - Any redirection is overridden for OAuth users by the value of `OAuth2ClientRegistrationProperties.ClientProvider#logoutUri`.

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- upgraded org.springframework.boot:spring-boot-starter-parent from 3.5.4 to 3.5.5
- upgraded io.sentry:sentry-spring-boot-starter-jakarta from 8.18.0 to 8.19.1
- upgraded io.jsonwebtoken:jjwt.version from 0.12.6 to 0.13.0
- upgraded org.wiremock.integrations:wiremock-spring-boot from 3.10.0 to 3.10.6
- upgraded org.springdoc:springdoc-openapi-starter-webmvc-ui from 2.8.8 t0 2.8.10

## Version `2.11.0`

### 🌟 Features

- **Java 21**
- `ProxyAuthCodeTokenClient` – i.e. the theoretical possibility that the application contacts the authentication provider via a defined web proxy – has been removed. The implementation had no effect on the behavior of the OAuth client.

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- upgraded org.springframework.boot:spring-boot-starter-parent from 3.4.5 to 3.5.4
- upgraded io.sentry:sentry-spring-boot-starter-jakarta from 8.13.2 to 8.18.0

## Version `2.10.3`

### 🌟 Features

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- upgraded io.sentry:sentry-spring-boot-starter-jakarta from 8.5.0 to 8.13.2
- upgraded org.springdoc:springdoc-openapi-starter-webmvc-ui from 2.8.5 to 2.8.6

## Version `2.10.2`

### 🌟 Features

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- upgraded io.sentry:sentry-spring-boot-starter-jakarta from 8.4.0 to 8.5.0
- upgraded org.springframework.boot:spring-boot-starter-parent from `3.4.3` to `3.4.4`

## Version `2.10.1`

### 🌟 Features

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- upgraded org.springframework.boot:spring-boot-starter-parent from `3.4.1` to `3.4.3`
- upgraded org.springdoc:springdoc-openapi-starter-webmvc-ui from `2.8.3` to `2.8.5`
- upgraded io.sentry:sentry-spring-boot-starter-jakarta from `7.20.1` to `8.4.0`
- upgraded org.wiremock.integrations:wiremock-spring-boot from `3.6.0` to `3.9.0`
- upgraded net.kaczmarzyk:specification-arg-resolver from `3.1.0` to `3.1.1`

## Version `2.10.0`

### 🌟 Features

- switched from `com.github.tomakehurst.wiremock-standalone` to `org.wiremock.integrations:wiremock-spring-boot` for testing
- removed `commons-logging:commons-logging` from library (see [MIGRATION.md](MIGRATION.md))
- removed `jakarta.xml.bind:jakarta.xml.bind-api` from library (see [MIGRATION.md](MIGRATION.md))
- removed `org.glassfish.jaxb:jaxb-runtime` from library (see [MIGRATION.md](MIGRATION.md))
- removed `CheckedMailException` and replaced it with `MailException` & logging of the exception instead of throwing it. (see [MIGRATION.md](MIGRATION.md))
- updated license year to 2025

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- upgraded org.springframework.boot:spring-boot-starter-parent from `3.3.5` to `3.4.1`
- upgraded org.springdoc:springdoc-openapi-starter-webmvc-ui from 2.6.0 to 2.8.3
- upgraded io.sentry:sentry-spring-boot-starter-jakarta from 7.16.0 to 7.20.1

## Version `2.9.0`

### 🌟 Features

- The signature of the method `convertDtoToEntity` has been extended to include an `Optional<OUT> currentEntityOpt`, which contains the already persistent entity in the case of an update request. This can be used to carry out any necessary transfers of values from the database or validations. (see [MIGRATION.md](MIGRATION.md))
- refactor: `AbstractUserController` now extends `AbstractAccessAwareController`

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- upgraded org.springframework.boot:spring-boot-starter-parent from 3.3.3 to 3.3.5
- upgraded org.wiremock:wiremock-standalone from 3.9.1 to 3.9.2
- upgraded io.sentry:sentry-spring-boot-starter-jakarta from 7.14.0 to 7.16.0
- upgraded net.sf.okapi.lib:okapi-lib-xliff2 from 1.46.0 to 1.47.0

## Version `2.8.0`

### 🌟 Features

- `BasicRepresentation` can be used to create slim representations of entities. `BasicRepresentation` is very well suited for filling embedded objects or lists of objects in other representations.
- Each Controller now has a `GET .../basic` endpoint that returns a List of BasicRepresentations of the entity. Filtering via Specifications is possible
- `AbstractUserController` can now filter users by using a substring of an email.

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- upgraded io.sentry:sentry-spring-boot-starter-jakarta from 7.12.0 to 7.14.0
- upgraded org.hibernate.orm:hibernate-jpamodelgen from 6.5.2.Final to 6.6.0.Final
- upgraded commons-logging:commons-logging from 1.3.3 to 1.3.4
- upgraded org.springframework.boot:spring-boot-starter-parent from 3.3.2 to 3.3.3

## Version `2.7.0`

### 🌟 Features

- `AbstractUserController` now allows to test access to specific database entries before executing the actual request. This is done by using the `testAccess(<Specification>)` method provided by the `AbstractUserService`. This functionality has already been implemented and tested in the `AbstractAccessAwareController`.
- The role mapping and signup behavior can now be configured individually for each OAuth provider.

### 🐞 Bug Fixes

- Prevent NullPointerException in OAuth2SuccessHandler/AbstractUserService when OAuth-mapped internal role is not found
- Avoid calling DaoAuthenticationProvider on every request, which calls BCryptPasswordEncoder every time and causes performance problems.

### 🔨 Dependency Upgrades

- upgraded org.springframework.boot:spring-boot-starter-parent from 3.3.0 to 3.3.2
- upgraded io.jsonwebtoken:jjwt-api from 0.12.5 to 0.12.6
- upgraded io.jsonwebtoken:jjwt-impl from 0.12.5 to 0.12.6
- upgraded io.jsonwebtoken:jjwt-jackson from 0.12.5 to 0.12.6
- upgraded org.wiremock:wiremock-standalone from 3.6.0 to 3.9.0
- upgraded com.unboundid:unboundid-ldapsdk from 7.0.0 to 7.0.1
- upgraded commons-logging:commons-logging from 1.3.2 to 1.3.3
- upgraded io.sentry:sentry-spring-boot-starter-jakarta from 7.10.0 to 7.12.0
- upgraded org.springdoc:springdoc-openapi-starter-webmvc-ui from 2.5.0 to 2.6.0

## Version `2.6.0`

### 🌟 Features

- Updating the roles of a user or deleting a hole user object is only allowed, if after the request a user with at least one admin role remains in the system. (see [MIGRATION.md](MIGRATION.md))
- LDAP-Authentication - Enable Group Subtree Search, introduced environment variable `APP_AUTH_LDAP_GROUP_SEARCH_SUBTREE` (see [MIGRATION.md](MIGRATION.md))

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- upgraded io.sentry:sentry-spring-boot-starter-jakarta from 7.6.0 to 7.10.0
- upgraded org.springframework.boot:spring-boot-starter-parent from 3.2.4 to 3.3.0
- upgraded org.flywaydb:flyway-core and org.flywaydb:flyway-database-postgresql from 10.11.0 to 10.15.0
- upgraded org.hibernate.orm:hibernate-jpamodelgen from 6.4.4.Final to 6.5.2.Final
- upgraded org.wiremock:wiremock-standalone from 3.5.3 to 3.6.0
- upgraded commons-logging:commons-logging from 1.3.1 to 1.3.2
- upgraded jakarta.servlet:jakarta.servlet-api from 6.0.0 to 6.1.0

## Version `2.5.14`

### 🌟 Features

- revert `The FallbackResourceResolver has been removed. URL paths that do not exist are no longer responded to with a DefaultSuccessPage.`

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- upgraded com.nulab-inc:zxcvbn from 1.8.2 to 1.9.0

## Version `2.5.13`

### 🌟 Features

- Documentation and messages on password security improved
- The FallbackResourceResolver has been removed. URL paths that do not exist are no longer responded to with a DefaultSuccessPage.
- Make JPA-Table-Name-Style configurable, Allow disabling Upper-Case-Table-Names. Default is Upper-Case-Table-Names.

### 🐞 Bug Fixes

- Boolean logic for parsing OIDC attributes corrected. Last name and first name were not correctly separated and assigned.
- Deletion of obsolete rights failed on application start if the right was assigned to a role
- NotAllowedException during RightInitialization (see [MIGRATION.md](MIGRATION.md))
- An expired but still existing SessionToken throws an Internal Server Error (HTTP 500)
- Role-Initialization failed if no default role was defined in the application.yaml

### 🔨 Dependency Upgrades

- upgraded org.springframework.boot:spring-boot-starter-parent from 3.2.3 to 3.2.4
- upgraded com.unboundid:unboundid-ldapsdk from 6.0.11 to 7.0.0
- upgraded org.springdoc:springdoc-openapi-starter-webmvc-ui from 2.3.0 to 2.5.0
- upgraded org.flywaydb:flyway-core from 10.9.1 to 10.10.0 (demo-application)
- upgraded org.flywaydb:flyway-database-postgresql from 10.9.1 to 10.10.0 (demo-application)

## Version `2.5.12`

### 🌟 Features

- OAuth2:
    - added Support for additional redirect URI after successful login
    - added `allowed-redirect-urls` to `application.yaml` to define allowed redirect URLs
    - fixed name mapping for OAuth2-Users

### 🐞 Bug Fixes

- prevent clearing the roles of a user during patch update

### 🔨 Dependency Upgrades

- upgraded io.sentry:sentry-spring-boot-starter-jakarta from 7.4.0 to 7.6.0
- upgraded org.eclipse.angus:jakarta.mail from 2.0.2 to 2.0.3

## Version `2.5.11`

### 🌟 Features

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- upgraded org.springframework.boot:spring-boot-starter-parent from 3.2.2 to 3.2.3
    - previously pinned org.springframework.security:spring-security-core to 6.2.2 due to CVE-2024-22234, now unpinned
    - previously pinned ch.qos.logback:logback-classic and ch.qos.logback:logback-core to 1.5.0, now unpinned
- upgraded org.wiremock:wiremock-standalone from 3.4.1 to 3.4.2
- upgraded io.sentry:sentry-spring-boot-starter-jakarta from 7.3.0 to 7.4.0

## Version `2.5.10`

### 🌟 Features

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- upgraded org.postgresql:postgresql from 42.7.1 to 42.7.2 (solving CVE-2024-1597)
- upgraded org.hibernate.orm:hibernate-jpamodelgen from 6.4.3.Final to 6.4.4.Final
- upgraded org.flywaydb:flyway-core from 10.7.1 to 10.8.1
- upgraded org.flywaydb:flyway-database-postgresql from 10.7.1 to 10.8.1
- upgraded org.wiremock:wiremock-standalone from 3.3.1 to 3.4.1
- upgraded ch.qos.logback:logback-classic from 1.4.14 to 1.5.0
- upgraded ch.qos.logback:logback-core from 1.4.14 to 1.5.0
- pinned org.springframework.security:spring-security-core to 6.2.2 due to CVE-2024-22234, will be unpinned as soon as a new version of spring-boot-starter-parent is available

## Version `2.5.9`

### 🌟 Features

### 🐞 Bug Fixes

- NullPointerException in DefaultRightInitializer when right description is null

### 🔨 Dependency Upgrades

## Version `2.5.8`

### 🌟 Features

### 🐞 Bug Fixes

- time interval for deleting expired tokens

### 🔨 Dependency Upgrades

## Version `2.5.7`

### 🌟 Features

### 🐞 Bug Fixes

- Role-Update fails in DefaultUserInitializer using UUID Model

### 🔨 Dependency Upgrades

## Version `2.5.6`

### 🌟 Features

### 🐞 Bug Fixes

- duplicate violation during User-Initialization

### 🔨 Dependency Upgrades

- upgraded io.sentry:sentry-spring-boot-starter-jakarta from 7.2.0 to 7.3.0
- upgraded org.flywaydb:flyway-core from 10.6.0 to 10.7.1
- upgraded org.flywaydb:flyway-database-postgresql from 10.6.0 to 10.7.1
- upgraded io.jsonwebtoken:jjwt-api from 0.12.3 to 0.12.4
- upgraded io.jsonwebtoken:jjwt-impl from 0.12.3 to 0.12.4
- upgraded io.jsonwebtoken:jjwt-jackson from 0.12.3 to 0.12.4
- upgraded com.cosium.code:git-code-format-maven-plugin from 5.1 to 5.3
- upgraded com.cosium.code:google-java-format from 5.1 to 5.3
- upgraded org.hibernate.orm:hibernate-jpamodelgen from 6.4.2.Final to 6.4.3.Final

## Version `2.5.5`

### 🌟 Features

- The set of Rights inside RoleDto can either be a set of Strings or a Set of (Right-)Objects.

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- upgraded io.jsonwebtoken:jjwt-api from 0.12.3 to 0.12.4
- upgraded io.jsonwebtoken:jjwt-impl from 0.12.3 to 0.12.4
- upgraded io.jsonwebtoken:jjwt-jackson from 0.12.3 to 0.12.4
- upgraded org.apache.httpcomponents.client5:httpclient5 from 5.3 to 5.3.1

## Version `2.5.4`

### 🌟 Features

- Multi-Language-Support for Error-Messages

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- upgraded org.springframework.boot:spring-boot-starter-parent from 3.2.1 to 3.2.2
    - :warning: Due to a changed error handling of `HandlerMethodValidationException` on the part of Spring Boot, the notes in the migration guide must be observed. See [MIGRATION.md](MIGRATION.md) for more information.

## Version `2.5.3`

### 🌟 Features

### 🐞 Bug Fixes

- fix overwriting password during user initialization

### 🔨 Dependency Upgrades

## Version `2.5.2`

### 🌟 Features

- re-implementation of `DefaultRoleInitializer.getAdditionalRoles` to allow role initialization during development

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

## Version `2.5.1`

### 🌟 Features

- Removed deprecated `Model.class`
- Removed deprecated `NativeIdModel.class`
- Removed deprecated `InvalidCredentialsException.class`. Use one of the known subclasses of AuthenticationException instead. See https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/core/AuthenticationException.html
- Removed deprecated `UnauthorizedException.class`. Use one of the known subclasses of AuthenticationException instead. See https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/core/AuthenticationException.html
- Removed deprecated `CurrentUserController.class` which forwarded all requests to `/v1/me/*` to `/v1/users/me/*` for backward compatibility reasons.
- Removed `spring-boot-starter-hateoas` dependency from base library. See Migration Guide for more information.
- Introduced `@ExposesEntity` annotation to replace `@ExposesResourceFor` (`spring-boot-starter-hateoas`) annotation. See Migration Guide for more information.
- optimised Gender-appropriate language

### 🐞 Bug Fixes

- fix `/auth/renew` endpoint (CSRF)

### 🔨 Dependency Upgrades

- upgraded org.apache.maven.plugins:maven-surefire-plugin from 3.2.3 to 3.2.5
- upgraded org.apache.maven.plugins:maven-failsafe-plugin from 3.2.3 to 3.2.5

## Version `2.5.0`

### 🌟 Features

- Database dependencies removed from base library. See Migration Guide for more information.
- Users can now be assigned to multiple roles. The rights of the user result from the sum of the rights of the assigned roles.
- Roles and Users can now be created via environment variables. For more information see [MIGRATION.md](MIGRATION.md)
- With regard to the environment variables, the previous root element 'essencium-backend' has been renamed to '
  essencium'. For more information see [MIGRATION.md](MIGRATION.md)

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- upgraded net.sf.okapi.lib:okapi-lib-xliff2 from 1.45.0 to 1.46.0

## Version `2.4.11`

### 🌟 Features

### 🐞 Bug Fixes

- fixed NPE in LoginMailTemplate

### 🔨 Dependency Upgrades

- upgraded org.flywaydb:flyway-core from 10.3.0 to 10.4.0
- upgraded org.flywaydb:flyway-database-postgresql from 10.3.0 to 10.4.0
- upgraded org.springframework.boot:spring-boot-starter-parent from 3.2.0 to 3.2.1

## Version `2.4.10`

### 🌟 Features

- Deprecated `UnauthorizedException`. Use implementations of
  `org.springframework.security.core.AuthenticationException` (https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/core/AuthenticationException.html)
  instead.
- Deprecated `InvalidCredentialsException`. Use implementations of
  `org.springframework.security.core.AuthenticationException` (https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/core/AuthenticationException.html)
  instead.
- switch to `devnull@frachtwerk.de` as default admin user

### 🐞 Bug Fixes

- fix HTTP-Error 500 if an expired refresh token is used to renew an access token

### 🔨 Dependency Upgrades

- upgraded org.flywaydb:flyway-core from 10.1.0 to 10.3.0
- upgraded org.flywaydb:flyway-database-postgresql from 10.1.0 to 10.3.0
- upgraded org.hibernate.orm:hibernate-jpamodelgen from 6.4.0.Final to 6.4.1.Final
- upgraded io.sentry:sentry-spring-boot-starter-jakarta from 7.0.0 to 7.1.0

## Version `2.4.9`

### 🌟 Features

- introduced endpoint `/auth/oauth-registrations` to list all OAuth2 registrations so that any frontend can display them dynamically

### 🐞 Bug Fixes

- fix NPE when logging in for the first time

### 🔨 Dependency Upgrades

- upgraded ch.qos.logback:logback-classic from 1.4.12 to 1.4.14
- upgraded ch.qos.logback:logback-core from 1.4.12 to 1.4.14
- upgraded org.springdoc:springdoc-openapi-starter-webmvc-ui from 2.2.0 to 2.3.0
- upgraded org.apache.httpcomponents.client5:httpclient5 from 5.2.2 to 5.2.3
- upgraded org.apache.maven.plugins:maven-javadoc-plugin from 3.6.2 to 3.6.3

## Version `2.4.8`

### 🌟 Features

- Introduction of the APP_DOMAIN environment variable:
    - APP_DOMAIN is used to set the domain of the cookies. APP_DOMAIN contains only the domain without protocol and port (`localhost`).
    - APP_URL is used for branding and redirects. APP_URL contains the protocol, domain and port (
      `http://localhost:8098`).
    - This change reverts the change of version `2.4.7` and introduces a new environment variable.

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- upgraded org.hibernate.orm:hibernate-jpamodelgen from 6.3.1.Final to 6.4.0.Final

## Version `2.4.7`

### 🌟 Features

- several changes to internal methods for token generation
- RefreshToken: In addition to the existing `accessToken`, a `refreshToken` is introduced. This is only required for the creation of further `accessToken` at the `/renew` endpoint. The `refreshToken` is set as a cookie that is only permitted for use at the refresh endpoint.
- Users receive an email notification on every new login.
- New RegEx for Mail-Validation

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- upgraded io.jsonwebtoken:jjwt-* from 0.12.2 to 0.12.3
- upgraded org.jacoco:jacoco-maven-plugin from 0.8.10 to 0.8.11
- upgraded io.sentry:sentry-spring-boot-starter-jakarta from 6.31.0 to 6.34.0
- upgraded org.apache.maven.plugins:maven-failsafe-plugin from 3.1.2 to 3.2.2
- upgraded org.apache.maven.plugins:maven-surefire-plugin from 3.2.1 to 3.2.2
- upgraded org.apache.maven.plugins:maven-javadoc-plugin from 3.6.0 to 3.6.2
- upgraded org.springframework.boot:spring-boot-starter-parent from 3.1.5 to 3.2.0
- upgraded org.hibernate.orm:hibernate-jpamodelgen from 6.3.1.Final to 6.3.2.Final
- upgraded org.flywaydb:flyway-* from 9.22.3 to 10.1.0
- upgraded org.apache.httpcomponents:httpclient5 from 5.2.1 to 5.2.2
- upgraded org.postgresql:postgresql from 42.6.0 to 42.7.0
- upgraded org.wiremock:wiremock from 3.2.0 to 3.3.1 and switched to wiremock-standalone
- upgraded org.cyclonedx:cyclonedx-maven-plugin from 2.7.9 to 2.7.10

## Version `2.4.6`

### 🌟 Features

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- upgraded com.cosium.code:git-code-format-maven-plugin from 4.3 to 5.1
- upgraded com.cosium.code:google-java-format from 4.3 to 5.1
- upgraded io.jsonwebtoken:jjwt-jackson from 0.11.5 to 0.12.2
- upgraded io.jsonwebtoken:jjwt-impl from 0.11.5 to 0.12.2
- upgraded io.jsonwebtoken:jjwt-api from 0.11.5 to 0.12.2
- upgraded org.flywaydb:flyway-core from 9.22.2 to 9.22.3
- upgraded org.springframework.boot:spring-boot-starter-parent from 3.1.4 to 3.1.5
- upgraded io.sentry:sentry-spring-boot-starter-jakarta from 6.30.0 to 6.32.0
- upgraded org.jacoco:jacoco-maven-plugin from 0.8.10 to 0.8.11

## Version `2.4.5`

### 🌟 Features

- revert maven structure changes (parent pom, child pom's for each module) due to problems with maven publishing

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

## Version `2.4.4`

### 🌟 Features

- new maven structure (parent pom, child pom's for each module)

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- spring-boot: `3.1.3` -> `3.1.4`
- maven-javadoc-plugin: `3.5.0` -> `3.6.0`
- flyway-core: `9.19.4` -> `9.22.2`
- unboudid-ldapsdk: `6.0.9` -> `6.0.10`
- hibernate-jpamodelgen: `6.2.7.Final` -> `6.3.1.Final`

## Version `2.4.3`

### 🌟 Features

### 🐞 Bug Fixes

- fix: Immutable Map causes UnsupportedOperationException

### 🔨 Dependency Upgrades

- bump `com.h2database:h2` from `2.2.220` to `2.2.224`
- bump `io.sentry:sentry-spring-boot-starter-jakarta` from `6.28.0` to `6.29.0`
- bump `org.apache.maven.plugins:maven-javadoc-plugin` from `3.5.0` to `3.6.0`

## Version `2.4.2`

### 🌟 Features

### 🐞 Bug Fixes

- fix typo in `docker/build_docker_image.sh`

### 🔨 Dependency Upgrades

- upgrade to Spring Boot `3.1.3`
- upgrade `com.nulab-inc:zxcvbn` from `1.8.0` to `1.8.2`
- upgrade `io.sentry:sentry-spring-boot-starter-jakarta` from `6.27.0` to `6.28.0`
- upgrade `org.springdoc:springdoc-openapi-starter-webmvc-ui` from `2.1.0` to `2.2.0`
- upgrade `org.yaml:snakeyaml` from `2.0` to `2.2`

## Version `2.4.1`

### 🌟 Features

- add `loginDisabled` flag to user dto's

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

## Version `2.4.0`

### 🌟 Features

- Refactoring according to the new name of the project
- Cleanup Postman collection

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

## Version `2.3.0`

### 🌟 Features

- free choice of ID strategy (Global ID, ID per table, UUID) for entities
- Introduction of three basic implementation libraries for an easy start in application development
    - essencium-backend-identity-model (using ID per table as ID strategy, on PostgreSQL)
    - essencium-backend-sequence-model (using Global ID as ID strategy, on PostgreSQL)
    - essencium-backend-uuid-model (using UUID as ID strategy)
- Restructuring of the role-rights model

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

## Version `2.2.5`

### 🌟 Features

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- Upgraded Spring Boot from `3.1.0` to `3.1.2`
    - fixes CVE-2023-34036 (spring-hateoas)
    - fixes CVE-2023-34034 (spring-security-web & spring-security-config)
    - fixes CVE-2023-34035 (spring-security-config)
- Upgraded `io.sentry:sentry-spring-boot-starter-jakarta` from `6.25.0` to `6.25.2`
- Upgraded `com.lazerycode.jmeter:jmeter-maven-plugin` from `3.7.0` to `3.8.0`

## Version `2.2.4`

### 🌟 Features

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- Upgraded Spring Boot from `3.1.0` to `3.1.1`
    - fixes CVE-2023-34981
- Upgraded `com.github.tomakehurst:wiremock` from `3.0.0-beta-9` to `3.0.0-beta-10`
- Upgraded `com.h2database:h2` from `2.1.214` to `2.2.220`
- Upgraded `io.sentry:sentry-spring-boot-starter-jakarta` from `6.22.0` to `6.25.0`
- Upgraded `com.nulab-inc:zxcvbn` from `1.7.0` to `1.8.0`
- Upgraded `org.hibernate.orm:hibernate-jpamodelgen` from `6.2.4.Final` to `6.2.6.Final`

## Version `2.2.3`

### 🌟 Features

### 🐞 Bug Fixes

- fix: Immutable Map causes UnsupportedOperationException

### 🔨 Dependency Upgrades

## Version `2.2.2`

### 🌟 Features

- added GNU LGPL license header to all files

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

## Version `2.2.1`

### 🌟 Features

- make UserRepository and RoleService accessible by inhabitants of AbstractUserService

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

## Version `2.2.0`

### 🌟 Features

- AbstractUserController now allows to choose Representation-Entity. Default is User-Entity.

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

## Version `2.1.1`

### 🌟 Features

- Sentry-Yaml Configuration
- Adding new Post-Method for User-Feedback (Sentry)

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

## Version `2.1.0`

### 🌟 Features

- Upgraded Spring Boot from `3.0.6` to `3.1.0`
    - Reconfigured `HttpSecurity` since e.g. `cors()` ist deprecated and marked for removal and has to be replaced by `cors(Customizer.withDefaults())` (see https://github.com/spring-projects/spring-security/releases/tag/6.1.0)
- Upgraded `com.github.tomakehurst.wiremock` from `3.0.0-beta-8` to `3.0.0-beta-9`
- Upgraded `unboundid-ldapsdk` from `6.0.8` to `6.0.9`
- Upgraded `jakarta.mail` from `2.0.1` to `2.0.2`
- Upgraded `sentry-spring-boot-starter-jakarta` from `6.18.1` to `2.22.0`
- Upgraded `specification-arg-resolver` from `3.0.1` to `3.1.0`
- Upgraded `hibernate-jpamodelgen` from `6.2.1.Final` to `6.2.4.Final`
- Upgraded `yclonedx-maven-plugin` from `2.7.7` to `2.7.9`
- removed jetbrains-annotations dependency (see https://git.frachtwerk.de/web-starter/backend/-/issues/190)
- fix `createdBy` and `updatedBy` being null in PUT requests (
  see https://git.frachtwerk.de/web-starter/backend/-/issues/128)
- init `getAllowedMethods()` in AccessAwareController. This method provides a default set of allowed HTTP-Methods and can be overridden if needed. By default, the following methods are offered in the OPTIONS request:
  `HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE, HttpMethod.OPTIONS` (
  see https://git.frachtwerk.de/web-starter/backend/-/issues/177)

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

## Version `2.0.8`

### 🌟 Features

### 🐞 Bug Fixes

- fix sentry integration (wasn't sending anymore)

## Version `2.0.7`

### 🌟 Features

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- `com.github.tomakehurst.wiremock`: `3.0.0-beta-4` -> `3.0.0-beta-7`
    - test dependency, no breaking changes
- `org.springdoc.springdoc-openapi-starter-webmvc-ui`: `2.0.4` -> `2.1.0`
    - several internal dependency upgrades, no breaking changes
- `io.sentry.sentry-spring-boot-starter`: `6.16.0` -> `6.17.0`
    - several additional parameters which can be transmitted, no breaking changes
- `org.hibernate.orm.hibernate-jpamodelgen`: `6.1.7.Final` -> `6.2.1.Final`
    - deprecation of several methods:
        - deprecate GenerationTime
        - deprecate @Target, @Proxy, @Polymorphism
        - deprecate FilterKey and QuerySpacesHelper
        - deprecated CacheModeType
        - deprecate lock(entityName,...)
        - SelectionQuery.setAliasSpecificLockMode() confusion
        - deprecate LockRequest and buildLockRequest(), and have lock() accept LockOptions
        - deprecate SessionFactory.getFilterDefinition()
    - several improvements and bugfixes
    - no breaking changes concerning the essencium backend

## Version `2.0.6`

### 🌟 Features

### 🐞 Bug Fixes

- Fix: LDAP-Group-Sync

### 🔨 Dependency Upgrades

## Version `2.0.5`

### 🌟 Features

- Replace dedicated info endpoint with actuator

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

## Version `2.0.4`

### 🌟 Features

- Prevent default admin creation when an admin user is already present

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

## Version `2.0.3`

### 🌟 Features

- Set 'protected' flag for USER role to 'false'

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

## Version `2.0.2`

### 🌟 Features

- Make admin-user configurable in environment variables
- switched from `springfox-swagger-ui` to `springdoc-openapi-starter-webmvc-ui`
    - Breaking Change in documenting an API:
        - https://springdoc.org/v2/#migrating-from-springfox

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

## Version `2.0.1`

### 🌟 Features

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- Spring Boot -> 3.0.5 (https://github.com/spring-projects/spring-boot/releases/tag/v3.0.5)

## Version `2.0.0`

### 🌟 Features

- `javax`-packages migrated to corresponding `jakarta`-packages
- code formatter upgraded from `1.39` -> `4.2`
    - added `jvm.config` according to https://github.com/Cosium/git-code-format-maven-plugin
- CI/CD-Pipeline-Images upgraded to openjdk-17
- Docker base image switched to `amazoncorretto:17`
- Dependencies upgraded to Spring Boot 3.0.x versions
- No more support for Java versions < 17 as several dependencies require Java 17.
- Hibernate 6.1.x Breaking Changes
    - `GenericGenerator` deprecated (see Migration)
    - Table- and entity-naming strategies have changed.
- Removed deprecated `RestrictToOwnedEntities`

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades
