package com.interview.test.loadbalancers;

import com.interview.test.providers.IProvider;

import java.util.List;

final class RoundRobinLoadBalancer extends AbstractLoadBalancer implements LoadBalancer
{
    private volatile int counter = 0;

    public RoundRobinLoadBalancer(final List<IProvider> providerList, final LoadBalancerConfiguration configuration)
    {
        super(providerList,configuration);
    }

    @Override
    protected int getNext()
    {
        int i = counter%getSize();
        counter = counter == Integer.MAX_VALUE? 0 : counter+1;
        return i;
    }
}
