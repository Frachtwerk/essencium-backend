# Changelog

## version `2.5.0`

- upgraded net.sf.okapi.lib:okapi-lib-xliff2 from 1.45.0 to 1.46.0
- Database dependencies removed from base library. See Migration Guide for more information.
- Users can now be assigned to multiple roles. The rights of the user result from the sum of the rights of the assigned roles.
- Roles and Users can now be created via environment variables. For more information see [MIGRATION.md](MIGRATION.md)
- With regard to the environment variables, the previous root element 'essencium-backend' has been renamed to 'essencium'. For more information see [MIGRATION.md](MIGRATION.md)

## Version `2.4.11`

- upgraded org.flywaydb:flyway-core from 10.3.0 to 10.4.0
- upgraded org.flywaydb:flyway-database-postgresql from 10.3.0 to 10.4.0
- upgraded org.springframework.boot:spring-boot-starter-parent from 3.2.0 to 3.2.1
- fixed NPE in LoginMailTemplate

## Version `2.4.10`

- upgraded org.flywaydb:flyway-core from 10.1.0 to 10.3.0
- upgraded org.flywaydb:flyway-database-postgresql from 10.1.0 to 10.3.0
- bump org.hibernate.orm:hibernate-jpamodelgen from 6.4.0.Final to 6.4.1.Final
- bump io.sentry:sentry-spring-boot-starter-jakarta from 7.0.0 to 7.1.0
- fix HTTP-Error 500 if an expired refresh token is used to renew an access token
- Deprecated `UnauthorizedException`. Use implementations of `org.springframework.security.core.AuthenticationException` (https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/core/AuthenticationException.html) instead.
- Deprecated `InvalidCredentialsException`. Use implementations of `org.springframework.security.core.AuthenticationException` (https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/core/AuthenticationException.html) instead.
- switch to `devnull@frachtwerk.de` as default admin user

## Version `2.4.9`

- upgraded ch.qos.logback:logback-classic from 1.4.12 to 1.4.14
- upgraded ch.qos.logback:logback-core from 1.4.12 to 1.4.14
- upgraded org.springdoc:springdoc-openapi-starter-webmvc-ui from 2.2.0 to 2.3.0
- upgraded org.apache.httpcomponents.client5:httpclient5 from 5.2.2 to 5.2.3
- upgraded org.apache.maven.plugins:maven-javadoc-plugin from 3.6.2 to 3.6.3
- fix NPE when logging in for the first time
- introduced endpoint `/auth/oauth-registrations` to list all OAuth2 registrations so that any frontend can display
  them dynamically

## Version `2.4.8`

- upgraded org.hibernate.orm:hibernate-jpamodelgen from 6.3.1.Final to 6.4.0.Final
- Introduction of the APP_DOMAIN environment variable:
  - APP_DOMAIN is used to set the domain of the cookies. APP_DOMAIN contains only the domain without protocol and port (`localhost`).
  - APP_URL is used for branding and redirects. APP_URL contains the protocol, domain and port (`http://localhost:8098`).
  - This change reverts the change of version `2.4.7` and introduces a new environment variable.

## Version `2.4.7`

- upgraded io.jsonwebtoken:jjwt-* from 0.12.2 to 0.12.3
  - several changes to internal methods for token generation
  - RefreshToken: In addition to the existing `accessToken`, a `refreshToken` is introduced. This is only required for the creation of further `accessToken` at the `/renew` endpoint. The `refreshToken` is set as a cookie that is only permitted for use at the refresh endpoint.
  - Users receive an email notification on every new login.
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
- New RegEx for Mail-Validation

## Version `2.4.6`

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

