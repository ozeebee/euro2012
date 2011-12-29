# --- First database schema

# --- !Ups

-- set ignorecase true;

create table user (
  email                     varchar(255) not null primary key,
  name                      varchar(255) not null,
  password                  varchar(255) not null
);

create table team (
  id                        varchar(16) not null primary key,
  name                      varchar(255) not null,
  "group"					varchar(255) not null
);

create table match (
  id                        bigint not null primary key,
  teamA						varchar(16) not null,
  teamB						varchar(16) not null,
  kickoff					timestamp not null,
  phase						varchar(50) not null,
  -- result
  result					varchar(16), -- either 'DRAW', teamAId or TeamBId
  scoreA					integer,
  scoreB					integer,
  foreign key(teamA)		references team(id) on delete cascade,
  foreign key(teamB)		references team(id) on delete cascade,
);

create sequence match_seq start with 1000;

# --- !Downs

drop table if exists match;
drop sequence if exists match_seq;
drop table if exists team;
drop table if exists user;
