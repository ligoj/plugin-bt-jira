define({
	"steps" : ["Validation des paramètres du service", "Validation de la version de JIRA", "Lecture du fichier CSV", "Validation de la syntaxe des changements", "Validation de la chronologie et constance de la PKEY", "Collection des données requises pour les tickets", "Validation des statuts requis", "Validation des priorités requises", "Validation des résolutions requises", "Validation des types requis", "Validation des champs personnalisés requis", "Validation des utilisateurs requis", "Rapprochement des identifiants", "Calcul des changements", "Calcul des étiquettes de fin", "Calcul des types de workflow", "Validation des statuts de workflow", "Validation des statuts des resolutions", "Calcul des nouveaux composants", "Calcul des nouvelles versions", "Calcul des tickets à mettre à jour", "Création des nouveaux composants", "Création des nouvelles versions", "Création des tickets", "Association des composants et des versions", "Valorisation des champs personnalisés", "Association des étiquettes", "Création des historiques des statuts", "Synchonisation du cache et de l'index de JIRA"],
	"import-succeed" : "Import réussi, '{{this}}' changements",
	"import-failed" : "Import échoué",
	"service:bt:jira:url-pkey" : "Page d'accueil JIRA de ce projet",
	"service:bt:jira:csv" : "Données simples, format CSV (;)",
	"service:bt:jira:sla-xls" : "SLA, format Excel 2003+",
	"service:bt:jira:sla-csv" : "SLA et peu de données, format CSV (;)",
	"service:bt:jira:sla-csv-full" : "SLA et données, format CSV (;) (long)",
	"service:bt:jira:sla-csv-status" : "Historique des statuts, format CSV (;)",
	"service:bt:jira:import" : "Importer historique en CSV (;)",
	"service:bt:jira:pkey" : "Key",
	"service:bt:jira:status" : '<span style="color: {{{[0]}}}">&#9679;</span>&nbspNon résolues<br>{{{[1]}}} : {{{[2]}}}/{{{[3]}}} ({{{[4]}}}%)<br>Cliquer pour voir les tickets.',
	"jira-database-success" : "Connexion validée, version détectée est {{this}}",
	"jira-project-success" : "Projet {{this.description}}",
	"jira-admin-success" : "Accès administrateur réussi",
	"error" : {
		"jira-database" : "Connexion échouée : {{this}}",
		"jira-project" : "Invalide PKEY ou id",
		"jira-admin" : "Accès administrateur échoué"
	}

});
