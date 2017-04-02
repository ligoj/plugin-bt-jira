package org.ligoj.app.plugin.bt.jira.model;

import java.util.Map;

import org.ligoj.bootstrap.core.INamableBean;

import lombok.Getter;
import lombok.Setter;

/**
 * A workflow with valid steps
 */
@Getter
@Setter
public class Workflow {

	/**
	 * The workflow's name, and also identifier.
	 */
	private String name;

	/**
	 * The mapping status to step identifier/name.
	 */
	private Map<String, INamableBean<Integer>> statusToSteps;
}
