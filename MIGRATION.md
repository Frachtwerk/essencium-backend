# Migration Guide

## Migrate to `2.4.8`

Reverting the changes made in `2.4.7` to the environment variable `APP_URL` and the corresponding property `app.url` a
new environment variable called `APP_DOMAIN` is introduced. This variable is by default used to build the `app.url`
property.

| environment variable | property   | default value                                        | description                                                                                                                      |
|----------------------|------------|------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------|
| APP_DOMAIN           | app.domain | `localhost`                                          | The domain of the application without any protocol or port. APP_DOMAIN is used to set the domain of the cookies (refresh token). |
| APP_URL              | app.url    | `http://${app.domain}:8098`, `http://localhost:8098` | The URL of the application. APP_URL is used for branding and redirects                                                           |

## Migrate to `2.4.7`

- If you have extendend `AbstractUserService`, you fave to update your Constructor adding `JwtTokenService`.

```java

@Service
public class UserService extends AbstractUserService<User, Long, UserInput> {

    protected UserService(
            @NotNull UserRepository userRepository,
            @NotNull PasswordEncoder passwordEncoder,
            @NotNull UserMailService userMailService,
            @NotNull RoleService roleService,
            @NotNull JwtTokenService jwtTokenService) {
        super(userRepository, passwordEncoder, userMailService, roleService, jwtTokenService);
    }
// ...
}
```

- If you have added the Annotation `@EnableAsync` somehow, you have to remove it. In Essencium Asyncroneous execution is
  enabled by default.
- If you use Flyway in your application, please note that in addition to the core library, the specific implementation
  for your DBMS must also be inserted in the `pom.xml`. Example for PostgreSQL databases:

```xml
        <!-- Flyway Database Migration Dependencies -->
<!-- https://mvnrepository.com/artifact/org.flywaydb/flyway-core -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <version>10.1.0</version>
</dependency>
<dependency>
<groupId>org.flywaydb</groupId>
<artifactId>flyway-database-postgresql</artifactId>
<version>10.1.0</version>
</dependency>
```

- You'll have to add following properties in your `application.yml` (Please note that this can also apply to unit and
  integration tests):

```yaml
mail:
  # ... previous values for host, port, username, password, default-sender, smtp, branding, new-user-mail, reset-token-mail, contact-mail
  new-login-mail:
    subject-key: mail.new-login.subject
    template: NewLoginMessage.ftl
```

- JWT-Processing has been restructured. Please add the following parameters in your `application.yaml`:

```yaml
app:
  auth:
    jwt:
      access-token-expiration: 86400 # 24 hours
      refresh-token-expiration: 2592000 # 30 days
      issuer: Frachtwerk GmbH
      cleanup-interval: 3600 # 1 hour
```

- The parameter `app.url` has to be given without any pre- or suffix. (e.g. `app.url: localhost`)

- In Dockerfiles, the previous ENTRYPOINT must be changed from:

```Dockerfile
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom org.springframework.boot.loader.JarLauncher" ]
```

to:

```Dockerfile
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom org.springframework.boot.loader.launch.JarLauncher" ]
```

See https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.2-Release-Notes for details.

## Migrate to `2.4.1`

- If you are using `essencium-backend` (not one of the model implementations) you have to update your UserService and
  UserRepresentationAssembler implementations. `UserDto` as input brings a new boolean-field named `loginDisabled` which
  has to be handled in the implementations. Same goes for the `UserRepresentation` which should have a new boolean-field
  named `loginDisabled` as well.

## Migrate to `2.4.0`

- Replace all `spring-starter-*` dependencies with the corresponding `essencium-backend-*` dependencies.
- Replace all `de.frachtwerk.starter.backend.*` imports with the corresponding `de.frachtwerk.essencium.backend.*`
  imports.

## Migrate to `2.3.0`

