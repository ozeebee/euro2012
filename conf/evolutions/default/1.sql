# --- First database schema

# --- !Ups

-- set ignorecase true;

create table param (
	name					varchar(255) not null primary key,
	value					varchar(1024)
);

create table user (
  name                      varchar(255) not null primary key,
  email                     varchar(255) not null unique,
  password                  varchar(255) not null,
  groups					varchar(255) -- comma separated list of groups (ex: 'Admin')
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
  penaltyScoreA				integer, -- optional penalty score for teamA
  penaltyScoreB				integer, -- optional penalty score for teamB
  foreign key(teamA)		references team(id) on delete cascade,
  foreign key(teamB)		references team(id) on delete cascade,
);

create sequence match_seq start with 1;

create table forecast (
  username					varchar(255) not null,
  matchid					bigint not null,
  scoreA					integer not null,
  scoreB					integer not null,
  modifDate					timestamp not null default current_timestamp(),
  constraint pk_forecast	primary key(username, matchid),	
  foreign key(username)		references user(name) on delete cascade,
  foreign key(matchid)		references match(id) on delete cascade
);

# --- !Downs

drop table if exists forecast;
drop table if exists match;
drop sequence if exists match_seq;
drop table if exists team;
drop table if exists user;
drop table if exists param;
