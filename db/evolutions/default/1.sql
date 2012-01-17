# --- First database schema

# --- !Ups

-- set ignorecase true;

create table user (
  email                     varchar(255) not null primary key,
  name                      varchar(255) not null unique,
  password                  varchar(255) not null
);

create unique index on user(name);

create table team (
  id                        varchar(16) not null primary key,
  name                      varchar(255) not null,
  "group"					varchar(255) not null
);

create table match (
  id                        bigint not null primary key,
  teamA						varchar(16), -- teamId or null if team is not yet known
  teamAformula				varchar(16), -- the formula to compute teamA (eg. Win#29 means Winner match #29)
  teamB						varchar(16), -- teamId or null if team is not yet known
  teamBformula				varchar(16), -- the formula to compute teamB (eg. Win#29 means Winner match #29)
  kickoff					timestamp not null,
  phase						varchar(50) not null,
  -- result
  result					varchar(16), -- either 'DRAW', teamAId or TeamBId
  scoreA					integer,
  scoreB					integer,
  foreign key(teamA)		references team(id) on delete cascade,
  foreign key(teamB)		references team(id) on delete cascade,
);

create sequence match_seq start with 1;

# --- !Downs

drop table if exists match;
drop sequence if exists match_seq;
drop table if exists team;
drop table if exists user;
