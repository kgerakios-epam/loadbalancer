package com.interview.test.test;

import com.interview.test.loadbalancers.*;
import com.interview.test.providers.IProvider;
import com.interview.test.providers.Provider;
import com.interview.test.healthcheck.mock.ProviderFailureRandomGenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RandomLoadBalancingTest
{
    public static final double FAILURE_PROBABILITY = 0.3;

    public static void main(String[] args)
    {
        testRandomWithNoHeartbeatLogic();
        testRandom();
    }

    private static void testRandom()
    {
        final LoadBalancerConfiguration loadBalancerConfiguration = new LoadBalancerConfiguration();
        loadBalancerConfiguration.setHeartbeatIntervalInSeconds(1);

        int numberOfTests = 5000;
        int size = 7;

        List<IProvider> providerList = getProviders(size);
        LoadBalancer loadBalancer = LoadBalancerBuilder
                .createLoadBalancer(providerList, loadBalancerConfiguration, LoadBalancerType.Random);

        for (int i = 0; i < numberOfTests; i++)
        {
            loadBalancer.get();
        }

    }

    private static void testRandomWithNoHeartbeatLogic()
    {
        final LoadBalancerConfiguration loadBalancerConfiguration = new LoadBalancerConfiguration();

        testExceedingNumberOfProviders(loadBalancerConfiguration);

        int size = 7;
        int numberOfTests = 100;
        List<IProvider> providerList = getProviders(size);
        LoadBalancer loadBalancer = LoadBalancerBuilder
                .createLoadBalancer(providerList, loadBalancerConfiguration, LoadBalancerType.Random);

        for (int i = 0; i < numberOfTests; i++)
        {
            final String id = loadBalancer.get();
            // could count frequencies per id and check that
            // that are within reasonable range
        }

        checkRemove(numberOfTests, loadBalancer);
        Set<String> providersChecked = checkAddProvider(numberOfTests, loadBalancer);
        checkNoHealthyProvidersError(loadBalancer, providersChecked);

        final String any = providersChecked.stream().findAny().get();
        loadBalancer.addProvider(any);
        loadBalancer.addProvider(any);

        for (int i = 1; i < numberOfTests; i++)
        {
            if (!loadBalancer.get().equals(any))
            {
                throw new RuntimeException("Random Load Balancer not Working");
            }
        }

        System.out.println("Random Load Balancer : Basic Tests Pasted");
    }

    private static void checkNoHealthyProvidersError(final LoadBalancer loadBalancer,
                                                     final Set<String> providersChecked)
    {
        for (String id : providersChecked)
        {
            loadBalancer.removeProvider(id);
        }

        if (!loadBalancer.get().equals(LoadBalancer.ERROR))
        {
            throw new RuntimeException("Random Load Balancer not Working");
        }
    }

    private static Set<String> checkAddProvider(final int numberOfTests, final LoadBalancer loadBalancer)
    {
        loadBalancer.addProvider("Provider-1");
        Set<String> providersChecked = new HashSet<>();
        for (int i = 0; i < numberOfTests; i++)
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

    private static void checkRemove(final int numberOfTests, final LoadBalancer loadBalancer)
    {
        loadBalancer.removeProvider("Provider-1");
        for (int i = 0; i < numberOfTests; i++)
        {
            final String id = loadBalancer.get();
            if (id.equals("Provider-1"))
            {
                throw new RuntimeException("Random Load Balancer not Working");
            }
        }
    }

    private static void testExceedingNumberOfProviders(final LoadBalancerConfiguration loadBalancerConfiguration)
    {
        try
        {
            LoadBalancerBuilder
                    .createLoadBalancer(new ArrayList<>(11), loadBalancerConfiguration, LoadBalancerType.Random);
            throw new RuntimeException("Providers list size check not working");
        }
        catch (final Exception e)
        {
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
