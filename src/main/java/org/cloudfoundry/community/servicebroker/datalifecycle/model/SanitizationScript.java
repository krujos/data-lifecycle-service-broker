package org.cloudfoundry.community.servicebroker.datalifecycle.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class SanitizationScript {

	public final static long ID = 1;

	@Id
	private long id = ID;

	private String script;

	protected SanitizationScript() {
	}

	public SanitizationScript(final String script) {
		this.setScript(script);
	}

	public String getScript() {
		return script;
	}

	private void setScript(String script) {
		this.script = script;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
}
