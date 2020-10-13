package com.interview.test.loadbalancers;

public interface LoadBalancer
{
    String ERROR = "ERROR";
    String get();
    void removeProvider(String providerId);
    void addProvider(String providerId);
}