- Either choose one of the implementation libraries or build directly on `essencium-backend`.
- When you build on `essencium-backend`, you need to implement the `User` entity, as well as all associated
  services (`UserService`, `UserRepository`, `UserRepresentation`, `UserRepresentationAssembler` and `UserController`)
  in your application. For a more comfortable development result it is recommended to implement the
  classes `AbstractModel`, `ModelSpec`, `AssemblingService` and `DefaultAssemblingEntityService` as well and build on
  them afterwards. Example implementations can be found below.
- Adjust the import paths for `User`, `Role` and `Right` according to the new implementations.
- Fix all other code issues that arise from the new implementation. ;-)
- Due to the changed role rights model, existing databases have to be migrated. An example migration script (which also
  works on empty databases and can be used with e.g. Flyway) can be found below.

**All examples are based on the SequenceID model. For the other variants it is recommended to use the corresponding
implementation libraries as a template.**

### User implementation

```java

@Entity
public class User extends AbstractBaseUser<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(
            name = "hibernate_sequence",
            sequenceName = "hibernate_sequence",
            allocationSize = 1)
    private Long id;
}
```

### UserRepository implementation

```java

@Repository
public interface UserRepository extends BaseUserRepository<User, Long> {
}
```

### UserService implementation

```java

@Service
public class UserService extends AbstractUserService<User, Long, UserDto<Long>> {

    protected UserService(
            @NotNull UserRepository userRepository,
            @NotNull PasswordEncoder passwordEncoder,
            @NotNull UserMailService userMailService,
            @NotNull RoleService roleService) {
        super(userRepository, passwordEncoder, userMailService, roleService);
    }

    @Override
    protected @NotNull <E extends UserDto<Long>> User convertDtoToEntity(@NotNull E entity) {
        Role role = roleService.getById(entity.getRole());
        return User.builder()
                .email(entity.getEmail())
                .enabled(entity.isEnabled())
                .role(role)
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .locale(entity.getLocale())
                .mobile(entity.getMobile())
                .phone(entity.getPhone())
                .source(entity.getSource())
                .id(entity.getId())
                .build();
    }

    @Override
    public UserDto<Long> getNewUser() {
        return new UserDto<>();
    }
}
```

### UserController implementation

```java

@RestController
@RequestMapping("/v1/users")
public class UserController
        extends AbstractUserController<
        User, UserRepresentation, AppUserDto, BaseUserSpec<User, Long>, Long> {

    protected UserController(UserService userService, UserAssembler assembler) {
        super(userService, assembler);
    }
}
```

### UserRepresentation implementation

```java
public class UserRepresentation {
    private Long id;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String firstName;
    private String lastName;
    private String phone;
    private String mobile;
    private String email;
    private Locale locale;
    private Role role;
}
```

### UserRepresentationAssembler implementation

```java

@Primary
@Component
public class UserAssembler extends AbstractRepresentationAssembler<User, UserRepresentation> {
    @Override
    public @NonNull UserRepresentation toModel(@NonNull User entity) {
        return UserRepresentation.builder()
                .id(entity.getId())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .phone(entity.getPhone())
                .mobile(entity.getMobile())
                .email(entity.getEmail())
                .locale(entity.getLocale())
                .role(entity.getRole())
                .build();
    }
}
```

### AbstractModel implementation

```java
public abstract class AbstractModel extends SequenceIdModel {
}
```

### ModelSpec implementation

```java
public interface ModelSpec<T extends SequenceIdModel> extends BaseModelSpec<T, Long> {
}
```

### AssemblingService implementation

```java
public interface AssemblingService<M extends AbstractBaseModel, R> {

    AbstractRepresentationAssembler<M, R> getAssembler();

    default R toOutput(M entity) {
        return getAssembler().toModel(entity);
    }

    default Page<R> toOutput(Page<M> page) {
        if (page == null) {
            return null;
        }
        return page.map(this::toOutput);
    }
}
```

### DefaultAssemblingEntityService implementation

