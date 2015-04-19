package io.pivotal.cdm.service;

import io.pivotal.cdm.model.SanitizationScript;
import io.pivotal.cdm.repo.ScriptRepo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataProviderService {

	ScriptRepo scriptRepo;

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
