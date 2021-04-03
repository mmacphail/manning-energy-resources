create table DEVICE (
    DEVICE_ID          varchar(36) not null,
    RECEIVED_TIMESTAMP timestamp   not null,
    REGION_ID          varchar(36) not null,
    CHARGING           integer     not null,
    PRIMARY KEY (DEVICE_ID)
);
