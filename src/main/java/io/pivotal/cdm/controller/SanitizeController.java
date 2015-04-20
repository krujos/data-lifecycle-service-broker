package io.pivotal.cdm.controller;

import io.pivotal.cdm.service.DataProviderService;
import net.minidev.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@RestController
class SanitizeController {

	@Autowired
	private DataProviderService dataService;

	private static final String location = "/api/sanitizescript";

	@RequestMapping(value = location, method = RequestMethod.GET)
	public ResponseEntity<JSONObject> getSanitizeScript() {
		String script = dataService.getScript();
		JSONObject response = new JSONObject();
		if (null != script) {

			response.put("script", script);
		}
		return new ResponseEntity<JSONObject>(response, HttpStatus.OK);
	}

	@RequestMapping(value = location, method = RequestMethod.POST, produces = "application/json")
	public ResponseEntity<String> addSanitizeScript(@RequestBody Map<String, Object> script) {

		dataService.saveScript((String) script.get("script"));
		String response = "{}";

		HttpHeaders headers = new HttpHeaders();
		UriComponents components = UriComponentsBuilder.fromPath(location)
				.buildAndExpand();

		headers.setLocation(components.toUri());
		return new ResponseEntity<String>(response, headers, HttpStatus.CREATED);
	}
}
