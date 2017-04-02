insert  into resolution(ID,SEQUENCE,pname,DESCRIPTION,ICONURL) values ('1','1','Fixed','A fix for this issue is checked into the tree and tested.',NULL);
insert  into resolution(ID,SEQUENCE,pname,DESCRIPTION,ICONURL) values ('10100','6','Done','',NULL);
insert  into resolution(ID,SEQUENCE,pname,DESCRIPTION,ICONURL) values ('2','2','Won''t Fix','The problem described is an issue which will never be fixed.',NULL);
insert  into resolution(ID,SEQUENCE,pname,DESCRIPTION,ICONURL) values ('3','3','Duplicate','The problem is a duplicate of an existing issue.',NULL);
insert  into resolution(ID,SEQUENCE,pname,DESCRIPTION,ICONURL) values ('4','4','Incomplete','The problem is not completely described.',NULL);
insert  into resolution(ID,SEQUENCE,pname,DESCRIPTION,ICONURL) values ('5','5','Cannot Reproduce','All attempts at reproducing this issue failed, or not enough information was available to reproduce the issue. Reading the code produces no clues as to why this behavior would occur. If more information appears later, please reopen the issue.',NULL);

insert into project_key (ID, PROJECT_ID, PROJECT_KEY) values('1','10074','MDA');
insert into project (ID, pname, URL, LEAD, DESCRIPTION, pkey, pcounter, ASSIGNEETYPE, AVATAR) values('10074','MDA','','fdaugan','Solution MDA pour GFI Groupe. Ce produit est une suite de plugins Eclipse destinée à mettre en œuvre la démarche MDA dans les projets forfait, TMA et migration technologique. La génération de code (M2T)  inclus les tests, la transformation de modèle (M2M) inclus l expérience de modèles dynamiques et exécutables, la génération de documentation (M2D) permet d obtenir des sorties au format  Word.','MDA','1299','2','10520');
insert into project (ID, pname, URL, LEAD, DESCRIPTION, pkey, pcounter, ASSIGNEETYPE, AVATAR) values('10000','gStack','','fdaugan','Pile Web','GSTACK','1299','2','10520');

insert  into priority(ID,SEQUENCE,pname,DESCRIPTION,ICONURL,STATUS_COLOR) values ('1','1','Blocker','Blocks development and/or testing work, production could not run.','/images/icons/priority_blocker.gif','#cc0000');
insert  into priority(ID,SEQUENCE,pname,DESCRIPTION,ICONURL,STATUS_COLOR) values ('2','2','Critical','Crashes, loss of data, severe memory leak.','/images/icons/priority_critical.gif','#ff0000');
insert  into priority(ID,SEQUENCE,pname,DESCRIPTION,ICONURL,STATUS_COLOR) values ('3','3','Major','Major loss of function.','/images/icons/priority_major.gif','#009900');
insert  into priority(ID,SEQUENCE,pname,DESCRIPTION,ICONURL,STATUS_COLOR) values ('4','4','Minor','Minor loss of function, or other problem where easy workaround is present.','/images/icons/priority_minor.gif','#006600');
insert  into priority(ID,SEQUENCE,pname,DESCRIPTION,ICONURL,STATUS_COLOR) values ('5','5','Trivial','Cosmetic problem like misspelt words or misaligned text.','/images/icons/priority_trivial.gif','#003300');

insert into issuetype (ID, SEQUENCE, pname, pstyle, DESCRIPTION, ICONURL) values('1','1','Bug',NULL,'A problem which impairs or prevents the functions of the product.','/images/icons/bug.gif');
insert into issuetype (ID, SEQUENCE, pname, pstyle, DESCRIPTION, ICONURL) values('2','2','New Feature',NULL,'A new feature of the product, which has yet to be developed.','/images/icons/newfeature.gif');
insert into issuetype (ID, SEQUENCE, pname, pstyle, DESCRIPTION, ICONURL) values('3','3','Task',NULL,'A task that needs to be done.','/images/icons/task.gif');
insert into issuetype (ID, SEQUENCE, pname, pstyle, DESCRIPTION, ICONURL) values('4','4','Improvement',NULL,'An improvement or enhancement to an existing feature or task.','/images/icons/improvement.gif');
insert into issuetype (ID, SEQUENCE, pname, pstyle, DESCRIPTION, ICONURL) values('5',NULL,'DFE',NULL,'GIESV_DFE','/images/icons/requirement.gif');
insert into issuetype (ID, SEQUENCE, pname, pstyle, DESCRIPTION, ICONURL) values('6',NULL,'Question',NULL,'Question sur le fonctionnement Applicatif','/images/icons/undefined.gif');
insert into issuetype (ID, SEQUENCE, pname, pstyle, DESCRIPTION, ICONURL) values('7',NULL,'Action',NULL,'Action à faire','/images/icons/exclamation.gif');
insert into issuetype (ID, SEQUENCE, pname, pstyle, DESCRIPTION, ICONURL) values('8','0','Sub-task','jira_subtask','The sub-task of the issue','/images/icons/issue_subtask.gif');

