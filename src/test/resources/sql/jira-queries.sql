SELECT * FROM `project` WHERE ID = 10074;
SELECT * FROM `jiraissue` WHERE PROJECT = 10074 AND ID < 15426;
SELECT * FROM `issuetype`
SELECT * FROM `issuestatus`
SELECT * FROM `changegroup` AS cg WHERE cg.issueid IN (SELECT ID FROM `jiraissue` WHERE PROJECT = 10074 AND ID < 15426);
SELECT * FROM `changeitem` WHERE groupid IN (SELECT ID FROM `changegroup` AS cg WHERE cg.issueid IN (SELECT ID FROM `jiraissue` WHERE PROJECT = 10074 AND ID < 15426));
SELECT * FROM `jiraworkflows` WHERE workflowname LIKE "%CSN%"
SELECT * FROM `workflowscheme` WHERE `NAME` LIKE "%CSN%"
SELECT * FROM `workflowschemeentity` WHERE `WORKFLOW` LIKE "%CSN%"
SELECT * FROM `nodeassociation` WHERE SOURCE_NODE_ID = 10074
/*
SELECT * FROM `nodeassociation` WHERE SOURCE_NODE_ENTITY = "Project" AND ASSOCIATION_TYPE = "ProjectScheme" AND SINK_NODE_ENTITY = "WorkflowScheme" AND SOURCE_NODE_ID = 10074
*/

SELECT ID AS id, pname AS pname FROM issuetype AS i, (SELECT OPTIONID FROM optionconfiguration AS o,
 (SELECT FIELDCONFIGSCHEME FROM configurationcontext WHERE PROJECT = 10074 AND customfield = "issuetype") AS f WHERE o.FIELDCONFIG IN (f.FIELDCONFIGSCHEME)
 ) AS t WHERE i.ID IN (t.OPTIONID) ORDER BY id
 
 SELECT * FROM configurationcontext WHERE PROJECT = 10074 AND customfield = "issuetype"

SELECT o.* FROM optionconfiguration AS o, (SELECT FIELDCONFIGSCHEME FROM configurationcontext WHERE PROJECT = 10074 AND customfield = "issuetype") AS f WHERE o.FIELDCONFIG IN (f.FIELDCONFIGSCHEME)

SELECT * FROM `nodeassociation` WHERE `SOURCE_NODE_ENTITY` = "Issue" AND `SINK_NODE_ENTITY`="Component"

SELECT DISTINCT * FROM customfield WHERE CUSTOMFIELDTYPEKEY="com.atlassian.jira.plugin.system.customfieldtypes:datepicker"

SELECT FIELDCONFIGSCHEME FROM configurationcontext WHERE PROJECT = 10600
SELECT customfield FROM configurationcontext WHERE PROJECT = 10600


SELECT * FROM customfield AS cf WHERE CONCAT("customfield_",cf.ID) IN (SELECT customfield FROM configurationcontext WHERE PROJECT = 10600)
SELECT * FROM customfield AS cf WHERE CONCAT("customfield_",cf.ID) IN (SELECT customfield FROM configurationcontext WHERE PROJECT = 10401)

SELECT SIOPCGI 10401

"Date livraison HTEC réelle "

SELECT * FROM customfield WHERE cfname ="Date livraison HTEC réelle"
ID     CUSTOMFIELDTYPEKEY                                          CUSTOMFIELDSEARCHERKEY                                           cfname
10207  "com.atlassian.jira.plugin.system.customfieldtypes:datetime"  com.atlassian.jira.plugin.system.customfieldtypes:datetimerange  "Date livraison HTEC réelle"

SELECT * FROM configurationcontext WHERE customfield = CONCAT("customfield_",10207)
ID     PROJECTCATEGORY  PROJECT  customfield        FIELDCONFIGSCHEME  
10364                            customfield_10207  10307
SELECT * FROM fieldconfigscheme WHERE ID = 10364 -> 0

