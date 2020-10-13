package com.interview.test.loadbalancers;

public class LoadBalancerConfiguration
{
    private Integer maxNumberOfProviders;
    private Integer heartbeatIntervalInSeconds;
    private Integer maxNumberOfConcurrentRequests;

    public Integer getMaxNumberOfConcurrentRequests()
    {
        return maxNumberOfConcurrentRequests;
    }

    public void setMaxNumberOfConcurrentRequests(final Integer maxNumberOfConcurrentRequests)
    {
        this.maxNumberOfConcurrentRequests = maxNumberOfConcurrentRequests;
    }

    public Integer getMaxNumberOfProviders()
    {
        return maxNumberOfProviders;
    }

    public void setMaxNumberOfProviders(final int maxNumberOfProviders)
    {
        this.maxNumberOfProviders = maxNumberOfProviders;
    }

    public Integer getHeartbeatIntervalInSeconds()
    {
        return heartbeatIntervalInSeconds;
    }

    public void setHeartbeatIntervalInSeconds(final int heartbeatIntervalInSeconds)
    {
        this.heartbeatIntervalInSeconds = heartbeatIntervalInSeconds;
    }
}
