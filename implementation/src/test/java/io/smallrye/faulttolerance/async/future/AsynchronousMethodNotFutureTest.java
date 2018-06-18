/*
 * Copyright 2017 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.faulttolerance.async.future;

import javax.enterprise.inject.spi.DefinitionException;

import io.smallrye.faulttolerance.TestArchive;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.ShouldThrowException;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Martin Kouba
 */
@RunWith(Arquillian.class)
public class AsynchronousMethodNotFutureTest {

    @ShouldThrowException(DefinitionException.class)
    @Deployment
    public static JavaArchive createTestArchive() {
        return TestArchive.createBase(AsynchronousMethodNotFutureTest.class)
                .addPackage(AsynchronousMethodNotFutureTest.class.getPackage());
    }

    @Test
    public void testIgnored() {
    }
}