package io.pivotal.cdm.provider;

import java.util.Map;

public interface DataProvider {

    /**
     * Sanitize the data source with the incoming script. This method is
     * called upon bind and typically removes or modifies sensitive data from
     * the database. This method is always called in order to ensure the
     * sanatize point has a chance to perform operations which my not be
     * scripted.
     *
     * Creds are obtained from the CopyProvider after creating a copy, so
     * the DataProvider needs to understand the object, as it's opaque
     * to the framework.
     *
     * @param script to run against the data source, may be null
     */
    void sanitize(String script, Map<String, Object> creds);
}