insert into issuestatus (ID, SEQUENCE, pname, DESCRIPTION, ICONURL) values('1','1','Open','The issue is open and ready for the assignee to start work on it.','/images/icons/status_open.gif');
insert into issuestatus (ID, SEQUENCE, pname, DESCRIPTION, ICONURL) values('10024','31','Assigned','Demande assignée','/images/icons/status_assigned.gif');
insert into issuestatus (ID, SEQUENCE, pname, DESCRIPTION, ICONURL) values('3','3','In Progress','This issue is being actively worked on at the moment by the assignee.','/images/icons/status_inprogress.gif');
insert into issuestatus (ID, SEQUENCE, pname, DESCRIPTION, ICONURL) values('4','4','Reopened','This issue was once resolved, but the resolution was deemed incorrect. From here issues are either marked assigned or resolved.','/images/icons/status_reopened.gif');
insert into issuestatus (ID, SEQUENCE, pname, DESCRIPTION, ICONURL) values('5','5','Resolved','A resolution has been taken, and it is awaiting verification by reporter. From here issues are either reopened, or are closed.','/images/icons/status_resolved.gif');
insert into issuestatus (ID, SEQUENCE, pname, DESCRIPTION, ICONURL) values('6','6','Closed','The issue is considered finished, the resolution is correct. Issues which are closed can be reopened.','/images/icons/status_closed.gif');

insert into nodeassociation (SOURCE_NODE_ID, SOURCE_NODE_ENTITY, SINK_NODE_ID, SINK_NODE_ENTITY, ASSOCIATION_TYPE, SEQUENCE) values('10074','Project','10022','IssueTypeScreenScheme','ProjectScheme',NULL);

insert  into optionconfiguration(ID,FIELDID,OPTIONID,FIELDCONFIG,SEQUENCE) values ('19015','issuetype','1','10065','0');
insert  into optionconfiguration(ID,FIELDID,OPTIONID,FIELDCONFIG,SEQUENCE) values ('19016','issuetype','2','10065','1');
insert  into optionconfiguration(ID,FIELDID,OPTIONID,FIELDCONFIG,SEQUENCE) values ('19017','issuetype','6','10065','2');
insert  into optionconfiguration(ID,FIELDID,OPTIONID,FIELDCONFIG,SEQUENCE) values ('19018','issuetype','8','10065','3');
insert  into optionconfiguration(ID,FIELDID,OPTIONID,FIELDCONFIG,SEQUENCE) values ('19019','issuetype','3','10065','4');
insert  into configurationcontext(ID,PROJECTCATEGORY,PROJECT,customfield,FIELDCONFIGSCHEME) values ('17134',NULL,'10074','issuetype','10065');
insert  into configurationcontext(ID,PROJECTCATEGORY,PROJECT,customfield,FIELDCONFIGSCHEME) values ('17135',NULL,'10000','issuetype','10065');

insert  into cwd_user(ID,directory_id,user_name,lower_user_name,active,created_date,updated_date,first_name,lower_first_name,last_name,lower_last_name,display_name,lower_display_name,email_address,lower_email_address,CREDENTIAL,deleted_externally,EXTERNAL_ID) values ('10110','10000','fdaugan','fdaugan','1','2013-07-24 14:08:00','2015-09-28 12:06:22','Fabrice','fabrice','Daugan','daugan','Fabrice Daugan','fabrice daugan','fabrice.daugan@gfi.fr','fabrice.daugan@gfi.fr','nopass',NULL,'0da8fd74-b7ed-1032-8eba-f53fba2dfb3d');
insert  into cwd_user(ID,directory_id,user_name,lower_user_name,active,created_date,updated_date,first_name,lower_first_name,last_name,lower_last_name,display_name,lower_display_name,email_address,lower_email_address,CREDENTIAL,deleted_externally,EXTERNAL_ID) values ('10211','10000','alocquet','alocquet','1','2013-09-18 22:22:06','2015-09-28 12:06:22','Arnaud','arnaud','Locquet','locquet','Arnaud Locquet','arnaud locquet','arnaud.locquet@gfi.fr','arnaud.locquet@gfi.fr','nopass',NULL,'0dad73fe-b7ed-1032-8ebe-f53fba2dfb3d');
insert  into cwd_user(ID,directory_id,user_name,lower_user_name,active,created_date,updated_date,first_name,lower_first_name,last_name,lower_last_name,display_name,lower_display_name,email_address,lower_email_address,CREDENTIAL,deleted_externally,EXTERNAL_ID) values ('10213','10000','mmasztalir','mmasztalir','1','2013-09-19 10:22:06','2014-02-11 21:13:12','Matthieu','matthieu','Masztalir','masztalir','Matthieu Masztalir','matthieu masztalir','matthieu.masztalir@gfi.fr','matthieu.masztalir@gfi.fr','nopass',NULL,'0daeb174-b7ed-1032-8ebf-f53fba2dfb3d');

insert  into cwd_user_attributes(ID,user_id,directory_id,attribute_name,attribute_value,lower_attribute_value) values ('211','10110','10000','lastAuthenticated','1443434782321','1443434782321');
insert  into cwd_user_attributes(ID,user_id,directory_id,attribute_name,attribute_value,lower_attribute_value) values ('212','10110','10000','login.currentFailedCount','1','1');

