-- Mails and files a player attached to a message. Only references are stored; the content lives in the case files.
CREATE TABLE chat_message_attachment
(
    message_id UUID         NOT NULL REFERENCES chat_message (id) ON DELETE CASCADE,
    position   INTEGER      NOT NULL,
    desktop_id VARCHAR(100) NOT NULL,
    app_id     VARCHAR(100) NOT NULL,
    item_id    VARCHAR(100) NOT NULL,
    PRIMARY KEY (message_id, position)
);
