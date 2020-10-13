package com.interview.test.healthcheck.mock;

import java.util.Random;

public class ProviderFailureRandomGenerator
{
    private Random random = new Random();
    private int failureProbability;

    public ProviderFailureRandomGenerator(double failureProbability)
    {
        this.failureProbability = (int) (100* failureProbability);
    }
    public boolean check()
    {
        return random.nextInt(100) > failureProbability;
    }
}
