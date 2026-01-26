CREATE TABLE IF NOT EXISTS "FW_API_TOKEN"
(
    id          UUID         NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE,
    created_by  VARCHAR(255),
    updated_at  TIMESTAMP WITHOUT TIME ZONE,
    updated_by  VARCHAR(255),
    description VARCHAR(255),
    linked_user VARCHAR(255) NOT NULL,
    status      VARCHAR(48)  NOT NULL,
    valid_until date,
    CONSTRAINT FW_API_TOKEN_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS "FW_API_TOKEN_RIGHTS"
(
    api_token_id     UUID         NOT NULL,
    rights_authority VARCHAR(255) NOT NULL,
    CONSTRAINT FW_API_TOKEN_RIGHTS_pkey PRIMARY KEY (api_token_id, rights_authority)
);

ALTER TABLE IF EXISTS "FW_API_TOKEN"
    ADD CONSTRAINT FW_API_TOKEN_linked_user_description_uindex UNIQUE (linked_user, description);

ALTER TABLE IF EXISTS "FW_API_TOKEN_RIGHTS"
    ADD CONSTRAINT FW_API_TOKEN_RIGHTS_FW_RIGHT_authority_fk FOREIGN KEY (rights_authority) REFERENCES "FW_RIGHT" (authority) ON DELETE NO ACTION;

ALTER TABLE IF EXISTS "FW_API_TOKEN_RIGHTS"
    ADD CONSTRAINT FW_API_TOKEN_RIGHTS_FW_API_TOKEN_id_fk FOREIGN KEY (api_token_id) REFERENCES "FW_API_TOKEN" (id) ON DELETE NO ACTION;

