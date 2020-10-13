package com.interview.test.loadbalancers;

import com.interview.test.providers.IProvider;

import java.util.List;

public class LoadBalancerBuilder
{
    public static LoadBalancer createLoadBalancer(List<IProvider> providers, LoadBalancerConfiguration configuration,
                                                  LoadBalancerType type)
    {
        LoadBalancer loadBalancer = null;
        if (type == LoadBalancerType.Random)
        {
            loadBalancer = new RandomLoadBalancer(providers, configuration);
        }
        else if (type == LoadBalancerType.RoundRobin)
        {
            loadBalancer = new RoundRobinLoadBalancer(providers,configuration);
        }

        if (loadBalancer != null)
        {
            return loadBalancer;
        }
        throw new UnsupportedOperationException("Unsupported loadbalancer type");
    }
}
