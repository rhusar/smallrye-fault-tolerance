package io.smallrye.faulttolerance.core.util;

import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class SetBasedExceptionDecision implements ExceptionDecision {
    // @CircuitBreaker.failOn, @Fallback.applyOn, @Retry.retryOn
    private final SetOfThrowables consideredFailure;
    // @CircuitBreaker.skipOn, @Fallback.skipOn, @Retry.abortOn
    private final SetOfThrowables consideredExpected;

    private final boolean inspectCauseChain;

    private final boolean nonDefaultConsideredFailure;
    private final boolean nonDefaultConsideredExpected;

    public SetBasedExceptionDecision(SetOfThrowables consideredFailure, SetOfThrowables consideredExpected,
            boolean inspectCauseChain) {
        this.consideredFailure = checkNotNull(consideredFailure, "Set of considered-failure throwables must be set");
        this.consideredExpected = checkNotNull(consideredExpected, "Set of considered-expected throwables must be set");
        this.inspectCauseChain = inspectCauseChain;
        this.nonDefaultConsideredFailure = !consideredFailure.isAll();
        this.nonDefaultConsideredExpected = !consideredExpected.isEmpty();
    }

    public boolean isConsideredExpected(Throwable e) {
        // per `@CircuitBreaker` javadoc, `skipOn` takes priority over `failOn`
        // per `@Fallback` javadoc, `skipOn` takes priority over `applyOn`
        // per `@Retry` javadoc, `abortOn` takes priority over `retryOn`
        // to sum up, the exceptions considered expected win over those considered failure
        if (inspectCauseChain) {
            return isConsideredExpectedWithCauseChain(e);
        } else {
            return isConsideredExpectedDefault(e);
        }
    }

    private boolean isConsideredExpectedDefault(Throwable e) {
        if (consideredExpected.includes(e.getClass())) {
            return true;
        }
        if (consideredFailure.includes(e.getClass())) {
            return false;
        }
        return true;
    }

    private boolean isConsideredExpectedWithCauseChain(Throwable e) {
        if (nonDefaultConsideredExpected && consideredExpected.includes(e.getClass())) {
            return true;
        }
        if (nonDefaultConsideredFailure && consideredFailure.includes(e.getClass())) {
            return false;
        }

        if (includes(consideredExpected, e)) {
            return true;
        }
        if (includes(consideredFailure, e)) {
            return false;
        }

        return true;
    }

    private boolean includes(SetOfThrowables set, Throwable e) {
        Set<Throwable> alreadySeen = Collections.newSetFromMap(new IdentityHashMap<>());

        // guard against hypothetical cycle in the cause chain
        while (e != null && !alreadySeen.contains(e)) {
            alreadySeen.add(e);

            if (set.includes(e.getClass())) {
                return true;
            }

            e = e.getCause();
        }

        return false;
    }
}
