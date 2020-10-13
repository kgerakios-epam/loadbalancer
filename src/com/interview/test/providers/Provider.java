package com.interview.test.providers;

import java.util.function.Supplier;

public class Provider implements IProvider
{
    private String id;
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