- revert maven structure changes (parent pom, child pom's for each module) due to problems with maven publishing

## Version `2.4.4`

- new maven structure (parent pom, child pom's for each module)
- dependency upgrades:
  - spring-boot: `3.1.3` -> `3.1.4` 
  - maven-javadoc-plugin: `3.5.0` -> `3.6.0`
  - flyway-core: `9.19.4` -> `9.22.2`
  - unboudid-ldapsdk: `6.0.9` -> `6.0.10`
  - hibernate-jpamodelgen: `6.2.7.Final` -> `6.3.1.Final`

## Version `2.4.3`

- bump `com.h2database:h2` from `2.2.220` to `2.2.224`
- bump `io.sentry:sentry-spring-boot-starter-jakarta` from `6.28.0` to `6.29.0`
- bump `org.apache.maven.plugins:maven-javadoc-plugin` from `3.5.0` to `3.6.0`

## Version `2.4.2`

- upgrade to Spring Boot `3.1.3`
- upgrade `com.nulab-inc:zxcvbn` from `1.8.0` to `1.8.2`
- upgrade `io.sentry:sentry-spring-boot-starter-jakarta` from `6.27.0` to `6.28.0`
- upgrade `org.springdoc:springdoc-openapi-starter-webmvc-ui` from `2.1.0` to `2.2.0`
- upgrade `org.yaml:snakeyaml` from `2.0` to `2.2`
- fix typo in `docker/build_docker_image.sh`

## Version `2.4.1`

- add `loginDisabled` flag to user dto's

## Version `2.4.0`

- Refactoring according to the new name of the project
- Cleanup Postman collection

## Version `2.3.0`

- free choice of ID strategy (Global ID, ID per table, UUID) for entities
- Introduction of three basic implementation libraries for an easy start in application development
    - essencium-backend-identity-model (using ID per table as ID strategy, on PostgreSQL)
    - essencium-backend-sequence-model (using Global ID as ID strategy, on PostgreSQL)
    - essencium-backend-uuid-model (using UUID as ID strategy)
- Restructuring of the role-rights model

<table >
    <tr>
    <th>Entity</th>
        <th>previous structure</th>
        <th>new structure</th>
        </tr>
    <tr>
        <td>User</td>
        <td><pre>
{
    "createdBy": null,
    "updatedBy": "devnull@frachtwerk.de",
    "createdAt": "2023-06-14T16:58:16.3574",
    "updatedAt": "2023-07-19T10:56:09.772018",
    "id": 18,
    "enabled": true,
    "email": "devnull@frachtwerk.de",
    "firstName": "Admin",
    "lastName": "User",
    "phone": "",
    "mobile": "",
    "locale": "en",
    "role": {
        "createdBy": null,
        "updatedBy": null,
        "createdAt": "2023-06-14T16:58:16.121704",
        "updatedAt": "2023-06-14T16:58:16.121704",
        "id": 16,
        "name": "ADMIN",
        "description": "Application Admin",
        "rights": [
            {
                "id": 1,
                "name": "USER_DELETE",
                "description": ""
            },
            ...
            {
                "id": 15,
                "name": "TRANSLATION_UPDATE",
                "description": ""
            }
        ],
        "protected": true,
        "editable": false
    },
    "source": "local"
}</pre></td>
        <td><p>ID's can be long (numbers), as well as UUID (strings) in the future.</p>
<pre>{
    "id": "d1ff0efa-cd3e-4fb1-b10d-dd7de78c9d8f",
    "firstName": "Admin",
    "lastName": "User",
    "phone": null,
    "mobile": null,
    "email": "devnull@frachtwerk.de",
    "locale": "de",
    "role": {
        "name": "ADMIN",
        "description": "Application Admin",
        "rights": [
            {
                "authority": "USER_DELETE",
                "description": ""
            },
            ...
            {
                "authority": "TRANSLATION_UPDATE",
                "description": ""
            }
        ],
        "protected": true,
        "editable": false
    }
}</pre></td>
    </tr>
    <tr><td>Role</td>
    <td><pre>{
    "createdBy": null,
    "updatedBy": null,
    "createdAt": "2023-06-14T16:58:16.121704",
    "updatedAt": "2023-06-14T16:58:16.121704",
    "id": 16,
    "name": "ADMIN",
    "description": "Application Admin",
    "rights": [
        {
            "id": 1,
            "name": "USER_DELETE",
            "description": ""
        },
        ...
        {
            "id": 15,
            "name": "TRANSLATION_UPDATE",
            "description": ""
        }
    ],
    "protected": true,
    "editable": false
}</pre></td>
        <td><pre>{
    "name": "ADMIN",
    "description": "Application Admin",
    "rights": [
        {
            "authority": "USER_DELETE",
            "description": ""
        },
        ...
        {
            "authority": "TRANSLATION_UPDATE",
            "description": ""
        }
    ],
    "protected": true,
    "editable": false
}</pre></td>
    </tr>
    <tr><td>Right</td>
    <td><pre>{
    "id": 1,
    "name": "USER_DELETE",
    "description": ""
}</pre></td><td><pre>{
    "authority": "USER_DELETE",
    "description": ""
}</pre></td></tr>
</table>

## Version `2.2.5`

- Upgraded Spring Boot from `3.1.0` to `3.1.2`
  - fixes CVE-2023-34036 (spring-hateoas)
  - fixes CVE-2023-34034 (spring-security-web & spring-security-config)
  - fixes CVE-2023-34035 (spring-security-config)
- Upgraded `io.sentry:sentry-spring-boot-starter-jakarta` from `6.25.0` to `6.25.2`
- Upgraded `com.lazerycode.jmeter:jmeter-maven-plugin` from `3.7.0` to `3.8.0`

## Version `2.2.4`

- Upgraded Spring Boot from `3.1.0` to `3.1.1`
    - fixes CVE-2023-34981
- Upgraded `com.github.tomakehurst:wiremock` from `3.0.0-beta-9` to `3.0.0-beta-10`
- Upgraded `com.h2database:h2` from `2.1.214` to `2.2.220`
- Upgraded `io.sentry:sentry-spring-boot-starter-jakarta` from `6.22.0` to `6.25.0`
- Upgraded `com.nulab-inc:zxcvbn` from `1.7.0` to `1.8.0`
- Upgraded `org.hibernate.orm:hibernate-jpamodelgen` from `6.2.4.Final` to `6.2.6.Final`

## Version `2.2.3`

- fix: Immutable Map causes UnsupportedOperationException

## Version `2.2.2`

- added GNU LGPL license header to all files

## Version `2.2.1`

- make UserRepository and RoleService accessible by inhabitants of AbstractUserService

## Version `2.2.0`

- AbstractUserController now allows to choose Representation-Entity. Default is User-Entity.

## Version `2.1.1`

- Sentry-Yaml Configuration
- Adding new Post-Method for User-Feedback (Sentry)

## Version `2.1.0`

- Upgraded Spring Boot from `3.0.6` to `3.1.0`
    - Reconfigured `HttpSecurity` since e.g. `cors()` ist deprecated and marked for removal and has to be replaced
      by `cors(Customizer.withDefaults())` (see https://github.com/spring-projects/spring-security/releases/tag/6.1.0)
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
- init `getAllowedMethods()` in AccessAwareController. This method provides a default set of allowed
  HTTP-Methods and can be overridden if needed. By default, the following methods are offered in the OPTIONS
  request: `HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE, HttpMethod.OPTIONS` (
  see https://git.frachtwerk.de/web-starter/backend/-/issues/177)

## Version `2.0.8`

- fix sentry integration (wasn't sending anymore)

## Version `2.0.7`

- dependency upgrades:
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

- Fix: LDAP-Group-Sync

> - see migration guide
> - known Issue: Only one role can be assigned to a User. If a user is in multiple groups in LDAP, only one match will
    be accepted and mapped.

## Version `2.0.5`

- Replace dedicated info endpoint with actuator

## Version `2.0.4`

- Prevent default admin creation when an admin user is already present

## Version `2.0.3`

- Set 'protected' flag for USER role to 'false'

## Version `2.0.2`

- Make admin-user configurable in environment variables
- switched from `springfox-swagger-ui` to `springdoc-openapi-starter-webmvc-ui`
    - Breaking Change in documenting an API:
        - https://springdoc.org/v2/#migrating-from-springfox

## Version `2.0.1`

- Spring Boot -> 3.0.5 (https://github.com/spring-projects/spring-boot/releases/tag/v3.0.5)

## Version `2.0.0`

### Java 17 Support

- `javax`-packages migrated to corresponding `jakarta`-packages
- code formatter upgraded from `1.39` -> `4.2`
    - added `jvm.config` according to https://github.com/Cosium/git-code-format-maven-plugin
- CI/CD-Pipeline-Images upgraded to openjdk-17
- Docker base image switched to `amazoncorretto:17`

### Spring Boot Upgrade

- Dependencies upgraded to Spring Boot 3.0.x versions

### Breaking Changes

- No more support for Java versions < 17 as several dependencies require Java 17.
- Hibernate 6.1.x Breaking Changes
    - `GenericGenerator` deprecated (see Migration)
    - Table- and entity-naming strategies have changed.
- Removed deprecated `RestrictToOwnedEntities`

### Migration

- In every project depending on this library this code snippet has to be added
  in `<project-root, same as pom.xml>/.mvn/jvm.config`:

```text
--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
```

- `org.hibernate.dialect.PostgreSQL10Dialect` has to be replaced by `org.hibernate.dialect.PostgreSQLDialect` in every
  profile-yaml using it.
- jwt-secrets have to be at least 32 char
- dependency-versions have to be checked individually
- Projects using this old code snippet have to change to the new one since `GenerationType.AUTO` isn't supported by
  Hibernate 6.1.x anymore:

```java
// old code
public class NativeIdModel extends AbstractModel {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "native")
    @SequenceGenerator(name = "native", sequenceName = "native", allocationSize = 1)
    private Long id;
}

// new code
public abstract class SequenceIdModel extends AbstractModel {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(
            name = "hibernate_sequence",
            sequenceName = "hibernate_sequence",
            allocationSize = 1)
    private Long id;
}
// If you have used `sequenceName = "native"` in your Project before, you should implement your own id using a SequenceGenerator with `sequenceName = "native"`. 
```

- Projects using this old code snippet have to change to the new one since `"org.hibernate.type.TextType` isn't
  supported by Hibernate 6.1.x anymore. Additionally `@Lob` is currently converted into CLOB by Hibernate which is
  currently unsupported by PostgreSQL.

```java
// old
public class Example extends NativeIdModel {
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String string;
}

// new
public class Example extends SequenceIdModel {
    @JdbcTypeCode(Types.LONGVARCHAR)
    private String string;
}
```

- `javax` has dto be replaced by `jakarta`
- Pipeline and docker images have to be upgraded
- To keep the "FW_" table prefix, the following has to be added in `application.yaml`:

```yaml
essencium-backend:
  jpa:
    table-prefix: "FW_"
```

- The default table naming strategy of hibernate and spring boot has changed. The new style uses camelCase column names.
  To activate the previously used `under_score`-style the following has to be added in `application.yaml`:

```yaml
essencium-backend:
  jpa:
    camel-case-to-underscore: true
```

## Version `1.26.1`

- cleanup pom.xml
- upgrade postgresql `42.4.1` -> `42.4.3`

## Version `1.26.0`

- prevent patch method from using GET-Pre- & PostProcessing of getById

## Version `1.25.1`

- resolve several vulnerabilities in `org.yaml:snakeyaml` by upgrading `1.30` -> `1.32`
- resolve vulnerability in `com.fasterxml.jackson.core:jackson-databind ` by upgrading `2.13.4` -> `2.13.4.2`
- resolve vulnerability in `org.springframework.security:spring-security-core` by upgrading `5.7.4` -> `5.7.5`
- resolve several vulnerabilities in `org.springframework.security:spring-security-oauth2-client` by
  upgrading `5.7.4` -> `5.7.5`
- resolve several vulnerabilities in `org.springframework.security:spring-security-web` by upgrading `5.7.4` -> `5.7.5`

### known vulnerabilities

- `org.springframework:spring-web:5.3.23`could be updated to `5.3.25`, but even there CVE-2016-1000027 is documented.
  Upgrading to a version >= `6.0.0` would solve this problem, but requires Java 17 and Spring Boot 3.0.0

## Version `1.25.0`

- Version Upgrade
  specification-arc-resolver. (https://github.com/tkaczmarzyk/specification-arg-resolver/blob/master/CHANGELOG.md). Only
  upgraded to version `2.15.0` since from 2.15.1 on Spring Boot 2.7.7 is required

### Relevant Changes since 2.6.0 (specification-arc-resolver)

- Bugfix for redundant joins
- JDK 17 support
- join fetch aliases in specification paths
- support for additional Types in Converter (several time formats like `Calender`, `TimeStamp`)
-

Introduced `SpecificationBuilder` (`Specification<Customer> spec = SpecificationBuilder.specification(CustomerByOrdersSpec.class).withParams("orderItem", "Pizza").build();`)

- swagger support improved
- introduced `OnTypeMismatch.IGNORE`
- introduced InTheFuture specification, that supports date-type paths
- introduced InThePast specification, that supports date-type paths

## Version `1.24.0`

- Specification for access rights check can be called separately via `AccessAwareSpecArgResolver`

### Example:

#### Procedure so far

For controller methods annotated e.g. with

```java
public class SomeController {
    @Secured("FILE_READ")
    public void someMethod();
}

// or 
public class SomeController {
    @RestrictAccessToOwnedEntities(rights = {"PERSON_READ_OWN"})
    @Secured({"PERSON_READ_ALL", "PERSON_READ_OWN"})
    public void someMethod();
}
 ```

are annotated only the presence of these rights (and possibly `_OWN` implementations) will be checked. If multiple
permissions are specified, they are OR-linked, so only one of them must be true.

#### New possibility

In case such permissions should be AND-linked and in particular the permissions of another controller method (different
controller) should also be checked, the `AccessAwareSpecArgResolver` provides the new
method `getRestrictionSpec(MethodParameter parameter, NativeWebRequest webRequest, List<Specification<Object>> baseList)`.
You get a specification with which for example the service method `existsFiltered(Specification spec)` can set the
access to further objects as a condition.

#### Example of implementation

see [doc/access_management.md#additionally-check-annotated-access-rights-of-another-method](doc/access_management.md#additionally-check-annotated-access-rights-of-another-method)

## Version `1.23.2`

- set mailAddress of ContactRequest or User (if logged in) as `Sender` in contact mails. The mail will be sent with the
  defined mail credentials, but the mailAddress will be set as `Reply to`.

## Version `1.23.1`

- Add debug receiver for rerouting all outgoing mails to one account

## Version `1.23.0`

- Update Spring-Boot to 2.7.5 (Spring-Security 5.7.4 + Spring-Framework)
- Major Update H2 Database
- Adding BeanPostProcessor in SwaggerConfig (SpringFox is not compatible with Spring 2.6+)
    - Filter out actuator controllers which don’t respect the path-matching-strategy
- Changing Path_Matching_Strategy

## Version `1.22.0`

- enable valueInSpEL-parameter in `OwnershipSpec`. Now it's possible to use dynamic default values in
  Ownership-Specification:

```java

@OwnershipSpec.And({
        @OwnershipSpec(
                path = "start",
                constVal = "#{T(java.time.LocalDate).now()}",
                valueInSpEL = true,
                spec = LessThanOrEqual.class),
        @OwnershipSpec(
                path = "end",
                constVal = "#{T(java.time.LocalDate).now()}",
                valueInSpEL = true,
                spec = GreaterThanOrEqual.class),
})
public class SomeController {
}
```

## Version `1.21.7`

- add OAuth2FailureHandler for logging
- add Proxy configuration for OAuth2

## Version `1.21.6`

- Update Dependencies from com.fasterxml.jackson.core

## Version `1.21.5`

- Mails can be sent with attachment

## Version `1.21.4`

- Fixed incompatible db default for mssql

## Version `1.21.3`

- Fixed bug in LikeConcatenated filter specification

## Version `1.21.1`

- add spring-beans to pom explicitly to please vulnerability scanners

## Version `1.21.0`

- upgrade to Spring-Boot 2.5.12

## Version `1.20.0`

- upgrade jackson core and databind versions

## Version `1.19.1`

- fix swagger errors on startup

## Version `1.19.0`

- extend user specification to allow filtering first- and lastname combinations like "firstname lastname", "lastname,
  firstname", ...

## Version `1.18.17`

- optimize Dockerfile to run commands as custom user

## Version `1.18.16`

- add lombok configuration to ignore lombok generated code in test coverage

## Version `1.18.15`

- Optimize brute force protection
    - allow to set the maximum failed logins limit before a user gets blocked
    - only listen to log in success events from `/auth/token` endpoint

## Version `1.18.13`

- Implement and publish specifications for `createdBy`, `createdAt`, `updatedBy` and `updatedAt`

## Version `1.18.12`

- Replace vulnerable log4j2 with logback (see [#122](https://git.frachtwerk.de/web-starter/backend/-/issues/122))

## Version `1.18.11`

- Clear password reset token after usage (see [#117](https://git.frachtwerk.de/web-starter/backend/-/issues/117))
- Fix user enumeration bug (using password reset function) (
  see [#116](https://git.frachtwerk.de/web-starter/backend/-/issues/116))
- Block brute-force attacks (see [#114](https://git.frachtwerk.de/web-starter/backend/-/issues/114))

## Version `1.18.10`

- Fixed `DefaultRestController` (see [#109](https://git.frachtwerk.de/web-starter/backend/-/issues/109))

## Version `1.18.9`

### Bug Fixes

- Fixed bug of translations being overwritten (see [#108](https://git.frachtwerk.de/web-starter/backend/-/issues/108))

## Version `1.18.8`

### Improvements

- Minor bugs and code smells (see [#65](https://git.frachtwerk.de/web-starter/backend/-/issues/65))

## Version `1.18.7`

- Moved password strength validation from frontend to backend (
  see [#104](https://git.frachtwerk.de/web-starter/backend/-/issues/104)

## Version `1.18.6`

### Bug Fixes

- Fixed contact mail endpoint authentication ([#107](https://git.frachtwerk.de/web-starter/backend/-/issues/107))

## Version `1.18.5`

### Bug Fixes

- Fixed contact mail translations ([#106](https://git.frachtwerk.de/web-starter/backend/-/issues/106))

## Version `1.18.4`

### Improvements

- Use of 2-digit locales (eg. "de" instead of "de_DE")

## Version `1.18.3`

* Show JVM version as part of info endpoint
* Upgrade to Spring Boot `2.5.2`

## Version `1.18.2`

### Improvements

* Map LDAP properties and OAuth attributes to user
  roles ([#80](https://git.frachtwerk.de/web-starter/backend/-/issues/80), [#84](https://git.frachtwerk.de/web-starter/backend/-/issues/84))

## Version `1.18.0`

### Improvements

- More expressive access management ([#68](https://git.frachtwerk.de/web-starter/backend/-/issues/68))

## Version `1.17.3`

- Fixed [#101](https://git.frachtwerk.de/web-starter/backend/-/issues/101)

## Version `1.17.2`

### Improvements

- Introduction of internationalization for emails (new user, reset password, contact mail)

## Version `1.17.1`

### Bugfixes

- Avoid NPE on initialization of Rights

## Version `1.17.0`

### Improvements

- Use of role ids in UserDTO's instead of whole role
  object ([#86](https://git.frachtwerk.de/web-starter/backend/-/issues/86))

---

## Version `1.16.4`

### Bug Fixes

- User session termination ([#87](https://git.frachtwerk.de/web-starter/backend/-/issues/87))

## Version `1.16.0`

### Improvements

- Introduction of `RoleDto`s for more straightforward creation and updates of
  roles ([#60](https://git.frachtwerk.de/web-starter/backend/-/issues/60))

### Bug Fixes

- SLF4J Error ([#49](https://git.frachtwerk.de/web-starter/backend/-/issues/49))

---

## Version `1.15.0`

### Bug Fixes

- When creating a user without password, a welcome mail is sent to the user containing a password reset link

### Improvements

- Switched to [Freemarker](https://freemarker.apache.org/) HTML templates for mails (contact, reset password, new user
  welcome mail)

### Technical Changes

- removed `mail.default-sender.override`, since "fake senders" are deprecated
- added `mail.branding.*` branch in profiles
- migrated `mail.user.new-user.*` to `mail.new-user-mail.*` in profiles
- migrated `mail.user.reset-token.*` to `mail.reset-token-mail.*` in profiles
- migrated `mail.contact.*` to `mail.contact-mail.*` in profiles

---

## Version `1.14.8`

### Bug Fixes

- Update right descriptions during initialization

### Improvements

- Validate certain config properties on startup
    - For instance, you must specify `MAIL_TEMPLATE_RESET_SUBJECT` now
- Add translations for default role descriptions
- Return `409` status when attempting to create user with conflicting e-mail address

## Version `1.14.7`

### Technical Changes

- Updated Spring Boot to version `2.4.2`
