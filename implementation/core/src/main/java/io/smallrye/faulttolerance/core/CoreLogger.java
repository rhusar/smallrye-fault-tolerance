package io.smallrye.faulttolerance.core;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRFTL", length = 5)
interface CoreLogger extends BasicLogger {
    CoreLogger LOG = Logger.getMessageLogger(MethodHandles.lookup(), CoreLogger.class, CoreLogger.class.getPackage().getName());
}
