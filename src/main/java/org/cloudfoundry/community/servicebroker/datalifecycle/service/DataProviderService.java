package org.cloudfoundry.community.servicebroker.datalifecycle.service;

import org.cloudfoundry.community.servicebroker.datalifecycle.model.SanitizationScript;
import org.cloudfoundry.community.servicebroker.datalifecycle.repo.ScriptRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataProviderService {

	private ScriptRepo scriptRepo;

	@Autowired
	public DataProviderService(ScriptRepo scriptRepo) {
		this.scriptRepo = scriptRepo;
	}

	public void saveScript(String script) {
		scriptRepo.save(new SanitizationScript(script));
	}

	/**
	 * Retrieve the sanitization script from the repo
	 * 
	 * @return the script if one has been set, null otherwise.
	 */
	public String getScript() {
		SanitizationScript result = scriptRepo.findOne(SanitizationScript.ID);

		return result == null ? null : result.getScript();
	}
}
