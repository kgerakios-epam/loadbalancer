package com.interview.test.loadbalancers;

import com.interview.test.providers.IProvider;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

final class RoundRobinLoadBalancer extends AbstractLoadBalancer implements LoadBalancer
{
    private AtomicInteger counter = new AtomicInteger(0);

    public RoundRobinLoadBalancer(final List<IProvider> providerList, final LoadBalancerConfiguration configuration)
    {
        super(providerList, configuration);
    }

    @Override
    protected int getNext()
    {
        int i = counter.getAndIncrement();
        if (i < 0)
        {
            if (i == Integer.MIN_VALUE)
            {
                i = 0;
            }
            else
            {
                i = Integer.MAX_VALUE + i + 1;
            }
        }
        return i % getSize();
    }
}
