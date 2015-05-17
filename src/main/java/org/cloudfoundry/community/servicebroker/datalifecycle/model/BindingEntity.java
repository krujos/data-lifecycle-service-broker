package org.cloudfoundry.community.servicebroker.datalifecycle.model;

import javax.persistence.Entity;
import javax.persistence.Id;

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

	private void setAppGuid(String appGuid) {
		this.appGuid = appGuid;
	}

	public String getBindingId() {
		return bindingId;
	}

	private void setBindingId(String bindingId) {
		this.bindingId = bindingId;
	}

	public String getServiceInstanceId() {
		return serviceInstanceId;
	}

	private void setServiceInstanceId(String serviceInstanceId) {
		this.serviceInstanceId = serviceInstanceId;
	}

	public String getDrainUrl() {
		return drainUrl;
	}

	private void setDrainUrl(String drainUrl) {
		this.drainUrl = drainUrl;
	}

	public String getId() {
		return id;
	}

	private void setId(String id) {
		this.id = id;
	}
}
