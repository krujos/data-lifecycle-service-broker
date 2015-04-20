package io.pivotal.cdm.service;

import io.pivotal.cdm.model.BindingEntity;
import io.pivotal.cdm.repo.BindingRepository;

import java.util.ArrayList;
import java.util.Collection;

import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.springframework.beans.factory.annotation.Autowired;

public class LCServiceInstanceBindingManager {

	@Autowired
	private BindingRepository repo;

	public LCServiceInstanceBindingManager(BindingRepository repo) {
		this.repo = repo;
	}

	public Collection<ServiceInstanceBinding> getBindings() {
		Collection<ServiceInstanceBinding> bindings = new ArrayList<ServiceInstanceBinding>();
		repo.findAll().forEach(t -> bindings.add(convert(t)));
		return bindings;
	}

	public ServiceInstanceBinding getBinding(String bindingId) {
		return convert(repo.findOne(bindingId));
	}

	public ServiceInstanceBinding removeBinding(String bindingId) {
		BindingEntity binding = repo.findOne(bindingId);
		if (null != binding) {
			repo.delete(bindingId);
		}
		return convert(binding);
	}

	public void saveBinding(ServiceInstanceBinding binding) {
		repo.save(new BindingEntity(binding));
	}

	private ServiceInstanceBinding convert(BindingEntity binding) {
		return null == binding ? null : new ServiceInstanceBinding(
				binding.getBindingId(), binding.getServiceInstanceId(), null,
				binding.getDrainUrl(), binding.getAppGuid());
	}
}
