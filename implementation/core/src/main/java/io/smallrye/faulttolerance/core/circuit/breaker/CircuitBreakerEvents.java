package io.smallrye.faulttolerance.core.circuit.breaker;

import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.core.FaultToleranceEvent;

public class CircuitBreakerEvents {
    public enum Result {
        SUCCESS,
        FAILURE,
        PREVENTED,
    }

    public enum StateTransition implements FaultToleranceEvent {
        TO_CLOSED(CircuitBreakerState.CLOSED),
        TO_OPEN(CircuitBreakerState.OPEN),
        TO_HALF_OPEN(CircuitBreakerState.HALF_OPEN),
        ;

        public final CircuitBreakerState targetState;

        StateTransition(CircuitBreakerState targetState) {
            this.targetState = targetState;
        }
    }

    public enum Finished implements FaultToleranceEvent {
        SUCCESS(Result.SUCCESS),
        FAILURE(Result.FAILURE),
        PREVENTED(Result.PREVENTED),
        ;

        public final Result result;

        Finished(Result result) {
            this.result = result;
        }
    }
}
