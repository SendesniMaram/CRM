-- Apply explicitly after review; application SQL initialization remains disabled.
-- Independent identity reference: no cross-database foreign key, no username/email matching.
CREATE TABLE IF NOT EXISTS customer_user_associations (
 user_id BIGINT NOT NULL,
 customer_id BIGINT NOT NULL,
 PRIMARY KEY (user_id),
 CONSTRAINT uk_customer_association_customer UNIQUE (customer_id),
 CONSTRAINT fk_customer_association_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
 CONSTRAINT ck_customer_association_user CHECK (user_id > 0)
);
