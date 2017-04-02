CREATE TABLE issuestatus (  ID varchar(60) NOT NULL,  SEQUENCE decimal(18,0) DEFAULT NULL,  pname varchar(60) DEFAULT NULL,  DESCRIPTION LONGVARCHAR,  ICONURL varchar(255) DEFAULT NULL,  PRIMARY KEY (ID));
CREATE TABLE issuetype (  ID varchar(60) NOT NULL,  SEQUENCE decimal(18,0) DEFAULT NULL,  pname varchar(60) DEFAULT NULL,  pstyle varchar(60) DEFAULT NULL,  DESCRIPTION LONGVARCHAR,  ICONURL varchar(255) DEFAULT NULL,  PRIMARY KEY (ID));
CREATE TABLE nodeassociation (  SOURCE_NODE_ID decimal(18,0) NOT NULL,  SOURCE_NODE_ENTITY varchar(60) NOT NULL,  SINK_NODE_ID decimal(18,0) NOT NULL,  SINK_NODE_ENTITY varchar(60) NOT NULL,  ASSOCIATION_TYPE varchar(60) NOT NULL,  SEQUENCE decimal(9,0) DEFAULT NULL,  PRIMARY KEY (SOURCE_NODE_ID,SOURCE_NODE_ENTITY,SINK_NODE_ID,SINK_NODE_ENTITY,ASSOCIATION_TYPE));
CREATE TABLE priority (  ID varchar(60) NOT NULL,  SEQUENCE decimal(18,0) default NULL,  pname varchar(60) default NULL,  DESCRIPTION varchar(255),  ICONURL varchar(255) default NULL,  STATUS_COLOR varchar(60) default NULL,  PRIMARY KEY  (ID));
CREATE TABLE project (  ID decimal(18,0) NOT NULL,  pname varchar(255) DEFAULT NULL,  URL varchar(255) DEFAULT NULL,  LEAD varchar(255) DEFAULT NULL,  DESCRIPTION LONGVARCHAR,  pkey varchar(255) DEFAULT NULL,  pcounter decimal(18,0) DEFAULT NULL,  ASSIGNEETYPE decimal(18,0) DEFAULT NULL,  AVATAR decimal(18,0) DEFAULT NULL,  PRIMARY KEY (ID));
CREATE TABLE workflowschemeentity (  ID decimal(18,0) NOT NULL,  SCHEME decimal(18,0) DEFAULT NULL,  WORKFLOW varchar(255) DEFAULT NULL,  issuetype varchar(255) DEFAULT NULL,  PRIMARY KEY (ID));
CREATE TABLE project_key (  ID decimal(18,0) NOT NULL, PROJECT_ID decimal(18,0) NOT NULL,  PROJECT_KEY varchar(255) NOT NULL,  PRIMARY KEY (ID));
CREATE TABLE propertystring (  ID decimal(18,0) NOT NULL, propertyvalue varchar(255) NOT NULL,  PRIMARY KEY (ID));
CREATE TABLE propertyentry (  ID decimal(18,0) NOT NULL, ENTITY_NAME varchar(255) default NULL, ENTITY_ID decimal(18,0) default 1, PROPERTY_KEY varchar(255) NOT NULL,  PRIMARY KEY (ID));
CREATE TABLE resolution (  ID varchar(60) NOT NULL,  SEQUENCE decimal(18,0) default NULL,  pname varchar(60) default NULL,  DESCRIPTION varchar(255),  ICONURL varchar(255) default NULL,  PRIMARY KEY  (ID));
CREATE TABLE cwd_user (  ID decimal(18,0) NOT NULL,  directory_id decimal(18,0) DEFAULT NULL,  user_name varchar(255) DEFAULT NULL,  lower_user_name varchar(255) DEFAULT NULL,  active decimal(9,0) DEFAULT NULL,  created_date datetime DEFAULT NULL,  updated_date datetime DEFAULT NULL,  first_name varchar(255) DEFAULT NULL,  lower_first_name varchar(255) DEFAULT NULL,  last_name varchar(255) DEFAULT NULL,  lower_last_name varchar(255) DEFAULT NULL,  display_name varchar(255) DEFAULT NULL,  lower_display_name varchar(255) DEFAULT NULL,  email_address varchar(255) DEFAULT NULL,  lower_email_address varchar(255) DEFAULT NULL,  CREDENTIAL varchar(255) DEFAULT NULL,  deleted_externally decimal(9,0) DEFAULT NULL,  EXTERNAL_ID varchar(255) DEFAULT NULL,  PRIMARY KEY (ID));
CREATE TABLE cwd_user_attributes (  ID decimal(18,0) NOT NULL,  user_id decimal(18,0) DEFAULT NULL,  directory_id decimal(18,0) DEFAULT NULL,  attribute_name varchar(255) DEFAULT NULL,  attribute_value varchar(255) DEFAULT NULL,  lower_attribute_value varchar(255) DEFAULT NULL,  PRIMARY KEY (ID));
CREATE TABLE optionconfiguration (  ID decimal(18,0) NOT NULL,  FIELDID varchar(60) default NULL,  OPTIONID varchar(60) default NULL,  FIELDCONFIG decimal(18,0) default NULL,  SEQUENCE decimal(18,0) default NULL,  PRIMARY KEY  (ID));
CREATE TABLE configurationcontext (  ID decimal(18,0) NOT NULL,  PROJECTCATEGORY decimal(18,0) default NULL,  PROJECT decimal(18,0) default NULL,  customfield varchar(255) default NULL,  FIELDCONFIGSCHEME decimal(18,0) default NULL,  PRIMARY KEY  (ID));
