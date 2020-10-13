package com.interview.test.test;

import com.interview.test.loadbalancers.*;
import com.interview.test.providers.IProvider;
import com.interview.test.providers.Provider;
import com.interview.test.healthcheck.mock.ProviderFailureRandomGenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RoundRobinLoadBalancingTest
{
    public static final double FAILURE_PROBABILITY = 0.3;

    public static void main(String[] args)
    {
        testRoundRobinWithNoHeartbeatLogic();
        testRoundRobinWithHeartbeatLogic();
    }

    private static void testRoundRobinWithHeartbeatLogic()
    {
        final LoadBalancerConfiguration loadBalancerConfiguration = new LoadBalancerConfiguration();
        loadBalancerConfiguration.setHeartbeatIntervalInSeconds(1);

        int numOfIterations = 5000000;
        int size = 7;
        List<IProvider> providerList = getProviders(size);
        LoadBalancer loadBalancer = LoadBalancerBuilder
                .createLoadBalancer(providerList, loadBalancerConfiguration, LoadBalancerType.RoundRobin);

        for (int i = 0; i < numOfIterations; i++)
        {
            final String id = loadBalancer.get();
        }
    }

    private static void testRoundRobinWithNoHeartbeatLogic()
    {
        final LoadBalancerConfiguration loadBalancerConfiguration = new LoadBalancerConfiguration();

        checkExceedingMaximumNumberOfProviders(loadBalancerConfiguration);

        int numberOfProviders = 7;
        int numOfIterations = 50000000;
        List<IProvider> providerList = getProviders(numberOfProviders);
        LoadBalancer loadBalancer = LoadBalancerBuilder
                .createLoadBalancer(providerList, loadBalancerConfiguration, LoadBalancerType.RoundRobin);

        testRoundRobinSequenceOfExecutedProviders(numberOfProviders, numOfIterations, loadBalancer);
        checkRemoveProvider(numOfIterations, loadBalancer);
        Set<String> providersChecked = checkAddProvider(numOfIterations, loadBalancer);
        checkAllUnhealthyProvidersError(loadBalancer, providersChecked);
        checkAddAfterRemove(numOfIterations, loadBalancer, providersChecked);

        System.out.println("Round Robin : Basic Tests Pasted");
    }

    private static void checkAddAfterRemove(final int numOfIterations, final LoadBalancer loadBalancer,
                                            final Set<String> providersChecked)
    {
        final String any = providersChecked.stream().findAny().get();
        loadBalancer.addProvider(any);
        loadBalancer.addProvider(any);

        for (int i = 1; i < numOfIterations; i++)
        {
            if (!loadBalancer.get().equals(any))
            {
                throw new RuntimeException("Random Load Balancer not Working");
            }
        }
    }

    private static void checkAllUnhealthyProvidersError(final LoadBalancer loadBalancer,
                                                        final Set<String> providersChecked)
    {
        for (String id : providersChecked)
        {
            loadBalancer.removeProvider(id);
        }

        if (!loadBalancer.get().equals(LoadBalancer.ERROR_MESSAGE))
        {
            throw new RuntimeException("Random Load Balancer not Working");
        }
    }

    private static Set<String> checkAddProvider(final int numOfIterations, final LoadBalancer loadBalancer)
    {
        loadBalancer.addProvider("Provider-1");
        Set<String> providersChecked = new HashSet<>();
        for (int i = 0; i < numOfIterations; i++)
        {
            final String id = loadBalancer.get();
            providersChecked.add(id);
        }

        if (!providersChecked.contains("Provider-1"))
        {
            throw new RuntimeException("Random Load Balancer not Working");
        }
        return providersChecked;
    }

    private static void checkRemoveProvider(final int numOfIterations, final LoadBalancer loadBalancer)
    {
        loadBalancer.removeProvider("Provider-1");
        for (int i = 0; i < numOfIterations; i++)
        {
            final String id = loadBalancer.get();
            if (id.equals("Provider-1"))
            {
                throw new RuntimeException("Random Load Balancer not Working");
            }
        }
    }

    private static void checkExceedingMaximumNumberOfProviders(
            final LoadBalancerConfiguration loadBalancerConfiguration)
    {
        try
        {
            LoadBalancer loadBalancer = LoadBalancerBuilder
                    .createLoadBalancer(new ArrayList<>(11), loadBalancerConfiguration, LoadBalancerType.RoundRobin);
            throw new RuntimeException("Providers list size check not working");
        }
        catch (final Exception e)
        {
        }
    }

    private static void testRoundRobinSequenceOfExecutedProviders(final int size, final int numOfIterations,
                                                                  final LoadBalancer loadBalancer)
    {
        for (int i = 0; i < numOfIterations; i++)
        {
            final String id = loadBalancer.get();
            if (!id.endsWith("-" + (i % size + 1)))
            {
                throw new RuntimeException("Round Robin is not working");
            }
        }
    }

    private static List<IProvider> getProviders(final int size)
    {
        ProviderFailureRandomGenerator generator = new ProviderFailureRandomGenerator(FAILURE_PROBABILITY);
        List<IProvider> providerList = new ArrayList<>();
        for (int i = 1; i <= size; i++)
        {
            providerList.add(new Provider("Provider-" + i, () -> generator.check()));
        }
        return providerList;
    }
}
