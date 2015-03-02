package io.pivotal.cdm.model;

import javax.persistence.*;

import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;

/**
 * Savable version of a service binding.
 * 
 * We don't save the creds
 *
 */
@Entity
public class BindingEntity {

	@Id
	private String id;
	private String appGuid;
	private String bindingId;
	private String serviceInstanceId;
	private String drainUrl;

	public BindingEntity() {
	}

	public BindingEntity(ServiceInstanceBinding binding) {
		setAppGuid(binding.getAppGuid());
		setBindingId(binding.getId());
		setServiceInstanceId(binding.getServiceInstanceId());
		setDrainUrl(binding.getSyslogDrainUrl());
		this.setId(binding.getId());
	}

	public String getAppGuid() {
		return appGuid;
	}

	public void setAppGuid(String appGuid) {
		this.appGuid = appGuid;
	}

	public String getBindingId() {
		return bindingId;
	}

	public void setBindingId(String bindingId) {
		this.bindingId = bindingId;
	}

	public String getServiceInstanceId() {
		return serviceInstanceId;
	}

	public void setServiceInstanceId(String serviceInstanceId) {
		this.serviceInstanceId = serviceInstanceId;
	}

	public String getDrainUrl() {
		return drainUrl;
	}

	public void setDrainUrl(String drainUrl) {
		this.drainUrl = drainUrl;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
