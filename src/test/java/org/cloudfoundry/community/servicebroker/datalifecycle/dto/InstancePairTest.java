package org.cloudfoundry.community.servicebroker.datalifecycle.dto;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.cloudfoundry.community.servicebroker.datalifecycle.dto.InstancePair;
import org.junit.Test;

public class InstancePairTest {

	@Test
	public void theyAreEqual() {
		assertThat(new InstancePair("source", "copy"),
				is(equalTo(new InstancePair("source", "copy"))));
	}

	@Test
	public void theyAreNotEqual() {
		assertThat(new InstancePair("source", "copy"),
				is(not(equalTo(new InstancePair("source", "c0py")))));
	}
}
