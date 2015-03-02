package io.pivotal.cdm.service;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import io.pivotal.cdm.model.BindingEntity;
import io.pivotal.cdm.repo.BindingRepository;

import java.util.*;

import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.junit.*;
import org.mockito.*;

public class ServiceInstanceBindingManagerTest {

	@Mock
	BindingRepository repo;

	private LCServiceInstanceBindingManager bindingManager;

	private Map<String, Object> creds = new HashMap<String, Object>();
	ServiceInstanceBinding binding = new ServiceInstanceBinding("binding-id",
			"service-instance-id", creds, "syslog-drain", "app-guid");

	BindingEntity bindingEntity = new BindingEntity(binding);

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		bindingManager = new LCServiceInstanceBindingManager(repo);
	}

	@Test
	public void itShoudlReturn1ServiceInstanceBinding() {
		when(repo.findOne("binding")).thenReturn(new BindingEntity(binding));
		assertNotNull(bindingManager.getBinding("binding"));
	}

	@Test
	public void itShouldConvertCorrectly() {
		when(repo.findOne("binding")).thenReturn(new BindingEntity(binding));
		ServiceInstanceBinding retreivedBinding = bindingManager
				.getBinding("binding");

		assertThat(retreivedBinding.getId(), is(equalTo("binding-id")));
		assertThat(retreivedBinding.getCredentials(), is(equalTo(creds)));
		assertThat(retreivedBinding.getAppGuid(), is(equalTo("app-guid")));
		assertThat(retreivedBinding.getServiceInstanceId(),
				is(equalTo("service-instance-id")));
		assertThat(retreivedBinding.getSyslogDrainUrl(),
				is(equalTo("syslog-drain")));

	}

	@Test
	public void itShouldReturnAllServiceBindings() {
		when(repo.findAll()).thenReturn(
				new ArrayList<BindingEntity>(Arrays.asList(bindingEntity,
						bindingEntity, bindingEntity, bindingEntity)));

		assertThat(bindingManager.getBindings(), hasSize(4));
	}

	@Test
	public void itShouldReturnEmptyCollectionWithNothingInRepo() {
		when(repo.findAll()).thenReturn(new ArrayList<BindingEntity>());
		assertThat(bindingManager.getBindings(), hasSize(0));
	}

	@Test
	public void itShouldDeleteOne() {
		when(repo.findOne("binding")).thenReturn(bindingEntity);
		ServiceInstanceBinding returned = bindingManager
				.removeBinding("binding");
		verify(repo).delete("binding");
		assertThat(returned.getId(), is(equalTo("binding-id")));
	}

	@Test
	public void itShouldSaveOne() {
		bindingManager.saveBinding(binding);
		verify(repo).save(any(BindingEntity.class));
	}

	@Test
	public void itShouldReturnNullWhenWeDeleteSometihngThatIsntThere() {
		assertNull(bindingManager.removeBinding("nothing"));
	}
}
