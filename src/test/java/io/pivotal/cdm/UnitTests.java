package io.pivotal.cdm;

import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;

import com.googlecode.junittoolbox.*;

@RunWith(WildcardPatternSuite.class)
@SuiteClasses("**/*Test.class")
@ExcludeCategories(IntegrationTest.class)
public class UnitTests {

}
