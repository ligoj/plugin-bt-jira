define({
	"root" : {
		"steps" : ["Validate subscription settings", "Validate JIRA version", "Read CSV data", "Validate syntax of the changes", "Validate chronology and PKEY constant", "Collect required data for the issues", "Validate required statuses", "Validate required priorities", "Validate required resolutions", "Validate required types", "Validate required custom fields", "Validate required users", "Convert texts to identifiers", "Compute the changes", "Compute the labels", "Compute the workflow types", "Validate the workflow statuses", "Validate the resolution statuses", "Compute the new components", "Compute the new versions", "Compute the issues to update", "Create new components", "Create new versions", "Create issues", "Associate components and versions", "Set custom field values", "Associate labels", "Create status changes history", "Synchronize JIRA cache and index"],
		"import-succeed" : "Import succeed, '{{this}}' changes",
		"import-failed" : "Import failed",
		"service:bt:jira:url-pkey" : "JIRA home page of this project",
		"service:bt:jira:csv" : "Simple data, CSV (;) file",
		"service:bt:jira:sla-xls" : "SLA, Excel 2003+ file",
		"service:bt:jira:sla-csv" : "SLA and few data, CSV (;) file",
		"service:bt:jira:sla-csv-full" : "SLA and data, CSV (;) file (slow)",
		"service:bt:jira:sla-csv-status" : "Status history, CSV (;)",
		"service:bt:jira:import" : "Import history from CSV (;)",
		"service:bt:jira:pkey" : "Clé",
		"service:bt:jira:status" : '<span style="color: {{{[0]}}}">&#9679;</span>&nbspUnresolved<br>{{{[1]}}} : {{{[2]}}}/{{{[3]}}} ({{{[4]}}}%)<br>Click to see issues.',
		"error" : {
			"jira-database" : "Connection failed : {{this}}",
			"jira-project" : "Invalid PKEY or id",
			"jira-admin" : "Administrator access failed"
		}
	},
	"fr" : true
});
