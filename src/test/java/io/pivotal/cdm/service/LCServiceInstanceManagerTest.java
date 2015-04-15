package io.pivotal.cdm.service;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import io.pivotal.cdm.model.ServiceInstanceEntity;
import io.pivotal.cdm.repo.ServiceInstanceRepo;

import java.util.*;
import java.util.stream.IntStream;

import org.cloudfoundry.community.servicebroker.model.*;
import org.hamcrest.*;
import org.junit.*;
import org.mockito.*;

public class LCServiceInstanceManagerTest {

	private LCServiceInstanceManager instanceManager;

	@Mock
	ServiceInstanceRepo repo;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		instanceManager = new LCServiceInstanceManager(repo);
	}

	@Test
	public void itShouldSaveAnInstance() {
		instanceManager.saveInstance(new ServiceInstance(
				new CreateServiceInstanceRequest("def_id", "plan_id", "org",
						"space").withServiceInstanceId("the_id")), "the_copy");
		verify(repo, times(1)).save(argThat(hasEntityWithId("the_id")));
	}

	@Test
	public void itGetsAnInstanceById() {
		when(repo.findOne("the_id")).thenReturn(makeSIEntity());
		assertNotNull(instanceManager.getInstance("the_id"));
	}

	@Test
	public void itShouldReturnNullForNonExistantInstance() {
		when(repo.findOne(anyString())).thenReturn(null);
		assertNull(instanceManager.getInstance("garbage"));
	}

	@Test
	public void itConvertsProperly() {
		ServiceInstance sourceInstance = new ServiceInstance(
				new CreateServiceInstanceRequest("def_id", "plan_id", "org",
						"space").withServiceInstanceId("the_id"))
				.withLastOperation(new ServiceInstanceLastOperation("failed",
						OperationState.FAILED));
		ServiceInstanceEntity entity = new ServiceInstanceEntity(
				sourceInstance, "theCopy");

		when(repo.findOne(anyString())).thenReturn(entity);
		ServiceInstance returnedInstance = instanceManager
				.getInstance("noMatter");
		assertThat(returnedInstance.getDashboardUrl(),
				is(equalTo(sourceInstance.getDashboardUrl())));
		assertThat(returnedInstance.getOrganizationGuid(),
				is(equalTo(sourceInstance.getOrganizationGuid())));
		assertThat(returnedInstance.getPlanId(),
				is(equalTo(sourceInstance.getPlanId())));
		assertThat(returnedInstance.getServiceDefinitionId(),
				is(equalTo(sourceInstance.getServiceDefinitionId())));
		assertThat(returnedInstance.getServiceInstanceId(),
				is(equalTo(sourceInstance.getServiceInstanceId())));
		assertThat(returnedInstance.getSpaceGuid(),
				is(equalTo(sourceInstance.getSpaceGuid())));
		assertThat(returnedInstance.getServiceInstanceLastOperation()
				.getDescription(), is(equalTo(sourceInstance
				.getServiceInstanceLastOperation().getDescription())));
		assertThat(returnedInstance.getServiceInstanceLastOperation()
				.getState(), is(equalTo(sourceInstance
				.getServiceInstanceLastOperation().getState())));
	}

	@Test
	public void itGetsAllInstances() {
		when(repo.findAll()).thenReturn(makeEntities(20));
		assertThat(instanceManager.getInstances(), hasSize(20));
	}

	@Test
	public void itGetsAnInstanceForACopyId() {
		when(repo.findOne("the_id")).thenReturn(makeSIEntity());
		assertThat(instanceManager.getCopyIdForInstance("the_id"),
				is(equalTo("the_copy")));
	}

	@Test
	public void itReturnsNullForACopyIdOnANonExistantService() {
		when(repo.findOne("the_id")).thenReturn(null);
		assertNull(instanceManager.getCopyIdForInstance("the_id"));
	}

	@Test
	public void itShouldReturnNullToDeleteNonExistantInstance() {
		assertNull(instanceManager.removeInstance("garbage"));
	}

	@Test
	public void itShouldDeleteAnInstanceAndReturnIt() {
		when(repo.findOne("the_id")).thenReturn(makeSIEntity());
		assertNotNull(instanceManager.removeInstance("the_id"));
		verify(repo).delete("the_id");
	}

	private List<ServiceInstanceEntity> makeEntities(int size) {
		List<ServiceInstanceEntity> entities = new ArrayList<ServiceInstanceEntity>();
		IntStream.range(1, size + 1).forEach(i -> entities.add(makeSIEntity()));
		return entities;
	}

	/**
	 * Create a service instance with "the_id" as the service instance id and
	 * "the_copy" as the copy id
	 * 
	 * @return the ServiceInstanceEntity
	 */
	private ServiceInstanceEntity makeSIEntity() {
		return new ServiceInstanceEntity(new ServiceInstance(
				new CreateServiceInstanceRequest(null, null, null, null)
						.withServiceInstanceId("the_id")), "the_copy");
	}

	private Matcher<ServiceInstanceEntity> hasEntityWithId(String id) {

		Matcher<ServiceInstanceEntity> matches = new BaseMatcher<ServiceInstanceEntity>() {
			@Override
			public boolean matches(Object item) {
				return ((ServiceInstanceEntity) item).getId().equals(id);
			}

			@Override
			public void describeTo(Description description) {
			}

		};
		return matches;
	}
}
