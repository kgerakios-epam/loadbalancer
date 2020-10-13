package com.interview.test.loadbalancers;

import com.interview.test.providers.IProvider;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The next index in the array of the providers is calculated using a counter and modulo operation on the number
 * of healthy providers. The abstract superclass partition the healthy (left part of the list) and unhealthy
 * (right part of the list) and the partition index is the current value of the size attribute.
 *
 */
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
            i = (Integer.MAX_VALUE + i) + 1;
        }
        return i % getSize();
    }
}
