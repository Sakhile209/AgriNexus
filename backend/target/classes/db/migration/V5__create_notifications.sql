CREATE TABLE notification (
 id UUID PRIMARY KEY, user_id UUID NOT NULL, farm_id UUID,
 notification_type VARCHAR(40) NOT NULL, title VARCHAR(200) NOT NULL, message VARCHAR(1000) NOT NULL,
 due_on DATE, source_key VARCHAR(250) NOT NULL, related_path VARCHAR(500), is_read BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_notification_user FOREIGN KEY(user_id) REFERENCES app_user(id) ON DELETE RESTRICT,
 CONSTRAINT fk_notification_farm FOREIGN KEY(farm_id) REFERENCES farm(id) ON DELETE RESTRICT,
 CONSTRAINT uq_notification_user_source UNIQUE(user_id,source_key)
);
CREATE INDEX idx_notification_user_read_created ON notification(user_id,is_read,created_at DESC);