```java
public abstract class DefaultAssemblingEntityService<M extends SequenceIdModel, IN, OUT>
        extends AbstractEntityService<M, Long, IN> implements AssemblingService<M, OUT> {

    @Getter
    private final AbstractRepresentationAssembler<M, OUT> assembler;

    protected DefaultAssemblingEntityService(
            final AbstractRepository<M> repository,
            final AbstractRepresentationAssembler<M, OUT> assembler) {
        super(repository);
        this.assembler = assembler;
    }
}
```

### Migration script

```sql
-- create potentially non-existing sequences
CREATE SEQUENCE IF NOT EXISTS hibernate_sequence
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

-- create potentially non-existing tables
CREATE TABLE IF NOT EXISTS "FW_USER"
(
    id                    bigint                 NOT NULL,
    created_at            timestamp(6) without time zone,
    created_by            character varying(255),
    updated_at            timestamp(6) without time zone,
    updated_by            character varying(255),
    email                 character varying(150),
    enabled               boolean                NOT NULL,
    failed_login_attempts integer                NOT NULL DEFAULT 0,
    first_name            character varying(255),
    last_name             character varying(255),
    locale                character varying(255) NOT NULL,
    login_disabled        boolean                NOT NULL,
    mobile                character varying(255),
    nonce                 character varying(255),
    password              character varying(255),
    password_reset_token  character varying(255),
    phone                 character varying(255),
    source                character varying(255),
    role_id               bigint                 NOT NULL,
    CONSTRAINT "FW_USER_pkey" PRIMARY KEY (id),
    CONSTRAINT uk_o5gwjnjfosht4tf5lq48rxfoj UNIQUE (email)
);
CREATE TABLE IF NOT EXISTS "FW_RIGHT"
(
    id          bigint                 NOT NULL,
    created_at  timestamp(6) without time zone,
    created_by  character varying(255),
    updated_at  timestamp(6) without time zone,
    updated_by  character varying(255),
    description character varying(512),
    name        character varying(255) NOT NULL,
    CONSTRAINT "FW_RIGHT_pkey" PRIMARY KEY (id),
    CONSTRAINT uk_jep1itavphekmnphj0vp38s9u UNIQUE (name)
);
CREATE TABLE IF NOT EXISTS "FW_ROLE"
(
    id           bigint                 NOT NULL,
    created_at   timestamp(6) without time zone,
    created_by   character varying(255),
    updated_at   timestamp(6) without time zone,
    updated_by   character varying(255),
    description  character varying(255),
    is_protected boolean                NOT NULL,
    name         character varying(255) NOT NULL,
    CONSTRAINT "FW_ROLE_pkey" PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS "FW_ROLE_RIGHTS"
(
    role_id   bigint NOT NULL,
    rights_id bigint NOT NULL,
    CONSTRAINT "FW_ROLE_RIGHTS_pkey" PRIMARY KEY (role_id, rights_id)
);

-- USER -> ROLE
ALTER TABLE IF EXISTS "FW_USER"
    ADD COLUMN "role_name" VARCHAR(255);

UPDATE "FW_USER"
SET role_name = role.name
FROM "FW_ROLE" role
WHERE role.id = "FW_USER".role_id;

ALTER TABLE IF EXISTS "FW_USER"
    DROP CONSTRAINT IF EXISTS "FKnpftnul0ve9guxtoqakx201de";

ALTER TABLE IF EXISTS "FW_USER"
    DROP COLUMN IF EXISTS role_id;

-- ROLE -> RIGHT
ALTER TABLE IF EXISTS "FW_ROLE_RIGHTS"
    ADD COLUMN "role_name" VARCHAR(255);

UPDATE "FW_ROLE_RIGHTS"
SET role_name = role.name
FROM "FW_ROLE" role
WHERE role.id = "FW_ROLE_RIGHTS".role_id;

-- RIGHT --> ROLE
ALTER TABLE IF EXISTS "FW_ROLE_RIGHTS"
    ADD COLUMN "rights_authority" VARCHAR(255);
ALTER TABLE IF EXISTS "FW_RIGHT"
    RENAME name TO authority;

UPDATE "FW_ROLE_RIGHTS"
SET rights_authority = appright.authority
FROM "FW_RIGHT" appright
WHERE appright.id = "FW_ROLE_RIGHTS".rights_id;

-- Remove old Constraints
ALTER TABLE IF EXISTS "FW_ROLE_RIGHTS"
    DROP CONSTRAINT IF EXISTS "FK4akiafdy6sibodflxw662bf0x";
ALTER TABLE IF EXISTS "FW_ROLE_RIGHTS"
    DROP CONSTRAINT IF EXISTS "FKc28mpb53220tvxffq0buv1sc6";

-- Remove old Primary Keys
ALTER TABLE IF EXISTS "FW_ROLE_RIGHTS"
    DROP CONSTRAINT IF EXISTS "FW_ROLE_RIGHTS_pkey";

ALTER TABLE IF EXISTS "FW_ROLE"
    DROP COLUMN IF EXISTS id;
ALTER TABLE IF EXISTS "FW_RIGHT"
    DROP COLUMN IF EXISTS id;

-- Remove old Columns
ALTER TABLE IF EXISTS "FW_ROLE_RIGHTS"
    DROP COLUMN "role_id";
ALTER TABLE IF EXISTS "FW_ROLE_RIGHTS"
    DROP COLUMN "rights_id";

-- Add new Primary Keys
ALTER TABLE IF EXISTS "FW_ROLE"
    ADD CONSTRAINT "FW_ROLE_pkey" PRIMARY KEY ("name");
ALTER TABLE IF EXISTS "FW_RIGHT"
    ADD CONSTRAINT "FW_RIGHT_pkey" PRIMARY KEY ("authority");
ALTER TABLE IF EXISTS "FW_ROLE_RIGHTS"
    ADD CONSTRAINT "FW_ROLE_RIGHT_pkey" PRIMARY KEY ("role_name", "rights_authority");

-- Add new Constraints
ALTER TABLE IF EXISTS "FW_USER"
    ADD CONSTRAINT "FK8xvm8eci4kcyn46nr2xd4axx9" FOREIGN KEY ("role_name") REFERENCES "FW_ROLE" ("name") MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION;
ALTER TABLE IF EXISTS "FW_ROLE_RIGHTS"
    ADD CONSTRAINT "FKhqod6jll49rbgohaml3pi5ofi" FOREIGN KEY ("rights_authority")
        REFERENCES "FW_RIGHT" ("authority") MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION;
ALTER TABLE IF EXISTS "FW_ROLE_RIGHTS"
    ADD CONSTRAINT "FKillb2aaughbvyxj9j8sa9835g" FOREIGN KEY ("role_name")
        REFERENCES "FW_ROLE" ("name") MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION;

-- Delete obsolete Columns
ALTER TABLE IF EXISTS "FW_RIGHT"
    DROP COLUMN IF EXISTS created_at;
ALTER TABLE IF EXISTS "FW_RIGHT"
    DROP COLUMN IF EXISTS created_by;
ALTER TABLE IF EXISTS "FW_RIGHT"
    DROP COLUMN IF EXISTS updated_at;
ALTER TABLE IF EXISTS "FW_RIGHT"
    DROP COLUMN IF EXISTS updated_by;

ALTER TABLE IF EXISTS "FW_ROLE"
    DROP COLUMN IF EXISTS created_at;
ALTER TABLE IF EXISTS "FW_ROLE"
    DROP COLUMN IF EXISTS created_by;
ALTER TABLE IF EXISTS "FW_ROLE"
    DROP COLUMN IF EXISTS updated_at;
ALTER TABLE IF EXISTS "FW_ROLE"
    DROP COLUMN IF EXISTS updated_by;
```

