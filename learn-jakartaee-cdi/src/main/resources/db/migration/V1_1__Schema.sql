create table post (
    id bigint generated by default as identity(start with 100),
    publish_date date not null,
    message varchar(50) not null,
    primary key (id)
);