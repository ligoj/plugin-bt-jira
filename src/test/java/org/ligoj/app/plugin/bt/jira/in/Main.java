package org.ligoj.app.plugin.bt.jira.in;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;

/**
 * Société Générale - UO to Groovy
 */
public class Main {

	private static final String[] KNOWN_TYPES = { "RG-Régularisation - Restauration de service", "RG-Régularisation-Supervision SAU B@D",
			"RG-Régularisation-Support fonctionnel SAU B@D", "RG-Régularisation-Support technique SAU B@D",
			"ET-Conception+Exécution tests Assemblage", "ET-Maintenance corrective", "ET-Maintenance évolutive", "ET-Maintenance technique < 20 j",
			"ET-Paramétrage applicatif", "ET-Stratégie+Conception+Exécution tests d'assemblage", "ET-Support - Assistance N3",
			"ET-Surveillance des applications" };

	private static final String[] KNOWN_DOMAINES = { "AMD", "CdC CMW", "Dmet SAS", "GPA DwH", "Maintenance du poste de travail BBDF (Siebel)",
			"Marketing", "Microstrategy", "Moteurs CDN", "Pil Financier & Risques", "Projet", "Release Datawarehouse", "SAS Zos", "ACP", "ARC",
			"Crédit Immo Gestion", "Crédit Immo Instruction", "Crédits Conso", "Crédits Pro/Ent", "Epargne Assurance Crédit", "Epargne et services",
			"GED", "Gestion des sûretés, gestion des provisions, gestion du contentieux", "Gestion Situations à risques", "Mistral", "Phare (ZOS)",
			"Projet (disabled)", "RES (disabled)", "Risques", "Risques de crédits", "Risques opérationnels - contrôle blanchiment",
			"Risques opérationnels - contrôle conformité", "Services aux agences", "Services Réseaux (OBAD)", "Socle", "Succession", "ZOS I1",
			"Centre de dev Psystem", "DIS - CdC test (disabled)", "DIS - REF - CDN", "FID/Ficoba/Synopsis", "Just, Audit, IDCE", "Mistral",
			"PFI - CdC test (disabled)", "Production de données", "Projet", "PSE - CdC test (disabled)", "QSI - CNV - Convergence", "QSI - DIS - FDS",
			"QSI - DIS - Orchestra GED", "QSI - DIS - Patrimoine", "QSI - DIS - PDT", "QSI - DIS - PRIV", "QSI - DIS - Référentiels",
			"QSI - PAI - Banques", "QSI - PAI - Chèques", "QSI - PAI - Trade", "QSI - RCE - Crédits", "QSI - RCE - Epargne", "QSI - RCE - Risque",
			"RCE - CdC test (disabled)", "RHM", "Référentiel CDN", "Synthèses & Moyens généraux", "Titres", "Bourse", "CCM", "CDN", "CMI", "LPP",
			"LPS", "Maintenance Poste de Travail", "Offres commerciales", "PLR", "PRI", "QSI - DIS - CAN", "QSI - DIS - FDS", "QSI - DIS - PDT",
			"QSI - PAI - ACQ", "QSI - PAI - CMI", "QSI - PAI - RES", "QSI - PAI - Restitution", "QSI - PRC - Pilotage", "SEC et PAR", "Sogecashweb",
			"Vocal", "PRI-SAU B@D" };

	public static void main(final String... args) throws IOException {
		Assert.assertEquals("Invalid arguments", 1, args.length);
		final PrintStream out = System.out;
		out.print("def Object[][] tables = [");
		boolean first = true;
		for (String line : IOUtils.readLines(new BOMInputStream(new FileInputStream(args[0])), StandardCharsets.UTF_8)) {
			line = StringUtils.trimToNull(line);
			if (line == null) {
				continue;
			}
			final String[] fields = line.split("[;\t]");
			Assert.assertEquals("Missing field", 5, fields.length);
			if (first) {
				first = false;
			} else {
				out.print(',');
			}
			out.print("\n\t[");
			boolean first2 = true;
			for (int index = 0; index < fields.length; index++) {
				String field = fields[index];
				if (first2) {
					first2 = false;
				} else {
					out.print(',');
				}

				if (index < 3) {
					// String type
					out.print('\"');

					if (index == 0) {
						// Issue type
						field = findMatch(field, KNOWN_TYPES);
					} else if (index == 1) {
						// Issue type
						field = findMatch(field, KNOWN_DOMAINES);
					}
					out.print(field);
					out.print('\"');
				} else {
					out.print("Double.valueOf(");
					out.print(field.replace(',', '.'));
					out.print(')');
				}
			}
			out.print(']');
		}
		out.println("];");
		System.exit(0);
	}

	/**
	 * Search a match.
	 */
	private static String findMatch(final String field, final String[] jiraNames) {
		final String tokenizedField = normalize(field);
		for (final String jiraName : jiraNames) {
			final String tokenizedJira = normalize(jiraName);
			if (tokenizedJira.equals(tokenizedField)) {
				return tokenizedField;
			}
		}
		Assert.fail("Uknown name " + field);
		return null;
	}

	private static String normalize(final String name) {
		return java.text.Normalizer.normalize(name.replaceAll("[\\s\\-,&()?]", ""), java.text.Normalizer.Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toUpperCase(java.util.Locale.ENGLISH);
	}
}
