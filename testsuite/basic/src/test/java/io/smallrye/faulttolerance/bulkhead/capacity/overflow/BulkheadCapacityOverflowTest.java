package io.smallrye.faulttolerance.bulkhead.capacity.overflow;

import jakarta.enterprise.inject.spi.DefinitionException;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.ExpectedDeploymentException;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@ExpectedDeploymentException(DefinitionException.class)
public class BulkheadCapacityOverflowTest {
    @Test
    public void test(MyService ignored) {
    }
}