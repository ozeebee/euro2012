# --- Initial dataset

# --- !Ups

insert into team (id,name,"group") values ('POL', 'Poland', 'A');
insert into team (id,name,"group") values ('GRE', 'Greece', 'A');
insert into team (id,name,"group") values ('RUS', 'Russia', 'A');
insert into team (id,name,"group") values ('CZE', 'Czech Republic', 'A');

insert into team (id,name,"group") values ('NED', 'Netherlands', 'B');
insert into team (id,name,"group") values ('DEN', 'Denmark', 'B');
insert into team (id,name,"group") values ('GER', 'Germany', 'B');
insert into team (id,name,"group") values ('POR', 'Portugal', 'B');

insert into team (id,name,"group") values ('ESP', 'Spain', 'C');
insert into team (id,name,"group") values ('ITA', 'Italy', 'C');
insert into team (id,name,"group") values ('IRL', 'Republic of Ireland', 'C');
insert into team (id,name,"group") values ('CRO', 'Croatia', 'C');

insert into team (id,name,"group") values ('UKR', 'Ukrain', 'D');
insert into team (id,name,"group") values ('SWE', 'Sweden', 'D');
insert into team (id,name,"group") values ('FRA', 'France', 'D');
insert into team (id,name,"group") values ('ENG', 'England', 'D');

--insert into match (id,teamA,teamB,kickoff,phase) values ( 1, 'POL', 'GRE', '2011-12-31 18:00:00', 'MD1');
INSERT INTO PUBLIC.MATCH(ID, TEAMA, TEAMB, KICKOFF, PHASE, RESULT, SCOREA, SCOREB) VALUES
(1000, 'POL', 'GRE', TIMESTAMP '2012-06-08 18:00:00.0', 'MD1', 'POL', 2, 1),
(1001, 'RUS', 'CZE', TIMESTAMP '2012-06-08 20:45:00.0', 'MD1', 'DRAW', 1, 1),
(1002, 'NED', 'DEN', TIMESTAMP '2012-06-09 18:00:00.0', 'MD1', 'DEN', 3, 4);

INSERT INTO PUBLIC.MATCH(ID, TEAMA, TEAMB, KICKOFF, PHASE) VALUES
(1003, 'GER', 'POR', TIMESTAMP '2012-06-09 20:45:00.0', 'MD1'),
(1004, 'ESP', 'ITA', TIMESTAMP '2012-06-10 18:00:00.0', 'MD1'),
(1005, 'IRL', 'CRO', TIMESTAMP '2012-06-10 20:45:00.0', 'MD1'),
(1006, 'FRA', 'ENG', TIMESTAMP '2012-06-11 18:00:00.0', 'MD1'),
(1007, 'UKR', 'SWE', TIMESTAMP '2012-06-11 20:45:00.0', 'MD1'),

(1008, 'GRE', 'CZE', TIMESTAMP '2012-06-12 18:00:00.0', 'MD2'),
(1009, 'POL', 'RUS', TIMESTAMP '2012-06-12 20:45:00.0', 'MD2'),
(1010, 'DEN', 'POR', TIMESTAMP '2012-06-13 18:00:00.0', 'MD2'),
(1011, 'NED', 'GER', TIMESTAMP '2012-06-13 20:45:00.0', 'MD2'),
(1012, 'ITA', 'CRO', TIMESTAMP '2012-06-14 18:00:00.0', 'MD2'),
(1013, 'ESP', 'IRL', TIMESTAMP '2012-06-14 20:45:00.0', 'MD2'),
(1014, 'UKR', 'FRA', TIMESTAMP '2012-06-15 18:00:00.0', 'MD2'),
(1015, 'SWE', 'ENG', TIMESTAMP '2012-06-15 20:45:00.0', 'MD2'),

(1016, 'GRE', 'RUS', TIMESTAMP '2012-06-16 20:45:00.0', 'MD3'),
(1017, 'CZE', 'POL', TIMESTAMP '2012-06-16 20:45:00.0', 'MD3'),
(1018, 'POR', 'NED', TIMESTAMP '2012-06-17 20:45:00.0', 'MD3'),
(1019, 'DEN', 'GER', TIMESTAMP '2012-06-17 20:45:00.0', 'MD3'),
(1020, 'CRO', 'ESP', TIMESTAMP '2012-06-18 20:45:00.0', 'MD3'),
(1021, 'ITA', 'IRL', TIMESTAMP '2012-06-18 20:45:00.0', 'MD3'),
(1022, 'SWE', 'FRA', TIMESTAMP '2012-06-19 20:45:00.0', 'MD3'),
(1023, 'ENG', 'UKR', TIMESTAMP '2012-06-19 20:45:00.0', 'MD3');


# --- !Downs

delete from match;
delete from team;
