insert into jiraissue (ID, pkey, PROJECT, REPORTER, ASSIGNEE, issuetype, SUMMARY, DESCRIPTION, ENVIRONMENT, PRIORITY, RESOLUTION, issuestatus, CREATED, UPDATED, DUEDATE, VOTES, TIMEORIGINALESTIMATE, TIMEESTIMATE, TIMESPENT, WORKFLOW_ID, SECURITY, FIXFOR, COMPONENT, RESOLUTIONDATE, WATCHES,issuenum) values('12706','MDA-41','10074','xsintive','fdaugan','2','Gestion des vues dans la doc technique','Lors de la transformation du modèle en document Word, serait-il possible de  : \n\nCréer une section Vues dans le document Word.\n\nCette section regroupera tous les objets possédant, entre autres, le stéréotype \"view\" défini dans le PIM. Ces objets ne devront plus être présent dans la section regroupant les \"do\" (alors qu ils peuvent en posséder le stéréotype).\nCes objets \"view\" auront des liens d utilisations (\"use\") vers d autres objets du modèle (éventuellement stéréotypés \"view\") afin de préciser sur quels objets repose leur construction. \nIl se peut que les objets référencés n aient pas de stéréotypes.\n\nL ajout de stéréotype \"view\" ne changera en rien la génération Java du modèle.\n\n',NULL,'4',NULL,'1','2009-07-09 08:45:33','2009-12-09 20:26:49',NULL,'0',NULL,NULL,NULL,'20258',NULL,NULL,NULL,NULL,'0',41);
insert into jiraissue (ID, pkey, PROJECT, REPORTER, ASSIGNEE, issuetype, SUMMARY, DESCRIPTION, ENVIRONMENT, PRIORITY, RESOLUTION, issuestatus, CREATED, UPDATED, DUEDATE, VOTES, TIMEORIGINALESTIMATE, TIMEESTIMATE, TIMESPENT, WORKFLOW_ID, SECURITY, FIXFOR, COMPONENT, RESOLUTIONDATE, WATCHES) values('15114','MDA-207','10074','fdaugan','fdaugan','2','Outil de complétion des diagrammes de séquence par des référence de diagramme','Afin de faciliter la navigation dans les diagrammes de séquence, compléter sur demande les diagrammes de séquence existant par des références.\nAinsi par un double clic, il sera possible d accéder directement au diagramme de séquence de l opération appelé, et ainsi pouvoir passer en revue toute la chaîne d appel.',NULL,'4',NULL,'1','2009-12-10 16:40:23','2009-12-18 17:03:24',NULL,'0',NULL,NULL,NULL,'20282',NULL,NULL,NULL,NULL,'0');
insert into jiraissue (ID, pkey, PROJECT, REPORTER, ASSIGNEE, issuetype, SUMMARY, DESCRIPTION, ENVIRONMENT, PRIORITY, RESOLUTION, issuestatus, CREATED, UPDATED, DUEDATE, VOTES, TIMEORIGINALESTIMATE, TIMEESTIMATE, TIMESPENT, WORKFLOW_ID, SECURITY, FIXFOR, COMPONENT, RESOLUTIONDATE, WATCHES) values('11432','MDA-1','10074','fdaugan','fdaugan','1','Mise à disposition d un update site pour RSM 7.0.0.4','Demande de mise à disposition d un update site disponible depuis Help -> Updates -> Install new Feature',NULL,'3','1','6','2009-03-23 15:26:43','2011-09-22 09:25:47',NULL,'0','10951','11951','12951','18951','10020',NULL,NULL,'2011-08-04 22:37:06','0');
insert into jiraissue (ID, pkey, PROJECT, REPORTER, ASSIGNEE, issuetype, SUMMARY, DESCRIPTION, ENVIRONMENT, PRIORITY, RESOLUTION, issuestatus, CREATED, UPDATED, DUEDATE, VOTES, TIMEORIGINALESTIMATE, TIMEESTIMATE, TIMESPENT, WORKFLOW_ID, SECURITY, FIXFOR, COMPONENT, RESOLUTIONDATE, WATCHES) values('11437','MDA-4','10074','xsintive','fdaugan','1','Génération hbm.xml : type des clés composite n-x','En fait la version actuelle est la 3.1.13\n\nDans Jour.hbm\n\n<set name=\"periodeScolaire\" table=\"RF_PERIODESCOLAIRE_JOUR_JOUR\">\n\n                  <key>\n\n                        <column name=\"JOUR_DATE_CALENDRIER\"/>\n\n                  </key>\n\n                  <many-to-many class=\"fr.stime.model.PeriodeScolaire\">\n\n                        <column name=\"PERIODESCOLAIRE\" sql-type=\"integer\" />\n\n                  </many-to-many>\n\n            </set>\n\n            <set name=\"coefficientsCorrecteurs\" table=\"RF_COEFFICIENTSCORRECTEURS_FE1\">\n\n                  <key>\n\n                        <column name=\"JOUR_DATE_CALENDRIER\"/>\n\n                  </key>\n\n                  <many-to-many class=\"fr.stime.model.CoefficientCorrecteur\">\n\n                        <column name=\"COEFFICIENTSCORRECTEURS\" sql-type=\"integer\" />\n\n                  </many-to-many>\n\n            </set>\n\n            <set name=\"exForEtablissements\" table=\"RF_JOUR_ETABLISSEMENT_EX_F_784\">\n\n                  <key>\n\n                        <column name=\"JOUR_DATE_CALENDRIER\"/>\n\n                  </key>\n\n                  <many-to-many unique=\"true\" class=\"fr.stime.model.Etablissement\">\n\n                        <column name=\"ETABLISSEMENT_CODE\" sql-type=\"VARCHAR(3)\" />\n\n                  </many-to-many>\n\n            </set>\n\nSeul le 1er et 3ème  <set> sont incorrects : \n\ncreate table RF_PERIODESCOLAIRE_JOUR_JOUR (JOUR_DATE_CALENDRIER TIMESTAMP not null, PERIODESCOLAIRE integer not null, primary key (PERIODESCOLAIRE, JOUR_DATE_CALENDRIER));\n\ncreate table RF_COEFFICIENTSCORRECTEURS_FE1 (COEFFICIENTSCORRECTEURS integer not null, JOUR_DATE_CALENDRIER DATE not null, primary key (JOUR_DATE_CALENDRIER, COEFFICIENTSCORRECTEURS));\n\ncreate table RF_JOUR_ETABLISSEMENT_EX_F_784 (JOUR_DATE_CALENDRIER TIMESTAMP not null, ETABLISSEMENT_CODE VARCHAR(3) not null, primary key (JOUR_DATE_CALENDRIER, ETABLISSEMENT_CODE));\n\nalors que JOUR_DATE_CALENDRIER devrait être de type DATE;\n',NULL,'3','1','6','2009-03-23 16:23:31','2010-02-04 10:18:24',NULL,'0',NULL,NULL,NULL,'19001',NULL,NULL,NULL,'2009-09-09 11:00:23','0');
insert into jiraissue (ID, pkey, PROJECT, REPORTER, ASSIGNEE, issuetype, SUMMARY, DESCRIPTION, ENVIRONMENT, PRIORITY, RESOLUTION, issuestatus, CREATED, UPDATED, DUEDATE, VOTES, TIMEORIGINALESTIMATE, TIMEESTIMATE, TIMESPENT, WORKFLOW_ID, SECURITY, FIXFOR, COMPONENT, RESOLUTIONDATE, WATCHES) values('11439','MDA-5','10074','egrosse','fdaugan','2','Création du stéréotype \"Label\"','Le stéréotype \"Label\" n existe pas et ne peut donc pas être rajouté à une \"Page\"\n  -> obligation pour le moment d utiliser le stéréotype \"Input\" en mode read only.',NULL,'4','1','6','2009-03-24 10:52:59','2009-09-09 10:58:47',NULL,'0',NULL,NULL,NULL,'20295',NULL,NULL,NULL,'2009-09-09 10:58:47','0');
insert into jiraissue (ID, pkey, PROJECT, REPORTER, ASSIGNEE, issuetype, SUMMARY, DESCRIPTION, ENVIRONMENT, PRIORITY, RESOLUTION, issuestatus, CREATED, UPDATED, DUEDATE, VOTES, TIMEORIGINALESTIMATE, TIMEESTIMATE, TIMESPENT, WORKFLOW_ID, SECURITY, FIXFOR, COMPONENT, RESOLUTIONDATE, WATCHES) values('11472','MDA-6','10074','xsintive','fdaugan','1','Libellé Plugin RSM','Dans RSM, lorsqu on applique un stéréotype \"sqlData\" à un attribut, dans le menu \"advanced\" on renseigne les propriétés sous le libellé \"Business\" => non cohérent',NULL,'4','1','6','2009-03-27 10:44:12','2010-02-04 10:22:57',NULL,'0',NULL,NULL,NULL,'19003',NULL,NULL,NULL,'2009-07-17 16:53:50','0');
insert into jiraissue (ID, pkey, PROJECT, REPORTER, ASSIGNEE, issuetype, SUMMARY, DESCRIPTION, ENVIRONMENT, PRIORITY, RESOLUTION, issuestatus, CREATED, UPDATED, DUEDATE, VOTES, TIMEORIGINALESTIMATE, TIMEESTIMATE, TIMESPENT, WORKFLOW_ID, SECURITY, FIXFOR, COMPONENT, RESOLUTIONDATE, WATCHES) values('11476','MDA-7','10074','xsintive','fdaugan','1','Génération hbm.xml','Complément du précédent (on peut modifier les précédentes demandes ?)\n\nLe cas se produit également pour la table de jointure : \ncreate table RF_ETABLISSEMENT_ENTREPOT__2CB (ENTREPOT_CODE varchar2(255 char) not null, ETABLISSEMENT_CODE VARCHAR(3) not null, primary key (ETABLISSEMENT_CODE, ENTREPOT_CODE));\n\noù le ENTREPOT_CODE devrait être un varchar(3) au lieu d être un varchar2(255)\n\n\nAinsi que pour le discriminant (stratégie d héritage) : \n\ncreate table PR_ASEUIL (ID integer not null, DISCR varchar2(255 char) not null, SOUSACTIVITE_CODE VARCHAR(13), ENTREPOT_CODE VARCHAR(3), ETABLISSEMENT_CODE VARCHAR(3), primary key (ID));\n',NULL,'3','1','6','2009-03-27 15:30:10','2010-02-04 10:18:24',NULL,'0',NULL,NULL,NULL,'19005',NULL,NULL,NULL,'2009-07-17 16:54:48','0');
insert into jiraissue (ID, pkey, PROJECT, REPORTER, ASSIGNEE, issuetype, SUMMARY, DESCRIPTION, ENVIRONMENT, PRIORITY, RESOLUTION, issuestatus, CREATED, UPDATED, DUEDATE, VOTES, TIMEORIGINALESTIMATE, TIMEESTIMATE, TIMESPENT, WORKFLOW_ID, SECURITY, FIXFOR, COMPONENT, RESOLUTIONDATE, WATCHES) values('11535','MDA-8','10074','challer','fdaugan','2','Création d un wizard VO/DO to CRUD','Dans le cadre du projet phoenix, il est nécessaire de créer un wizard permettant à partir d un VO ou d un DO de créer l ensemble des éléments UML impliqués dans un CRUD (action, page, macroactivity, callAction, callPage, service, diagramme de séquences).',NULL,'3','13','6','2009-04-01 14:20:29','2009-09-03 11:50:32',NULL,'0',NULL,NULL,NULL,'20296',NULL,NULL,NULL,'2009-09-03 11:50:30','0');
insert into jiraissue (ID, pkey, PROJECT, REPORTER, ASSIGNEE, issuetype, SUMMARY, DESCRIPTION, ENVIRONMENT, PRIORITY, RESOLUTION, issuestatus, CREATED, UPDATED, DUEDATE, VOTES, TIMEORIGINALESTIMATE, TIMEESTIMATE, TIMESPENT, WORKFLOW_ID, SECURITY, FIXFOR, COMPONENT, RESOLUTIONDATE, WATCHES) values('11538','MDA-11','10074','challer','fdaugan','2','Validateur de modèle','Il faudrait intégrer dans RSM un validateur de modèle, sur un élément uml, et sur l ensemble du projet. Les informations (warning et errors) seraient affichées au travers de la vue \"Problems\".\n\nCe validateur serait accessible via le menu contextuel MDA',NULL,'3','1','6','2009-04-01 14:39:59','2009-10-23 15:41:07',NULL,'0',NULL,NULL,NULL,'20297',NULL,NULL,NULL,'2009-10-23 15:41:03','0');
insert into jiraissue (ID, pkey, PROJECT, REPORTER, ASSIGNEE, issuetype, SUMMARY, DESCRIPTION, ENVIRONMENT, PRIORITY, RESOLUTION, issuestatus, CREATED, UPDATED, DUEDATE, VOTES, TIMEORIGINALESTIMATE, TIMEESTIMATE, TIMESPENT, WORKFLOW_ID, SECURITY, FIXFOR, COMPONENT, RESOLUTIONDATE, WATCHES) values('11542','MDA-14','10074','challer','fdaugan','2','Création d un wizard d ajout d un lien vers une maquette','Il serait intéressant d avoir un wizard permettant d ajouter sur un élément \"page\" un lien vers une maquette. Plutôt que de devoir créer un \"url\" à la main, le wizard prendrait en charge cette tâche tout en stéréotypant l\"url\" par \"maq\" ou \"screen\"....',NULL,'4','12','6','2009-04-01 15:08:35','2010-03-31 13:57:59',NULL,'0',NULL,NULL,NULL,'20255',NULL,NULL,NULL,'2010-03-08 12:44:28','0');