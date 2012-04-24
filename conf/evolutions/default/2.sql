# --- Initial dataset

# --- !Ups

insert into param (name, value) values ('currentDateTime', '08/06/2012 17:30'); -- used to simulate date for forecast input

insert into user (name, email, password, groups, enabled) values ('ozeebee', 'ozeebee@gmail.com', 'welcome1', 'Admin', TRUE);
insert into user (name, email, password, groups, enabled) values ('toto', 'toto@gmail.com', 'tototo', 'Admin', TRUE);

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

--insert into team (id,name) values ('WA', 'Winner Group A');
--insert into team (id,name) values ('SA', 'Second Group A');
--insert into team (id,name) values ('WB', 'Winner Group B');
--insert into team (id,name) values ('SB', 'Second Group B');
--insert into team (id,name) values ('WC', 'Winner Group C');
--insert into team (id,name) values ('SC', 'Second Group C');
--insert into team (id,name) values ('WD', 'Winner Group D');
--insert into team (id,name) values ('SD', 'Second Group D');
--
--insert into team (id,name) values ('WQ1', 'Winner Quarter Final 1');
--insert into team (id,name) values ('WQ2', 'Winner Quarter Final 2');
--insert into team (id,name) values ('WQ3', 'Winner Quarter Final 3');
--insert into team (id,name) values ('WQ4', 'Winner Quarter Final 4');
--
--insert into team (id,name) values ('WS1', 'Winner Semi Final 1');
--insert into team (id,name) values ('WS2', 'Winner Semi Final 2');

INSERT INTO PUBLIC.MATCH(ID, TEAMA, TEAMB, KICKOFF, PHASE, RESULT, SCOREA, SCOREB) VALUES
(01, 'POL', 'GRE', TIMESTAMP '2012-06-08 18:00:00.0', 'MD1', 'POL', 2, 1),
(02, 'RUS', 'CZE', TIMESTAMP '2012-06-08 20:45:00.0', 'MD1', 'DRAW', 1, 1),
(03, 'NED', 'DEN', TIMESTAMP '2012-06-09 18:00:00.0', 'MD1', 'DEN', 3, 4);

INSERT INTO PUBLIC.MATCH(ID, TEAMA, TEAMB, KICKOFF, PHASE) VALUES
(04, 'GER', 'POR', TIMESTAMP '2012-06-09 20:45:00.0', 'MD1'),
(05, 'ESP', 'ITA', TIMESTAMP '2012-06-10 18:00:00.0', 'MD1'),
(06, 'IRL', 'CRO', TIMESTAMP '2012-06-10 20:45:00.0', 'MD1'),
(07, 'FRA', 'ENG', TIMESTAMP '2012-06-11 18:00:00.0', 'MD1'),
(08, 'UKR', 'SWE', TIMESTAMP '2012-06-11 20:45:00.0', 'MD1'),

(09, 'GRE', 'CZE', TIMESTAMP '2012-06-12 18:00:00.0', 'MD2'),
(10, 'POL', 'RUS', TIMESTAMP '2012-06-12 20:45:00.0', 'MD2'),
(11, 'DEN', 'POR', TIMESTAMP '2012-06-13 18:00:00.0', 'MD2'),
(12, 'NED', 'GER', TIMESTAMP '2012-06-13 20:45:00.0', 'MD2'),
(13, 'ITA', 'CRO', TIMESTAMP '2012-06-14 18:00:00.0', 'MD2'),
(14, 'ESP', 'IRL', TIMESTAMP '2012-06-14 20:45:00.0', 'MD2'),
(15, 'SWE', 'ENG', TIMESTAMP '2012-06-15 20:45:00.0', 'MD2'),
(16, 'UKR', 'FRA', TIMESTAMP '2012-06-15 18:00:00.0', 'MD2'),

(17, 'CZE', 'POL', TIMESTAMP '2012-06-16 20:45:00.0', 'MD3'),
(18, 'GRE', 'RUS', TIMESTAMP '2012-06-16 20:45:00.0', 'MD3'),
(19, 'POR', 'NED', TIMESTAMP '2012-06-17 20:45:00.0', 'MD3'),
(20, 'DEN', 'GER', TIMESTAMP '2012-06-17 20:45:00.0', 'MD3'),
(21, 'CRO', 'ESP', TIMESTAMP '2012-06-18 20:45:00.0', 'MD3'),
(22, 'ITA', 'IRL', TIMESTAMP '2012-06-18 20:45:00.0', 'MD3'),
(23, 'ENG', 'UKR', TIMESTAMP '2012-06-19 20:45:00.0', 'MD3'),
(24, 'SWE', 'FRA', TIMESTAMP '2012-06-19 20:45:00.0', 'MD3');

INSERT INTO PUBLIC.MATCH(ID, TEAMAFORMULA, TEAMBFORMULA, KICKOFF, PHASE) VALUES
(25, 'WIN_GROUP_A' , 'SEC_GROUP_B' , TIMESTAMP '2012-06-21 20:45:00.0', 'QUARTERFINALS'),
(26, 'WIN_GROUP_B' , 'SEC_GROUP_A' , TIMESTAMP '2012-06-22 20:45:00.0', 'QUARTERFINALS'),
(27, 'WIN_GROUP_C' , 'SEC_GROUP_D' , TIMESTAMP '2012-06-23 20:45:00.0', 'QUARTERFINALS'),
(28, 'WIN_GROUP_D' , 'SEC_GROUP_C' , TIMESTAMP '2012-06-24 20:45:00.0', 'QUARTERFINALS'),

(29, 'WIN_25' , 'WIN_27' , TIMESTAMP '2012-06-27 20:45:00.0', 'SEMIFINALS'),
(30, 'WIN_26' , 'WIN_28' , TIMESTAMP '2012-06-28 20:45:00.0', 'SEMIFINALS'),

(31, 'WIN_29' , 'WIN_30' , TIMESTAMP '2012-07-01 20:45:00.0', 'FINAL');

# --- !Downs

delete from match;
delete from team;
