CREATE TABLE changegroup (
  ID decimal(18,0) NOT NULL,
  issueid decimal(18,0) DEFAULT NULL,
  AUTHOR varchar(255) DEFAULT NULL,
  CREATED datetime DEFAULT NULL,
  PRIMARY KEY (ID),
  KEY chggroup_issue (issueid)
);
CREATE TABLE changeitem (
  ID decimal(18,0) NOT NULL,
  groupid decimal(18,0) DEFAULT NULL,
  FIELDTYPE varchar(255) DEFAULT NULL,
  FIELD varchar(255) DEFAULT NULL,
  OLDVALUE longtext,
  OLDSTRING longtext,
  NEWVALUE longtext,
  NEWSTRING longtext,
  PRIMARY KEY (ID),
  KEY chgitem_chggrp (groupid),
  KEY chgitem_field (FIELD)
);
CREATE TABLE issuestatus (
  ID varchar(60) NOT NULL,
  SEQUENCE decimal(18,0) DEFAULT NULL,
  pname varchar(60) DEFAULT NULL,
  DESCRIPTION text,
  ICONURL varchar(255) DEFAULT NULL,
  PRIMARY KEY (ID)
);
CREATE TABLE issuetype (
  ID varchar(60) NOT NULL,
  SEQUENCE decimal(18,0) DEFAULT NULL,
  pname varchar(60) DEFAULT NULL,
  pstyle varchar(60) DEFAULT NULL,
  DESCRIPTION text,
  ICONURL varchar(255) DEFAULT NULL,
  PRIMARY KEY (ID)
);
CREATE TABLE jiraissue (
  ID decimal(18,0) NOT NULL,
  pkey varchar(255) DEFAULT NULL,
  PROJECT decimal(18,0) DEFAULT NULL,
  REPORTER varchar(255) DEFAULT NULL,
  ASSIGNEE varchar(255) DEFAULT NULL,
  issuetype varchar(255) DEFAULT NULL,
  SUMMARY varchar(255) DEFAULT NULL,
  DESCRIPTION longtext,
  ENVIRONMENT longtext,
  PRIORITY varchar(255) DEFAULT NULL,
  RESOLUTION varchar(255) DEFAULT NULL,
  issuestatus varchar(255) DEFAULT NULL,
  CREATED datetime DEFAULT NULL,
  UPDATED datetime DEFAULT NULL,
  DUEDATE datetime DEFAULT NULL,
  VOTES decimal(18,0) DEFAULT NULL,
  TIMEORIGINALESTIMATE decimal(18,0) DEFAULT NULL,
  TIMEESTIMATE decimal(18,0) DEFAULT NULL,
  TIMESPENT decimal(18,0) DEFAULT NULL,
  WORKFLOW_ID decimal(18,0) DEFAULT NULL,
  SECURITY decimal(18,0) DEFAULT NULL,
  FIXFOR decimal(18,0) DEFAULT NULL,
  COMPONENT decimal(18,0) DEFAULT NULL,
  RESOLUTIONDATE datetime DEFAULT NULL,
  WATCHES decimal(18,0) DEFAULT NULL,
  PRIMARY KEY (ID),
  KEY issue_key (pkey),
  KEY issue_proj_status (PROJECT,issuestatus),
  KEY issue_assignee (ASSIGNEE),
  KEY issue_workflow (WORKFLOW_ID)
);
CREATE TABLE jiraworkflows (
  ID decimal(18,0) NOT NULL,
  workflowname varchar(255) DEFAULT NULL,
  creatorname varchar(255) DEFAULT NULL,
  DESCRIPTOR longtext,
  ISLOCKED varchar(60) DEFAULT NULL,
  PRIMARY KEY (ID)
);
CREATE TABLE nodeassociation (
  SOURCE_NODE_ID decimal(18,0) NOT NULL,
  SOURCE_NODE_ENTITY varchar(60) NOT NULL,
  SINK_NODE_ID decimal(18,0) NOT NULL,
  SINK_NODE_ENTITY varchar(60) NOT NULL,
  ASSOCIATION_TYPE varchar(60) NOT NULL,
  SEQUENCE decimal(9,0) DEFAULT NULL,
  PRIMARY KEY (SOURCE_NODE_ID,SOURCE_NODE_ENTITY,SINK_NODE_ID,SINK_NODE_ENTITY,ASSOCIATION_TYPE),
  KEY node_source (SOURCE_NODE_ID,SOURCE_NODE_ENTITY),
  KEY node_sink (SINK_NODE_ID,SINK_NODE_ENTITY)
);
CREATE TABLE priority (
  ID varchar(60) NOT NULL,
  SEQUENCE decimal(18,0) DEFAULT NULL,
  pname varchar(60) DEFAULT NULL,
  DESCRIPTION text,
  ICONURL varchar(255) DEFAULT NULL,
  STATUS_COLOR varchar(60) DEFAULT NULL,
  PRIMARY KEY (ID)
);
CREATE TABLE project (
  ID decimal(18,0) NOT NULL,
  pname varchar(255) DEFAULT NULL,
  URL varchar(255) DEFAULT NULL,
  LEAD varchar(255) DEFAULT NULL,
  DESCRIPTION text,
  pkey varchar(255) DEFAULT NULL,
  pcounter decimal(18,0) DEFAULT NULL,
  ASSIGNEETYPE decimal(18,0) DEFAULT NULL,
  AVATAR decimal(18,0) DEFAULT NULL,
  PRIMARY KEY (ID)
);
CREATE TABLE workflowscheme (
  ID decimal(18,0) NOT NULL,
  NAME varchar(255) DEFAULT NULL,
  DESCRIPTION text,
  PRIMARY KEY (ID)
);
CREATE TABLE workflowschemeentity (
  ID decimal(18,0) NOT NULL,
  SCHEME decimal(18,0) DEFAULT NULL,
  WORKFLOW varchar(255) DEFAULT NULL,
  issuetype varchar(255) DEFAULT NULL,
  PRIMARY KEY (ID),
  KEY workflow_scheme (SCHEME)
);
