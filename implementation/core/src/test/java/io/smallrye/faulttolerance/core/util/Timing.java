package io.smallrye.faulttolerance.core.util;

public class Timing {
    public static long timed(Action action) throws Exception {
        long start = System.nanoTime();
        action.run();
        long end = System.nanoTime();
        return (end - start) / 1_000_000;
    }
}