## Migrate to `2.2.0`

Projects overriding UserController may now use AbstractUserController with 4 parameters:

- `U extends User` as handled Entity
- `R` as Representation
- `T extends UserDto` as Input-Entity
- `S extends UserSpec<U>` as Specification

By Default the AbstractUserController is implemented as follows:

```java

@RestController
@RequestMapping("/v1/users")
public class UserController extends AbstractUserController<User, User, UserDto, UserSpec<User>> {

    public UserController(UserService userService, UserRepresentationDefaultAssembler assembler) {
        super(userService, assembler);
    }
}
```

If you wish to use an own Representation, you have to do three steps:

1. Write your own controller
2. Write your own Representation
3. Write your own Assembler

```java

@RestController
@RequestMapping("/v1/users")
public class MyUserController extends AbstractUserController<MyUser, MyUserRepresentation, UserDto, UserSpec<User>> {
    public UserController(UserService userService, MyUserRepresentationAssembler assembler) {
        super(userService, assembler);
    }
}

public class MyUserRepresentation {
    private Long id;
    private String firstName;
    private String lastName;
}

@Primary        // <- to disable the UserRepresentationDefaultAssembler
@Component
public class MyUserRepresentationAssembler
        extends AbstractRepresentationAssembler<User, MyUserRepresentation> {
    @Override
    public @NonNull MyUserRepresentation toModel(@NonNull User entity) {
        return MyUserRepresentation.builder()
                .id(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .build();
    }
}
```

