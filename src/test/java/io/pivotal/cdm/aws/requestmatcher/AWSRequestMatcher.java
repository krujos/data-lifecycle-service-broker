package io.pivotal.cdm.aws.requestmatcher;

import io.pivotal.cdm.aws.AWSHelperTest;

import java.util.function.Predicate;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.mockito.Matchers;

/**
 * @stolen http://source.coveo.com/2014/10/01/java8-mockito/
 *
 * @param <T>
 *            which becomes your <whatever>Request object from the aws api. You
 *            can then compare against whatever your expected value is.
 * 
 * @see AWSHelperTest#itShouldStartAnEC2InstanceFromAnAMI()
 */
public class AWSRequestMatcher<T> extends BaseMatcher<T> {

	private Predicate<T> matcher;

	public static <T> T awsRqst(Predicate<T> predicate) {
		return Matchers.argThat(new AWSRequestMatcher<T>(predicate));
	}

	private AWSRequestMatcher(Predicate<T> matcher) {
		this.matcher = matcher;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean matches(Object item) {
		return matcher.test((T) item);
	}

	@Override
	public void describeTo(Description description) {

	}

}