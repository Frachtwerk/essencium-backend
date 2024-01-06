-- Tables 'FW_USER', 'FW_ROLE', 'FW_RIGHT' and 'FW_ROLE_RIGHT' are created during migration V2.3.1 and don't have to be created again.

ALTER TABLE IF EXISTS "FW_ROLE"
    ADD COLUMN IF NOT EXISTS "is_default_role" BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE IF EXISTS "FW_ROLE"
    ADD COLUMN IF NOT EXISTS "is_system_role" BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS "FW_USER_ROLES"
(
    "user_id" bigint NOT NULL,
    "roles_name" character varying(255)  NOT NULL,
    CONSTRAINT "FW_USER_ROLES_pkey" PRIMARY KEY (user_id, roles_name),
    CONSTRAINT "FK5x6ca7enc8g15hxhty2s9iikp" FOREIGN KEY (roles_name)
        REFERENCES "FW_ROLE" (name) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT "FKh0k8otxier8qii1l14gvc87wi" FOREIGN KEY (user_id)
        REFERENCES "FW_USER" (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

INSERT INTO "FW_USER_ROLES" ("user_id", "roles_name") SELECT "id", "role_name" FROM "FW_USER";

ALTER TABLE "FW_USER" DROP COLUMN "role_name";