## Migrate to `2.1.1`

- Configure the following Sentry properties in the application.yaml:
    - api_url
    - organization
    - project
- `environment`, `token` and `dsn` should be set in the Container Stacks
- You can find other Configurations in the `application.yaml`. Like Version tracking `release: @project.version@`
- New `/sentry/feedback` Post-Endpoint
    - Input:
      `{
      "eventId": "c3e0aefba2cd43c1b4570f96d14e14dc",
      "name": "Test User",
      "email": "test@mail.de",
      "comments": "Comment to Issue"
      }`
        - The eventId refers to the Sentry Issue Id.

## Migrate to `2.1.0`

- Replace `org.jetbrains.annotations.NotNull` with `jakarta.validation.constraints.NotNull` in all files
- Replace `org.jetbrains.annotations.Nullable` with `jakarta.annotation.Nullable` in all files
- If you have overridden or extended the `DefaultRoleInitializer` you have to change the
  constructor from `public MyRoleInitializer(RightService rightService, RoleService roleService) `
  to `public MyRoleInitializer(RightService rightService, RoleService roleService, DefaultRoleProperties defaultRoleProperties)`
- If you have accessed `USER_ROLE_NAME` or `USER_ROLE_DESCRIPTION` provided by `DefaultRoleInitializer` you have to
  change it to `defaultRoleProperties.getName()` and `defaultRoleProperties.getDescription()` respectively.

## Migrate to `2.0.6`

LDAP group sync

- remove `userRoleAttr` from any configuration
- the following parameters have to be set:
    - `groupSearchBase`
    - `groupSearchFilter`
    - `defaultRole`
- if you know what you are doing even `groupRoleAttribute` can be set.

The complete list of ldap associated env variables:

```yaml
app:
  auth:
    ldap:
      enabled: true
      allow-signup: true
      update-role: true
      user-search-base: ou=users
      user-search-filter: (mail={0})
      group-search-base: ou=groups
      group-search-filter: (member={0})
      # group-role-attribute: spring.security.ldap.dn # default value
      # default-role: USER # default value
      user-firstname-attr: givenName # default = "notSet"
      user-lastname-attr: sn # default = "notSet
      url: ldap://ldap.admin.frachtwerk.de:389/dc=user,dc=frachtwerk,dc=de
      manager-dn: uid=service.user,ou=services,ou=users,dc=user,dc=frachtwerk,dc=de
      manager-password: serviceuserpassword
      roles:
        - src: 'cn=admins,ou=groups,dc=user,dc=frachtwerk,dc=de'
          dst: 'ADMIN'
        - src: 'cn=users,ou=groups,dc=user,dc=frachtwerk,dc=de'
          dst: 'USER'
        # if 'group-role-attribute' is set to 'cn' roles can be mapped like this:
        # - src: 'intern'
        #   dst: 'USER'
```

