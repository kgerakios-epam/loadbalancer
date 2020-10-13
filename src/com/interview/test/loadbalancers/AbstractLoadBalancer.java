package com.interview.test.loadbalancers;

import com.interview.test.providers.IProvider;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The current implementation uses an arraylist to partition the healthy (left part) and unhealthy (right part)
 * providers. When a provider changes state it is swapped in the list so that the property described above can hold.
 * The get operations are non-blocking between callers except when some partitioning operation has started (which
 * happens periodically when health checks are performed).
 */
abstract class AbstractLoadBalancer
{
    public static final int MAX_CONSECUTIVE_SUCCESSFUL_HEALTH_CHECKS = 2;
    public static final int DEFAULT_MAX_CONCURRENT_REQUESTS = 1000;
    private static final int MAX_PROVIDER_LIST_SIZE = 10;

    private volatile int size;

    /**
     * The list is partitioned to two parts. The left part is used for healthy providers and the right part
     * for the unhealthy ones. Initially all are healthy. The size is used as partition index.
     */
    private List<IProvider> providerList;

    /**
     * Provider id to index in the providers list
     */
    private Map<String, Integer> uidToProviderIndex = new HashMap<>();

    /**
     * Provider id to health check counts
     */
    private Map<String, Integer> uidToHealthCheckCount = new HashMap<>();

    /**
     * Counter for the number of concurrent requests.
     */
    private AtomicInteger numberOfConcurrentRequests = new AtomicInteger();

    /**
     * Maximum number of concurrent requests. This is configurable on creating the load balancer.
     * If not set the maximum default value MAX_PROVIDER_LIST_SIZE is used instead.
     */
    private Integer maxNumberOfConcurrentRequests;

    /**
     * Number of seconds between each health check
     */
    private Integer heartbeatIntervalInSeconds;

    /**
     * Lock needed to protect the get() method from changes made when re-partitioning the providers list
     * due to health checks. When there are no health checks taking place the callers of the get operation of the
     * load balancer can perform get operations concurrently.
     */
    private ReentrantReadWriteLock.ReadLock readLock;

    /**
     * Lock needed to protect block callers of the get operation of the Load balancer while updating the healthy
     * and unhealthy providers
     */
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

        for (int i = 0; i < size; i++)
        {
            final String id = providerList.get(i).get();
            uidToProviderIndex.put(id, i);
            uidToHealthCheckCount.put(id, 0);
        }

        if (heartbeatIntervalInSeconds != null)
        {
            startHeartbeatThread(providerList);
        }
    }

    protected int getSize()
    {
        return size;
    }

    protected abstract int getNext();

    private void swap(int i, int j)
    {
        final IProvider providerI = providerList.get(i);
        final IProvider providerJ = providerList.get(j);
        providerList.set(j, providerI);
        providerList.set(i, providerJ);
        uidToProviderIndex.put(providerI.get(), j);
        uidToProviderIndex.put(providerJ.get(), i);
    }

    /**
     * Get the next healthy load balancer and if the number of concurrent requests is greater than the
     * maximum number of concurrent request times the number of healthy providers an error message is returned.
     * Another option would be to simply throw an exception.
     *
     * @return
     */
    public String get()
    {
        String id = LoadBalancer.ERROR_MESSAGE;

        try
        {
            readLock.lock();

            int numOfAliveProviders = size;
            final int requests = numberOfConcurrentRequests.incrementAndGet();

            if (requests <= maxNumberOfConcurrentRequests * numOfAliveProviders)
            {
                int i = getNext();
                id = providerList.get(i).get();
            }

            numberOfConcurrentRequests.decrementAndGet();
        }
        finally
        {
            readLock.unlock();
        }

        return id;
    }

    /**
     * Manually remove a provider by id. The method is called also from the health checks that occur periodically
     * every X seconds.
     *
     * @param id
     */
    public void removeProvider(String id)
    {
        try
        {
            writeLock.lock();
            final Integer i = uidToProviderIndex.get(id);
            if (i != null && i < size)
            {
                swap(i, size - 1);
                size--;
            }
        }
        finally
        {
            writeLock.unlock();
        }
    }

    /**
     * Manually ad a provider by id. The method is called also from the health checks that occur periodically
     * every X seconds.
     *
     * @param id
     */
    public void addProvider(String id)
    {
        try
        {
            writeLock.lock();
            final Integer i = uidToProviderIndex.get(id);
            if (i != null && i >= size)
            {
                swap(i, size);
                size++;
            }
        }
        finally
        {
            writeLock.unlock();
        }
    }

    /**
     * Start a separate thread to periodically perform the health checks on the providers
     *
     * @param providerList
     */
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
                // TODO: Log error
            }
        }
    }

    /**
     * Check provider health and update counter and update the provider as healthy or unhealthy as needed,
     * depending on the health check result and the number of consecutive successful health checks in case the provider
     * was previously unhealthy.
     *
     * @param provider
     */
    private void checkProvider(final IProvider provider)
    {
        final boolean check = provider.check();
        final String id = provider.get();
        Integer count = uidToHealthCheckCount.get(id);
        int newCount = check ? count + 1 : count - 1;

        newCount = Math.min(Math.max(0, newCount), MAX_CONSECUTIVE_SUCCESSFUL_HEALTH_CHECKS);

        // TODO : Log health check and counter for given provider
        if (!check)
        {
            removeProvider(id);
        }
        else if (newCount == MAX_CONSECUTIVE_SUCCESSFUL_HEALTH_CHECKS)
        {
            newCount = 0;
            addProvider(id);
        }
        uidToHealthCheckCount.put(id, newCount);
    }

}
