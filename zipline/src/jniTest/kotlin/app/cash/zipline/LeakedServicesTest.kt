/*
 * Copyright (C) 2022 Block, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.zipline

import app.cash.zipline.testing.EchoRequest
import app.cash.zipline.testing.EchoResponse
import app.cash.zipline.testing.EchoService
import app.cash.zipline.testing.LoggingEventListener
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test

class LeakedServicesTest {
  private val eventListener = LoggingEventListener()
  private val dispatcher = TestCoroutineDispatcher()
  private val zipline = Zipline.create(dispatcher, eventListener = eventListener)
  private val uncaughtExceptionHandler = TestUncaughtExceptionHandler()

  @Before fun setUp() {
    zipline.loadTestingJs()
    uncaughtExceptionHandler.setUp()
  }

  @After fun tearDown() {
    zipline.close()
    uncaughtExceptionHandler.tearDown()
  }

  @Test fun jvmLeaksService() = runBlocking {
    zipline.quickJs.evaluate("testing.app.cash.zipline.testing.prepareJsBridges()")
    allocateAndLeakService()
    awaitGarbageCollection()
    triggerJvmLeakDetection()
    val name = "helloService"
    assertThat(eventListener.take()).isEqualTo("takeService $name")
    assertThat(eventListener.take()).isEqualTo("serviceLeaked $name")
  }

  @Test fun jsLeaksService() = runBlocking {
    val supService = object : EchoService {
      override fun echo(request: EchoRequest): EchoResponse = error("unexpected call")
    }

    val name = "supService"
    zipline.bind<EchoService>(name, supService)
    zipline.quickJs.evaluate("testing.app.cash.zipline.testing.allocateAndLeakService()")
    zipline.quickJs.evaluate("testing.app.cash.zipline.testing.triggerLeakDetection()")
    assertThat(eventListener.take()).isEqualTo("bindService $name")
    assertThat(eventListener.take()).isEqualTo("serviceLeaked $name")
  }

  /** Use a separate method so there's no hidden reference remaining on the stack. */
  private fun allocateAndLeakService() {
    zipline.take<EchoService>("helloService") // Deliberately not closed for testing.
  }

  /** Just attempting to take a service causes Zipline to process leaked services. */
  private fun triggerJvmLeakDetection() {
    zipline.take<EchoService>("noSuchService")
  }
}
