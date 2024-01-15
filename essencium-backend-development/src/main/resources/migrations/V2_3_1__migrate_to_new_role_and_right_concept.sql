-- Copyright (C) 2023 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
--
-- This file is part of essencium-backend.
--
-- essencium-backend is free software: you can redistribute it and/or modify
-- it under the terms of the GNU Lesser General Public License as published by
-- the Free Software Foundation, either version 3 of the License, or
-- (at your option) any later version.
--
-- essencium-backend is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
-- GNU Lesser General Public License for more details.
--
-- You should have received a copy of the GNU Lesser General Public License
-- along with essencium-backend. If not, see <http://www.gnu.org/licenses/>.

-- create potentially non-existing tables
CREATE TABLE IF NOT EXISTS "FW_USER"
(
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    created_by character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    email character varying(150),
    enabled boolean NOT NULL,
    failed_login_attempts integer NOT NULL DEFAULT 0,
    first_name character varying(255),
    last_name character varying(255),
    locale character varying(255) NOT NULL,
    login_disabled boolean NOT NULL,
    mobile character varying(255),
    nonce character varying(255),
    password character varying(255),
    password_reset_token character varying(255),
    phone character varying(255),
    source character varying(255),
    role_id bigint NOT NULL,
    CONSTRAINT "FW_USER_pkey" PRIMARY KEY (id),
    CONSTRAINT uk_o5gwjnjfosht4tf5lq48rxfoj UNIQUE (email)
);
CREATE TABLE IF NOT EXISTS "FW_RIGHT"
(
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    created_by character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    description character varying(512),
    name character varying(255) NOT NULL,
    CONSTRAINT "FW_RIGHT_pkey" PRIMARY KEY (id),
    CONSTRAINT uk_jep1itavphekmnphj0vp38s9u UNIQUE (name)
);
CREATE TABLE IF NOT EXISTS "FW_ROLE"
(
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    created_by character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    description character varying(255),
    is_protected boolean NOT NULL,
    name character varying(255) NOT NULL,
    CONSTRAINT "FW_ROLE_pkey" PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS "FW_ROLE_RIGHTS"
(
    role_id bigint NOT NULL,
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