package io.pivotal.cdm.model;

import javax.persistence.*;

@Entity
public class BrokerAction {

	@Id
	private String id;

	private String actor;

	private BrokerActionState state;

	private String action;

	public BrokerAction() {
	};

	public BrokerAction(String id, BrokerActionState state, String action) {
		this.setId(id);
		this.actor = id;
		this.state = state;
		this.action = action;
	}

	public String getActor() {
		return actor;
	}

	public void setActor(String actor) {
		this.actor = actor;
	}

	public BrokerActionState getState() {
		return state;
	}

	public void setState(BrokerActionState state) {
		this.state = state;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
