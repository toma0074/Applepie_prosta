-- Apply once to the existing PROSTA Oracle schema before deploying this version.

ALTER TABLE customer ADD (
    consecutive_login_days NUMBER(10) DEFAULT 0 NOT NULL,
    last_seen_at TIMESTAMP
);

CREATE SEQUENCE seq_friend_connection
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 9999999999;

CREATE TABLE friend_connection (
    friend_connection_id NUMBER(10) NOT NULL,
    user_low_id          NUMBER(5) NOT NULL,
    user_high_id         NUMBER(5) NOT NULL,
    requested_by_id      NUMBER(5) NOT NULL,
    status               VARCHAR2(16 CHAR) NOT NULL,
    created_at           TIMESTAMP NOT NULL,
    updated_at           TIMESTAMP NOT NULL,
    CONSTRAINT pk_friend_connection PRIMARY KEY (friend_connection_id),
    CONSTRAINT uk_friend_connection_pair UNIQUE (user_low_id, user_high_id),
    CONSTRAINT ck_friend_connection_order CHECK (user_low_id < user_high_id),
    CONSTRAINT ck_friend_connection_status
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT fk_friend_connection_low
        FOREIGN KEY (user_low_id) REFERENCES customer (user_id),
    CONSTRAINT fk_friend_connection_high
        FOREIGN KEY (user_high_id) REFERENCES customer (user_id),
    CONSTRAINT fk_friend_connection_requester
        FOREIGN KEY (requested_by_id) REFERENCES customer (user_id)
);

CREATE INDEX ix_friend_connection_low_status
    ON friend_connection (user_low_id, status);
CREATE INDEX ix_friend_connection_high_status
    ON friend_connection (user_high_id, status);
