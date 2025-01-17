package io.smallrye.faulttolerance.reuse.sync.threadoffload.typedguard;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.TypedGuard;

@ApplicationScoped
public class MyFaultTolerance {
    @Produces
    @Identifier("my-fault-tolerance")
    public static final TypedGuard<String> GUARD = TypedGuard.create(String.class)
            .withThreadOffload(true)
            .build();
}
