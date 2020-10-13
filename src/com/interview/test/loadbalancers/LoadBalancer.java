package com.interview.test.loadbalancers;

public interface LoadBalancer
{
    String ERROR_MESSAGE = "Service Unavailable";

    String get();

    void removeProvider(String providerId);

    void addProvider(String providerId);
}
