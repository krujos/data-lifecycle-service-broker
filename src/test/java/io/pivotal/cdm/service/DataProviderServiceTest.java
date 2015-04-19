package io.pivotal.cdm.service;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.pivotal.cdm.model.SanitizationScript;
import io.pivotal.cdm.repo.ScriptRepo;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DataProviderServiceTest {

	@Mock
	ScriptRepo scriptRepo;

	DataProviderService service;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		service = new DataProviderService(scriptRepo);
	}

	@Test
	public void itShouldSaveTheScript() {
		service.saveScript("The Script");
		verify(scriptRepo, times(1)).save(
				argThat(new ArgumentMatcher<SanitizationScript>() {
					@Override
					public boolean matches(Object argument) {
						return ((SanitizationScript) argument).getScript()
								.equals("The Script");
					}
				}));
	}

	@Test
	public void itShouldGetTheScript() {
		Iterable<SanitizationScript> scripts = new ArrayList<SanitizationScript>(
				Arrays.asList(new SanitizationScript("fake scirpt")));
		when(scriptRepo.findAll()).thenReturn(scripts);

		assertThat(service.getScript(), is(notNullValue()));
	}

	@Test
	public void itShouldReturnNullIfTheresNoScript() {
		when(scriptRepo.findAll()).thenReturn(null);
		assertNull(service.getScript());
	}

	@Test
	public void itShouldReturnNullIfTheresNoElements() {
		when(scriptRepo.findAll()).thenReturn(
				new ArrayList<SanitizationScript>());
		assertNull(service.getScript());
	}
}
