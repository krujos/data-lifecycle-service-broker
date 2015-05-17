package org.cloudfoundry.community.servicebroker.datalifecycle.model;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.cloudfoundry.community.servicebroker.model.ServiceInstance;

@Entity
public class ServiceInstanceEntity {

	@Id
	private String id;

	private String copyId;

	private String spaceGuid;

	private String serviceInstanceId;

	private String planGuid;

	private String orgGuid;

	private String dashboardUrl;

	private String serviceDefinitionId;

	private String lastOperationState;

	private String lastOperationDescription;

	public ServiceInstanceEntity() {
	}

	public ServiceInstanceEntity(ServiceInstance instance, String copyId) {
		this.setCopyId(copyId);
		this.setDashboardUrl(instance.getDashboardUrl());
		this.setOrgGuid(instance.getOrganizationGuid());
		this.setPlanGuid(instance.getPlanId());
		this.setServiceDefinitionId(instance.getServiceDefinitionId());
		this.setServiceInstanceId(instance.getServiceInstanceId());
		this.setId(instance.getServiceInstanceId());
		this.setSpaceGuid(instance.getSpaceGuid());
		this.setLastOperationDescription(instance
				.getServiceInstanceLastOperation().getDescription());
		this.setLastOperationState(instance.getServiceInstanceLastOperation()
				.getState());
	}

	public String getCopyId() {
		return copyId;
	}

	private void setCopyId(String copyId) {
		this.copyId = copyId;
	}

	public String getSpaceGuid() {
		return spaceGuid;
	}

	private void setSpaceGuid(String spaceGuid) {
		this.spaceGuid = spaceGuid;
	}

	public String getServiceInstanceId() {
		return serviceInstanceId;
	}

	private void setServiceInstanceId(String serviceInstanceId) {
		this.serviceInstanceId = serviceInstanceId;
	}

	public String getPlanGuid() {
		return planGuid;
	}

	private void setPlanGuid(String planGuid) {
		this.planGuid = planGuid;
	}

	public String getOrgGuid() {
		return orgGuid;
	}

	private void setOrgGuid(String orgGuid) {
		this.orgGuid = orgGuid;
	}

	public String getDashboardUrl() {
		return dashboardUrl;
	}

	private void setDashboardUrl(String dashboardUrl) {
		this.dashboardUrl = dashboardUrl;
	}

	public String getServiceDefinitionId() {
		return serviceDefinitionId;
	}

	private void setServiceDefinitionId(String serviceDefinitionId) {
		this.serviceDefinitionId = serviceDefinitionId;
	}

	public String getId() {
		return id;
	}

	private void setId(String id) {
		this.id = id;
	}

	public String getLastOperationState() {
		return lastOperationState;
	}

	private void setLastOperationState(String lastOperationState) {
		this.lastOperationState = lastOperationState;
	}

	public String getLastOperationDescription() {
		return lastOperationDescription;
	}

	private void setLastOperationDescription(String lastOperationDescription) {
		this.lastOperationDescription = lastOperationDescription;
	}

}
