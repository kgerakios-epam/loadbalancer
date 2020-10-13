package com.interview.test.providers;

import java.util.function.Supplier;

public class Provider implements IProvider
{
    private String id;

    /**
     * Used as a proxy of some arbitrary implementation of the check method. It could return random
     * health check values or some constant check value (always true, false).
     */
    private Supplier<Boolean> healthChecker;

    public Provider(String id, Supplier<Boolean> healthChecker)
    {
        this.id = id;
        this.healthChecker = healthChecker;
    }

    @Override
    public String get()
    {
        return id;
    }

    @Override
    public boolean check()
    {
        return healthChecker.get();
    }
}