SELECT * FROM fieldconfigscheme WHERE FIELDID = CONCAT("customfield_",10508)
SELECT DISTINCT * FROM customfield WHERE CUSTOMFIELDTYPEKEY="com.atlassian.jira.plugin.system.customfieldtypes:select"
ID     CUSTOMFIELDTYPEKEY                                        CUSTOMFIELDSEARCHERKEY                                                 cfname                                  defaultvalue  FIELDTYPE  PROJECT  ISSUETYPE  
10508  com.atlassian.jira.plugin.system.customfieldtypes:SELECT  com.atlassian.jira.plugin.system.customfieldtypes:multiselectsearcher  groupama-motif-attente                                                               
SELECT * FROM `customfieldoption` WHERE CUSTOMFIELD = 10508
SELECT * FROM `customfieldvalue` WHERE CUSTOMFIELD = 10508
ID     ISSUE  CUSTOMFIELD  PARENTKEY  STRINGVALUE  NUMBERVALUE  DATEVALUE  VALUETYPE  
24377  11644  10508                   11810                                           
29274  11683  10508                   11711                                           
31178  11776  10508                   11712                                           
SELECT * FROM `fieldconfiguration` WHERE FIELDID = CONCAT("customfield_",10508)
ID     configname                                 FIELDID            CUSTOMFIELD  
10610  DEFAULT Configuration FOR Motif d attente  customfield_10508               
SELECT * FROM `fieldlayoutitem` WHERE FIELDIDENTIFIER = CONCAT("customfield_",10508)
ID     FIELDLAYOUT  FIELDIDENTIFIER    VERTICALPOSITION  ISHIDDEN  ISREQUIRED  RENDERERTYPE        
31647  10301        customfield_10508                    FALSE     FALSE       jira-TEXT-renderer  
31772  10302        customfield_10508                    FALSE     FALSE       jira-TEXT-renderer  
33514  10300        customfield_10508                    FALSE     FALSE       jira-TEXT-renderer  

SELECT * FROM `propertystring` WHERE propertyvalue LIKE "%Motif%"
ID     
23242  Motif d'attente '
23243  Motif d'attente '
SELECT pe.* FROM `propertyentry` AS pe INNER JOIN `propertystring` AS ps ON pe.ID = ps.ID WHERE pe.ENTITY_ID=10508 AND pe.ENTITY_NAME="CustomField" AND PROPERTY_KEY LIKE "%FR"
SELECT pe.ENTITY_ID AS id, ps.propertyvalue AS pname FROM `propertyentry` AS pe INNER JOIN `propertystring` AS ps ON pe.ID = ps.ID WHERE pe.ENTITY_NAME="CustomField" AND PROPERTY_KEY LIKE "%FR" AND ps.propertyvalue IN ("Motif d'attente")


SELECT DISTINCT * FROM customfield WHERE CUSTOMFIELDTYPEKEY="com.atlassian.jira.plugin.system.customfieldtypes:datetime"
ID     CUSTOMFIELDTYPEKEY                                          CUSTOMFIELDSEARCHERKEY                                           cfname                                  defaultvalue  FIELDTYPE  PROJECT  ISSUETYPE  
10200  com.atlassian.jira.plugin.system.customfieldtypes:DATETIME  com.atlassian.jira.plugin.system.customfieldtypes:datetimerange  DATE début réalisation réelle                                                     
SELECT * FROM `customfieldvalue` WHERE CUSTOMFIELD = 10200

SELECT DISTINCT * FROM customfield WHERE CUSTOMFIELDTYPEKEY="com.atlassian.jira.plugin.system.customfieldtypes:datepicker"
ID     CUSTOMFIELDTYPEKEY                                            CUSTOMFIELDSEARCHERKEY                                       cfname            defaultvalue  FIELDTYPE  PROJECT  ISSUETYPE  
10702  com.atlassian.jira.plugin.system.customfieldtypes:datepicker  com.atlassian.jira.plugin.system.customfieldtypes:daterange  DATE facturation                                               
SELECT * FROM `customfieldvalue` WHERE CUSTOMFIELD = 10702

