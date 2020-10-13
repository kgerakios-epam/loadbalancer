package com.interview.test.loadbalancers;

import com.interview.test.providers.IProvider;

import java.util.List;
import java.util.Random;

/**
 * The next index in the array of the providers is calculated using a random value between 0 and the number of healthy
 * providers (exclusive). The abstract superclass partition the healthy (left part of the list) and unhealthy
 * (right part of the list) and the partition index is the current value of the size attribute.
 *
 */
final class RandomLoadBalancer extends AbstractLoadBalancer implements LoadBalancer
{
    private Random random = new Random();

    public RandomLoadBalancer(final List<IProvider> providerList, LoadBalancerConfiguration configuration)
    {
        super(providerList, configuration);
    }

    @Override
    protected int getNext()
    {
        return random.nextInt(getSize());
    }
}
