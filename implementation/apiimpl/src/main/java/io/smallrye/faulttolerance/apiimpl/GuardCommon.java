package io.smallrye.faulttolerance.apiimpl;

import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Predicate;

import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.invocation.AsyncSupport;
import io.smallrye.faulttolerance.core.invocation.AsyncSupportRegistry;
import io.smallrye.faulttolerance.core.invocation.Invoker;
import io.smallrye.faulttolerance.core.invocation.StrategyInvoker;
import io.smallrye.faulttolerance.core.util.ExceptionDecision;
import io.smallrye.faulttolerance.core.util.PredicateBasedExceptionDecision;
import io.smallrye.faulttolerance.core.util.PredicateBasedResultDecision;
import io.smallrye.faulttolerance.core.util.ResultDecision;
import io.smallrye.faulttolerance.core.util.SetBasedExceptionDecision;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

final class GuardCommon {
    private static final Class<?>[] NO_PARAMS = new Class<?>[0];

    // V = value type, e.g. String
    // T = result type, e.g. String or CompletionStage<String> or Uni<String>
    //
    // in synchronous scenario, V = T
    // in asynchronous scenario, T is an async type that eventually produces V
    static <V, T> AsyncSupport<V, T> asyncSupport(Type type) {
        if (type instanceof Class<?>) {
            return AsyncSupportRegistry.get(NO_PARAMS, (Class<?>) type);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            return AsyncSupportRegistry.get(NO_PARAMS, rawType);
        } else {
            return null;
        }
    }

    // V = value type, e.g. String
    // T = result type, e.g. String or CompletionStage<String> or Uni<String>
    //
    // in synchronous scenario, V = T
    // in asynchronous scenario, T is an async type that eventually produces V
    static <V, T> AsyncInvocation<V, T> asyncInvocation(Callable<T> action, AsyncSupport<V, T> asyncSupport) {
        return asyncSupport != null ? new AsyncInvocation<>(asyncSupport, new CallableInvoker<>(action), null) : null;
    }

    // V = value type, e.g. String
    // T = result type, e.g. String or CompletionStage<String> or Uni<String>
    //
    // in synchronous scenario, V = T
    // in asynchronous scenario, T is an async type that eventually produces V
    static <V, T> T guard(Callable<T> action, FaultToleranceStrategy<V> strategy, AsyncInvocation<V, T> asyncInvocation,
            EventHandlers eventHandlers, Consumer<FaultToleranceContext<?>> contextModifier) throws Exception {
        if (asyncInvocation == null) {
            FaultToleranceContext<T> ctx = new FaultToleranceContext<>(() -> Future.from(action), false);
            if (contextModifier != null) {
                contextModifier.accept(ctx);
            }
            eventHandlers.register(ctx);
            try {
                FaultToleranceStrategy<T> castStrategy = (FaultToleranceStrategy<T>) strategy;
                return castStrategy.apply(ctx).awaitBlocking();
            } catch (Exception e) {
                throw e;
            } catch (Throwable e) {
                throw sneakyThrow(e);
            }
        }

        AsyncSupport<V, T> asyncSupport = asyncInvocation.asyncSupport;
        Invoker<T> toFutureInvoker = asyncInvocation.toFutureInvoker;
        FaultToleranceContext<V> ctx = new FaultToleranceContext<>(() -> asyncSupport.toFuture(toFutureInvoker), true);
        if (contextModifier != null) {
            contextModifier.accept(ctx);
        }
        eventHandlers.register(ctx);
        Invoker<Future<V>> fromFutureInvoker = new StrategyInvoker<>(asyncInvocation.arguments, strategy, ctx);
        return asyncSupport.fromFuture(fromFutureInvoker);
    }

    // ---

    static ResultDecision createResultDecision(Predicate<Object> whenResultPredicate) {
        if (whenResultPredicate != null) {
            // the builder API accepts a predicate that returns `true` when a result is considered failure,
            // but `Retry` accepts a predicate that returns `true` when a result is considered success,
            // hence the negation
            return new PredicateBasedResultDecision(whenResultPredicate.negate());
        }
        return ResultDecision.ALWAYS_EXPECTED;
    }

    static ExceptionDecision createExceptionDecision(Class<? extends Throwable>[] consideredExpected,
            Class<? extends Throwable>[] consideredFailure, Predicate<Throwable> whenExceptionPredicate) {
        if (whenExceptionPredicate != null) {
            // the builder API accepts a predicate that returns `true` when an exception is considered failure,
            // but `PredicateBasedExceptionDecision` accepts a predicate that returns `true` when an exception
            // is considered success -- hence the negation
            return new PredicateBasedExceptionDecision(whenExceptionPredicate.negate());
        }
        return new SetBasedExceptionDecision(createSetOfThrowables(consideredFailure),
                createSetOfThrowables(consideredExpected), true);
    }

    private static SetOfThrowables createSetOfThrowables(Class<? extends Throwable>[] throwableClasses) {
        if (throwableClasses == null || throwableClasses.length == 0) {
            return SetOfThrowables.EMPTY;
        }
        return SetOfThrowables.create(throwableClasses);
    }
}