SELECT DISTINCT * FROM customfield WHERE CUSTOMFIELDTYPEKEY="com.atlassian.jira.plugin.system.customfieldtypes:multiselect"
ID     CUSTOMFIELDTYPEKEY                                             CUSTOMFIELDSEARCHERKEY                                                 cfname               defaultvalue  FIELDTYPE  PROJECT  ISSUETYPE  
10116  com.atlassian.jira.plugin.system.customfieldtypes:multiselect  com.atlassian.jira.plugin.system.customfieldtypes:multiselectsearcher  Applications autres                                               
10401  com.atlassian.jira.plugin.system.customfieldtypes:multiselect  com.atlassian.jira.plugin.system.customfieldtypes:multiselectsearcher  Stage                                                             
SELECT * FROM `customfieldvalue` WHERE CUSTOMFIELD = 10116

SELECT DISTINCT * FROM customfield WHERE CUSTOMFIELDTYPEKEY="com.atlassian.jira.plugin.system.customfieldtypes:radiobuttons"
ID     CUSTOMFIELDTYPEKEY                                              CUSTOMFIELDSEARCHERKEY                                                 cfname                  defaultvalue  FIELDTYPE  PROJECT  ISSUETYPE  
10502  com.atlassian.jira.plugin.system.customfieldtypes:radiobuttons  com.atlassian.jira.plugin.system.customfieldtypes:multiselectsearcher  Moyen de contournement                                               
10503  com.atlassian.jira.plugin.system.customfieldtypes:radiobuttons  com.atlassian.jira.plugin.system.customfieldtypes:multiselectsearcher  Incident                                                             
SELECT * FROM `customfieldvalue` WHERE CUSTOMFIELD = 10502

SELECT DISTINCT * FROM customfield WHERE CUSTOMFIELDTYPEKEY="com.atlassian.jira.plugin.system.customfieldtypes:multicheckboxes"
ID     CUSTOMFIELDTYPEKEY                                                 CUSTOMFIELDSEARCHERKEY                                                 cfname                defaultvalue  FIELDTYPE  PROJECT  ISSUETYPE  
10223  com.atlassian.jira.plugin.system.customfieldtypes:multicheckboxes  com.atlassian.jira.plugin.system.customfieldtypes:multiselectsearcher  A livrer en                                                        
10224  com.atlassian.jira.plugin.system.customfieldtypes:multicheckboxes  com.atlassian.jira.plugin.system.customfieldtypes:multiselectsearcher  Livré en                                                          
10402  com.atlassian.jira.plugin.system.customfieldtypes:multicheckboxes  com.atlassian.jira.plugin.system.customfieldtypes:multiselectsearcher  TYPE de test(s)                                                    
10500  com.atlassian.jira.plugin.system.customfieldtypes:multicheckboxes  com.atlassian.jira.plugin.system.customfieldtypes:multiselectsearcher  Livraison exécution                                               
10501  com.atlassian.jira.plugin.system.customfieldtypes:multicheckboxes  com.atlassian.jira.plugin.system.customfieldtypes:multiselectsearcher  Livraison conception                                               
SELECT * FROM `customfieldvalue` WHERE CUSTOMFIELD = 10223

SELECT * FROM `label` WHERE FIELDID IS NOT NULL

SELECT * FROM `jiraissue` WHERE PROJECT = ? WHERE issuenum > ? AND issuenum < ?

Liste des composants d'un projet avec le nombre de ticket associés :
SELECT cp.cname, cp.ID, COUNT(i.ID) FROM (SELECT c.cname AS cname, c.ID AS ID FROM `component` AS c WHERE c.PROJECT = 10074) AS cp, 
 jiraissue AS i INNER JOIN nodeassociation AS n ON (n.SOURCE_NODE_ID=i.ID AND n.SINK_NODE_ENTITY = "Component" AND n.SOURCE_NODE_ENTITY = "Issue") 
WHERE i.PROJECT = 10074 AND cp.ID = n.SINK_NODE_ID GROUP BY cp.ID

Positionner la résolution à "Done" pour les tickets résolus et fermés sans résolution
UPDATE `jiraissue` SET RESOLUTION=10100, RESOLUTIONDATE=UPDATED WHERE project = 10600 AND (issuestatus=6 OR issuestatus=5) AND RESOLUTION IS NULL;