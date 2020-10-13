package com.interview.test.loadbalancers;

import com.interview.test.providers.IProvider;

import java.util.List;
import java.util.Random;

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
