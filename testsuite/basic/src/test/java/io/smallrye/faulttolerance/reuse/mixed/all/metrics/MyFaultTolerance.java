package io.smallrye.faulttolerance.reuse.mixed.all.metrics;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.Guard;

@ApplicationScoped
public class MyFaultTolerance {
    @Produces
    @Identifier("my-fault-tolerance")
    public static final Guard GUARD = Guard.create()
            .withRetry().maxRetries(5).done()
            .build();
}
