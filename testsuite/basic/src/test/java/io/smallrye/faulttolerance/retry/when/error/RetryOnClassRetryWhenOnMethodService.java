package io.smallrye.faulttolerance.retry.when.error;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.RetryWhen;

@Dependent
@Retry
public class RetryOnClassRetryWhenOnMethodService {
    @RetryWhen
    public void hello() {
        throw new IllegalArgumentException();
    }
}
