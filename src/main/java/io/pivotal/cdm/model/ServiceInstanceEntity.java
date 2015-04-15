package io.pivotal.cdm.model;

import javax.persistence.*;

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

	public void setCopyId(String copyId) {
		this.copyId = copyId;
	}

	public String getSpaceGuid() {
		return spaceGuid;
	}

	public void setSpaceGuid(String spaceGuid) {
		this.spaceGuid = spaceGuid;
	}

	public String getServiceInstanceId() {
		return serviceInstanceId;
	}

	public void setServiceInstanceId(String serviceInstanceId) {
		this.serviceInstanceId = serviceInstanceId;
	}

	public String getPlanGuid() {
		return planGuid;
	}

	public void setPlanGuid(String planGuid) {
		this.planGuid = planGuid;
	}

	public String getOrgGuid() {
		return orgGuid;
	}

	public void setOrgGuid(String orgGuid) {
		this.orgGuid = orgGuid;
	}

	public String getDashboardUrl() {
		return dashboardUrl;
	}

	public void setDashboardUrl(String dashboardUrl) {
		this.dashboardUrl = dashboardUrl;
	}

	public String getServiceDefinitionId() {
		return serviceDefinitionId;
	}

	public void setServiceDefinitionId(String serviceDefinitionId) {
		this.serviceDefinitionId = serviceDefinitionId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getLastOperationState() {
		return lastOperationState;
	}

	public void setLastOperationState(String lastOperationState) {
		this.lastOperationState = lastOperationState;
	}

	public String getLastOperationDescription() {
		return lastOperationDescription;
	}

	public void setLastOperationDescription(String lastOperationDescription) {
		this.lastOperationDescription = lastOperationDescription;
	}

}
