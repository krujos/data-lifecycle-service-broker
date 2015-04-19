package io.pivotal.cdm.provider;

interface DataProvider {

    /**
     * Sanitize the datasource with the incomming script. This method is called upon
     * bind and typically removes or modifies sensitive data from the database. This
     * method is always called in order to ensure the sanatize point has a chance to
     * perform operations which my not be scripted.
     *
     * @param script to run against the data source, may be null
     */
    void sanitize(String script);
}