## Migrate to `2.0.2`

Breaking Changes concerning openApi documentation

- If there are any project specific swagger-configurations, delete them.
- Follow this (https://springdoc.org/v2/#migrating-from-springfox) migration guide to update your api-documentation

## Migrate to `2.0.0`

### Environment

- The file `.mvn/jvm.config` with the following content must be created in the project root directory. This is necessary
  because due to an upgrade of the formatter maven has to be configured in advance.

```config
--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
```

- All Java-based images in the pipeline, as well as the Dockerfiles, must be changed to Java-17-based images.

### Maven (pom.xml)

- The property `java.version` must be changed to `17`
- The version of `org.springframework.boot.spring-boot-starter-parent` must be changed to `3.0.4`
- The version of `de.frachtwerk.spring-essencium-backend` must be changed to `2.0.0`
- The dependency `spring-boot-properties-migrator` has to be added

```xml
        <!-- https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-properties-migrator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-properties-migrator</artifactId>
</dependency>
```

- The dependency `com.github.tomakehurst.wiremock-jre8` has to be replaced by `com.github.tomakehurst.wiremock` using
  version `3.0.0-beta-4` (or higher)
- The Version of `com.h2database.h2` must be changed to `2.1.214`
- The Version of `org.postgresql.postgresql` must be changed to `42.5.4`
- The Version of `net.kaczmarzyk.specification-arg-resolver` must be changed to `3.0.1`
- If used `org.springframework.security.spring-security-oauth2-client`
  and `org.springframework.security.spring-security-oauth2-jose` have to be replaced
  by `org.springframework.boot.spring-boot-starter-oauth2-resource-server`
  and `org.springframework.boot.spring-boot-starter-oauth2-client`
- `javax`-packages have to be migrated to corresponding `jakarta`-packages, except `javax.servlet-api` (there is no
  corresponding jakarta-package)
- Every `fasterxml`-dependency has to be removed.
- The Dependency `com.cosium.code.maven-git-code-format` has to be replaced
  by `com.cosium.code.git-code-format-maven-plugin` using version `4.2` (or higher)
    -
  Use `mvn org.codehaus.mojo:build-helper-maven-plugin:add-test-source@add-integration-test-sources git-code-format:format-code`
  to format your code completely (including Integration Tests, see [README](./README.md))

:warning: Individually, the version statuses of each dependency used must be checked and updated.

### Code / Application

- `org.hibernate.dialect.PostgreSQL10Dialect` has to be replaced by `org.hibernate.dialect.PostgreSQLDialect` in every
  profile-yaml using it.
- jwt-secrets have to be at least 32 char
- `Model` and `NativeIdModel` have been deprecated. Consider using `IdentityIdModel` respectively `SequenceIdModel`
  instead. :warning: be aware of the sequence names in existing Databases
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

- `javax` has to be replaced by `jakarta`
- Pipeline and docker images have to be upgraded
- To keep the `FW_` table prefix, the following has to be added in `application.yaml`:

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

## Migrate to `1.23.0`

- Change Spring Version to `2.7.5`
- ResourceProperties (spring.resources) is deprecated. If you need to access resources information, you need to inject
  WebProperties and access its getResources accessor.
    - Change `Resources resources` to `WebProperties webproperties` and access the Information
      with `webproperties.getResources()`
- Major Update in H2 Database:
    - The maximum length of CHARACTER, CHARACTER VARYING and VARCHAR_IGNORECASE, columns, BINARY, BINARY VARYING,
      JAVA_OBJECT, GEOMETRY and JSON values is 1_000_000_000 characters.
    - `NEXTVAL` is replaced by `NEXT VALUE FOR`
    - Only Compatible in Legacy Mode - use the database URL jdbc:h2:~/test;MODE=LEGACY or the SQL statement SET MODE
      LEGACY

## Migrate to `1.18.6`

* `TranslationController#updateTranslation` now accepts a `TranslationDto` as first and only parameter instead of
  a `Translation`. If you override this method in your application it needs to be adapted accordingly.

## Migrate to `1.18.4`

Version `1.18.4` switches from 4-digit locales to 2-digit locales. Therefore, all translation files and mail translation
files
have to be renamed from `...translation-xx_XX.properties` to `...translation-xx.properties`. Same goes for all e-mail
templates, located under `src/main/resources/templates`. Also, all occurrences of `de_DE`, `en_US`, ... in config
files (`application-*.yaml`) need to be replaced by their language-only equivalent.

⚠️ Moreover, if you implemented a custom `DataInitializationConfiguration` in your application, make sure to
include `DataMigrationInitializer` in the return result of `getInitializers()`, otherwise the application won't start.

## Migrate to `1.18.3`

* Spring's `ErrorController` interface has changed,
  see [here](https://github.com/spring-projects/spring-boot/commit/1caca6e3d0eab2ab2af160fcf66cd9354f28323e#diff-7266013b572df782aaa79f326681788ca72ba9c2fd44820b8c7c5ee080b24da9).
  If your project overrides this class, you need to adapt it accordingly (i.e. remove `getErrorPath()` method).

## Migrate to `1.18.2`

* Constructor of `WebSecurityConfig` has changed. If your project overrides this class, you need to change it
  accordingly.
  See [here](https://git.frachtwerk.de/web-starter/backend/-/blob/8708f1e6bed1fe0fcab6be90556d7506fe034299/spring-starter-backend-lib/src/main/java/de/frachtwerk/starter/backend/configuration/WebSecurityConfig.java#L63).

## Migrate to `1.18.0`

For controller method access management the annotation `@RestrictToOwnedEntities` is now deprecated.
Use `@RestrictAccessToOwnedEntities` and `@OwnershipSpec` instead.

## Migrate to `1.17.2`

Email templates are now split in header, footer and main sections. Different languages of emails are now supported too.
Therefore, the freemarker templates and your application.yaml files need to be adapted.

See this [commit](https://git.frachtwerk.de/web-starter/backend/-/commit/9a1ec3d769d2a0cb9cc87bcef70cd4290c052e45).

## Migrate to `1.17.0`

One constructor of the User object expects a User object now instead of the UserDto. If you used this constructor in
your application you have to adapt it. Also, the
frontend [needs to be adapted](https://git.frachtwerk.de/web-starter/frontend/-/blob/master/MIGRATION.md).

## Migrate to `1.16.0`

No backend-side changes needed. However,
frontend [needs to be adapted](https://git.frachtwerk.de/web-starter/frontend/-/blob/master/MIGRATION.md).

## Migrate to `1.15.0`

## Mails

Version `1.15.0` switches from plain text mail to HTML mails. Therefore, the following files were removed:

- `resources/templates/ContactMessage.tmplt`
- `resources/templates/NewUserMessage.tmplt`
- `resources/templates/ResetToken.tmplt`

These files are replaced with:

- `resources/templates/ContactMessage.ftl`
- `resources/templates/NewUserMessage.ftl`
- `resources/templates/ResetTokenMessage.ftl`

When upgrading to `1.15.0` you need to **copy the new files in your corresponding `resources/templates/` folder**.

You can change the templates to fit your needs. Multiple parameters (like logo, colors or url of the service) can be
changed
using properties (e.g. with changing your profile or setting environment variables). You can even override the template
path using the corresponding properties, e.g. for the welcome mail for new users `mail.new-user-mail.template`.
Please make sure, that you use the corresponding properties before manually changing the mail templates.
