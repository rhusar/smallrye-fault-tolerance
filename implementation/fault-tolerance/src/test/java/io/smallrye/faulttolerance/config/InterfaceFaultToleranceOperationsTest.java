package io.smallrye.faulttolerance.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.autoconfig.FaultToleranceMethod;

public class InterfaceFaultToleranceOperationsTest {
    @Test
    public void testInterfaceMethods() throws NoSuchMethodException, SecurityException {
        FaultToleranceMethod pingMethod = FaultToleranceMethods.create(Proxy.class, Proxy.class.getMethod("ping"));
        FaultToleranceOperation ping = new FaultToleranceOperation(pingMethod);
        assertThat(ping.isValid()).isTrue();
        CircuitBreaker circuitBreaker = ping.getCircuitBreaker();
        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.requestVolumeThreshold()).isEqualTo(2);

        FaultToleranceMethod pongMethod = FaultToleranceMethods.create(Proxy.class, Proxy.class.getMethod("pong"));
        FaultToleranceOperation pong = new FaultToleranceOperation(pongMethod);
        assertThat(pong.isValid()).isTrue();
        Retry retry = pong.getRetry();
        assertThat(retry).isNotNull();
        assertThat(pong.hasAsynchronous()).isFalse();
        assertThat(retry.delay()).isEqualTo(1000);
        circuitBreaker = pong.getCircuitBreaker();
        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.requestVolumeThreshold()).isEqualTo(2);
    }

    @CircuitBreaker(requestVolumeThreshold = 2)
    interface Proxy {
        void ping();

        @Retry(delay = 1000)
        int pong();
    }
}
