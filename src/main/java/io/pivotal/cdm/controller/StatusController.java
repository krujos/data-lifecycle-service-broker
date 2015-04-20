package io.pivotal.cdm.controller;

import io.pivotal.cdm.dto.InstancePair;
import io.pivotal.cdm.service.LCServiceInstanceBindingService;
import io.pivotal.cdm.service.LCServiceInstanceService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

@RestController
class StatusController {

	@Autowired
	private LCServiceInstanceService instanceService;

	@Autowired
	private LCServiceInstanceBindingService bindingService;

	@RequestMapping(value = "/api/instances", method = RequestMethod.GET)
	ResponseEntity<List<InstancePair>> getServiceInstances() {
		return new ResponseEntity<List<InstancePair>>(
				instanceService.getProvisionedInstances(), HttpStatus.OK);
	}

	@RequestMapping(value = "/api/bindings", method = RequestMethod.GET)
	ResponseEntity<List<InstancePair>> getServiceBindings() {
		return new ResponseEntity<List<InstancePair>>(
				bindingService.getAppToCopyBinding(), HttpStatus.OK);
	}

	@RequestMapping(value = "/api/sourceinstance", method = RequestMethod.GET)
	public ResponseEntity<String> getSourceInstance() throws JSONException {
		JSONObject resp = new JSONObject();
		resp.put("sourceInstance", instanceService.getSourceInstanceId());
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return new ResponseEntity<String>(resp.toString(), headers,
				HttpStatus.OK);
	}

}
