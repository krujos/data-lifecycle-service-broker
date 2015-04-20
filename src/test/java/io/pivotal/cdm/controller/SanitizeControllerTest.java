package io.pivotal.cdm.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import io.pivotal.cdm.service.DataProviderService;

import net.minidev.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class SanitizeControllerTest {

	private MockMvc mockMvc;

	private String script = "drop * from table";

	private String location = "/api/sanitizescript";

	@InjectMocks
	SanitizeController sanitizeController;

	@Mock
	DataProviderService service;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		mockMvc = MockMvcBuilders
				.standaloneSetup(sanitizeController)
				.setMessageConverters(new MappingJackson2HttpMessageConverter())
				.build();
	}

	@Test
	public void itShouldTellMeWhereToGetTheScriptIvePosted() throws Exception {
		JSONObject input = new JSONObject();
		input.put("script", script);
		mockMvc.perform(
				post(location)
						.contentType(MediaType.APPLICATION_JSON)
						.content(input.toJSONString())
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", location));

		verify(service, times(1)).saveScript(any());
	}

	@Test
	public void itGetsTheScript() throws Exception {
		when(service.getScript()).thenReturn(script);

		mockMvc.perform(get(location).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.script", is(script)));
	}

	@Test
	public void itReturnsEmptyResponseWithNoScript() throws Exception {

		when(service.getScript()).thenReturn(null);

		mockMvc.perform(get(location).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(content().string("{}"));
	}
}
