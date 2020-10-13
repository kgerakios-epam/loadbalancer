package com.interview.test.run;

import com.interview.test.loadbalancers.*;
import com.interview.test.providers.IProvider;
import com.interview.test.providers.Provider;
import com.interview.test.healthcheck.mock.ProviderFailureRandomGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class LoadBalancerApp
{
    public static final double FAILURE_PROBABILITY = 0.3;

    public static void main(String[] args)
    {
        final LoadBalancerConfiguration loadBalancerConfiguration = new LoadBalancerConfiguration();
        loadBalancerConfiguration.setHeartbeatIntervalInSeconds(1);

        int numberOfTests = 50_000_000;
        int size = 7;

        List<IProvider> providerList = getProviders(size);
        LoadBalancer loadBalancer = LoadBalancerBuilder.createLoadBalancer(providerList,loadBalancerConfiguration,
                LoadBalancerType.RoundRobin);

        IntStream.range(0, numberOfTests).parallel().forEach((i) -> loadBalancer.get());
    }

    private static List<IProvider> getProviders(final int size)
    {
        ProviderFailureRandomGenerator generator = new ProviderFailureRandomGenerator(FAILURE_PROBABILITY);
        List < IProvider > providerList = new ArrayList<>();
        for (int i = 1; i <= size; i++)
        {
            providerList.add(new Provider("Provider-"+i, () -> generator.check()));
        }
        return providerList;
    }
}
