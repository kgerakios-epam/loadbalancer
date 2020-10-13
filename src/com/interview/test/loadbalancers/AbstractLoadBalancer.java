package com.interview.test.loadbalancers;

import com.interview.test.providers.IProvider;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

abstract class AbstractLoadBalancer
{
    public static final int MAX_CONSECUTIVE_SUCCESSFUL_HEALTH_CHECKS = 2;
    public static final int DEFAULT_MAX_CONCURRENT_REQUESTS = 1000;

    private static int MAX_PROVIDER_LIST_SIZE = 10;
    private volatile int size;
    private List<IProvider> providerList;
    private Map<String,Integer> uidToProviderIndex = new HashMap<>();
    private Map<String,Integer> uidToHealthCheckCount = new HashMap<>();
    private AtomicInteger numberOfConcurrentRequests = new AtomicInteger();
    private Integer maxNumberOfConcurrentRequests;
    private Integer heartbeatIntervalInSeconds;
    private ReentrantReadWriteLock.ReadLock readLock;
    private ReentrantReadWriteLock.WriteLock writeLock;

    public AbstractLoadBalancer(List<IProvider> providerList, LoadBalancerConfiguration configuration)
    {
        ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
        readLock = reentrantReadWriteLock.readLock();
        writeLock = reentrantReadWriteLock.writeLock();

        this.heartbeatIntervalInSeconds = configuration.getHeartbeatIntervalInSeconds();
        int maxNumberOfProviders =
                Optional.ofNullable(configuration.getMaxNumberOfProviders()).orElse(MAX_PROVIDER_LIST_SIZE);

        maxNumberOfConcurrentRequests = Optional.ofNullable(configuration.getMaxNumberOfConcurrentRequests())
                                                .orElse(DEFAULT_MAX_CONCURRENT_REQUESTS);

        if (providerList.size() > maxNumberOfProviders)
        {
            throw new IllegalArgumentException("Exceeded Maximum Number of Providers");
        }
        this.providerList = providerList;
        size = providerList.size();

        for (int i = 0; i <size; i++)
        {
            final String id = providerList.get(i).get();
            uidToProviderIndex.put(id, i);
            uidToHealthCheckCount.put(id,0);
        }

        if (heartbeatIntervalInSeconds!= null)
        {
            startHeartbeatThread(providerList);
        }
    }

    protected int getSize()
    {
        return size;
    }

    protected abstract int getNext();

    private void swap(int i,int j)
    {
        final IProvider providerI = providerList.get(i);
        final IProvider providerJ = providerList.get(j);
        providerList.set(j,providerI);
        providerList.set(i, providerJ);
        uidToProviderIndex.put(providerI.get(),j);
        uidToProviderIndex.put(providerJ.get(),i);
    }

    public String get()
    {
        String id= LoadBalancer.ERROR;

        readLock.lock();

        int numOfAliveProviders = size;
        final int requests = numberOfConcurrentRequests.incrementAndGet();

        if (requests <= maxNumberOfConcurrentRequests*numOfAliveProviders)
        {
            int i =  getNext();
            id = providerList.get(i).get();
        }

        numberOfConcurrentRequests.decrementAndGet();
        readLock.unlock();

        return id;
    }

    public void removeProvider(String id)
    {
        writeLock.lock();
        final Integer i = uidToProviderIndex.get(id);
        if (i != null && i < size)
        {
            swap(i,size-1);
            size--;
        }
        writeLock.unlock();
    }

    public void addProvider(String id)
    {
        writeLock.lock();
        final Integer i = uidToProviderIndex.get(id);
        if (i != null && i >= size)
        {
            swap(i,size);
            size++;
        }
        writeLock.unlock();
    }

    private void startHeartbeatThread(final List<IProvider> providerList)
    {
        Runnable healthCheck = () -> performHealthChecks(providerList, heartbeatIntervalInSeconds);
        Thread healthCheckThread = new Thread(healthCheck);
        healthCheckThread.setDaemon(true);
        healthCheckThread.start();
    }

    private void performHealthChecks(final List<IProvider> providerList, final int checkTimeInSeconds)
    {
        while (true)
        {
            for (IProvider provider : providerList)
            {
                checkProvider(provider);
            }

            try
            {
                Thread.sleep(1000L * checkTimeInSeconds);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void checkProvider(final IProvider provider)
    {
        final boolean check = provider.check();
        final String id = provider.get();
        Integer count = uidToHealthCheckCount.get(id);
        int newCount = check? count+1:count-1;

        newCount = Math.min(Math.max(0,newCount), MAX_CONSECUTIVE_SUCCESSFUL_HEALTH_CHECKS);

        System.out.println("Checking " + provider.get() + (check ? " healthy" : " unhealthy") + ", new counter " + newCount);

        if (!check)
        {
            removeProvider(id);
        }
        else if (newCount == MAX_CONSECUTIVE_SUCCESSFUL_HEALTH_CHECKS)
        {
            newCount = 0;
            addProvider(id);
        }
        uidToHealthCheckCount.put(id,newCount);
    }

}
