package com.asfoundation.wallet.app_start

import app.cash.turbine.testIn
import com.asfoundation.wallet.gherkin.coScenario
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.TimeUnit

/**
 * AS a Wallet Developer,
 * I WANT to track modes of app start,
 * FOR analysing newcomers and altering the first screen
 *
 * Since it is impossible to know if app data was cleared or not we assume that if it is not a fresh
 * install then this is not the first run.
 * So the first run event will occur only if app was not updated yet and run count is 0
 */

@ExperimentalCoroutinesApi
internal class AppStartUseCaseTest {

  @ParameterizedTest(name = "{0}")
  @MethodSource("testDataProvider")
  fun `Listen before job started`(data: TestData) = coScenario { scope ->

    m Given "use case with the given repository, FirstUtmUseCase and FirstTopAppUseCase mocks"
    val useCase = AppStartUseCase(
      data.givenData.repository,
      data.givenData.firstUtmUseCase,
      data.givenData.firstTopAppUseCase,
      StandardTestDispatcher(scope.testScheduler)
    )
    m And "subscribed for modes"
    val modes = useCase.startModes.testIn(scope)

    m When "app started"
    useCase.registerAppStart()
    m And "job finished"
    scope.advanceUntilIdle()

    m Then "run mode = expected mode from ThenData"
    assertEquals(data.thenData.mode, modes.awaitItem())
    m And "runs count = expected runs count from ThenData"
    assertEquals(data.thenData.runCount, data.givenData.repository.runCount)
    m But "no more modes received"
    modes.cancel()
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("testDataProvider")
  fun `Listen before job finished`(data: TestData) = coScenario { scope ->

    m Given "use case with the given repository, FirstUtmUseCase and FirstTopAppUseCase mocks"
    val useCase = AppStartUseCase(
      data.givenData.repository,
      data.givenData.firstUtmUseCase,
      data.givenData.firstTopAppUseCase,
      StandardTestDispatcher(scope.testScheduler)
    )

    m When "app started"
    useCase.registerAppStart()
    m And "job running"
    scope.advanceTimeBy(50)
    m And "subscribed for modes"
    val modes = useCase.startModes.testIn(scope)
    m And "job finished"
    scope.advanceUntilIdle()

    m Then "run mode = expected mode from ThenData"
    assertEquals(data.thenData.mode, modes.awaitItem())
    m And "runs count = expected runs count from ThenData"
    assertEquals(data.thenData.runCount, data.givenData.repository.runCount)
    m But "no more modes received"
    modes.cancel()
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("testDataProvider")
  fun `Listen after job finished`(data: TestData) = coScenario { scope ->

    m Given "use case with the given repository, FirstUtmUseCase and FirstTopAppUseCase mocks"
    val useCase = AppStartUseCase(
      data.givenData.repository,
      data.givenData.firstUtmUseCase,
      data.givenData.firstTopAppUseCase,
      StandardTestDispatcher(scope.testScheduler)
    )

    m When "app started"
    useCase.registerAppStart()
    m And "job finished"
    scope.advanceUntilIdle()
    m And "subscribed for modes"
    val modes = useCase.startModes.testIn(scope)

    m Then "run mode = expected mode from ThenData"
    assertEquals(data.thenData.mode, modes.awaitItem())
    m And "runs count = expected runs count from ThenData"
    assertEquals(data.thenData.runCount, data.givenData.repository.runCount)
    m But "no more modes received"
    modes.cancel()
  }

  companion object {
    private fun firstUtm(
      sku: String = "13204",
      source: String = "aptoide",
      packageName: String = "com.igg.android.lordsmobile",
      integration: String = "osp"
    ) = StartMode.FirstUtm(sku, source, packageName, integration)

    private fun firstTopApp(packageName: String = "com.igg.android.lordsmobile") =
      StartMode.FirstTopApp(packageName)

    @JvmStatic
    fun testDataProvider(): List<TestData> = listOf(
      TestData(
        scenario = "App started -> First",
        givenData = GivenData(),
        thenData = ThenData()
      ),
      TestData(
        scenario = "App started with UTM -> FirstUtm",
        givenData = GivenData(firstUtmUseCase = FirstUtmUseCaseMock(firstUtm())),
        thenData = ThenData(mode = firstUtm())
      ),
      TestData(
        scenario = "App started with top app -> FirstTopApp",
        givenData = GivenData(firstTopAppUseCase = FirstTopAppUseCaseMock(firstTopApp())),
        thenData = ThenData(mode = firstTopApp())
      ),
      TestData(
        scenario = "App started with UTM and top app -> FirstUtm",
        givenData = GivenData(
          firstUtmUseCase = FirstUtmUseCaseMock(firstUtm()),
          firstTopAppUseCase = FirstTopAppUseCaseMock(firstTopApp())
        ),
        thenData = ThenData(mode = firstUtm())
      ),
      TestData(
        scenario = "App started after update -> Subsequent",
        givenData = GivenData(repository = AppStartRepositoryMock(updatedAfter = 5.days())),
        thenData = ThenData(mode = StartMode.Subsequent)
      ),
      TestData(
        scenario = "App started after update with UTM -> Subsequent",
        givenData = GivenData(
          repository = AppStartRepositoryMock(updatedAfter = 5.days()),
          firstUtmUseCase = FirstUtmUseCaseMock(firstUtm())
        ),
        thenData = ThenData(mode = StartMode.Subsequent)
      ),
      TestData(
        scenario = "App started after update with top app -> Subsequent",
        givenData = GivenData(
          repository = AppStartRepositoryMock(updatedAfter = 5.days()),
          firstTopAppUseCase = FirstTopAppUseCaseMock(firstTopApp())
        ),
        thenData = ThenData(mode = StartMode.Subsequent)
      ),
      TestData(
        scenario = "App started after update with UTM and top app -> Subsequent",
        givenData = GivenData(
          repository = AppStartRepositoryMock(updatedAfter = 5.days()),
          firstUtmUseCase = FirstUtmUseCaseMock(firstUtm()),
          firstTopAppUseCase = FirstTopAppUseCaseMock(firstTopApp())
        ),
        thenData = ThenData(mode = StartMode.Subsequent)
      ),
      TestData(
        scenario = "App started for the second time -> Subsequent",
        givenData = GivenData(repository = AppStartRepositoryMock(runCount = 1)),
        thenData = ThenData(mode = StartMode.Subsequent, runCount = 2)
      ),
      TestData(
        scenario = "App started for the second time with UTM -> Subsequent",
        givenData = GivenData(
          repository = AppStartRepositoryMock(runCount = 1),
          firstUtmUseCase = FirstUtmUseCaseMock(firstUtm())
        ),
        thenData = ThenData(mode = StartMode.Subsequent, runCount = 2)
      ),
      TestData(
        scenario = "App started for the second time with top app -> Subsequent",
        givenData = GivenData(
          repository = AppStartRepositoryMock(runCount = 1),
          firstTopAppUseCase = FirstTopAppUseCaseMock(firstTopApp())
        ),
        thenData = ThenData(mode = StartMode.Subsequent, runCount = 2)
      ),
      TestData(
        scenario = "App started for the second time with UTM and top app -> Subsequent",
        givenData = GivenData(
          repository = AppStartRepositoryMock(runCount = 1),
          firstUtmUseCase = FirstUtmUseCaseMock(firstUtm()),
          firstTopAppUseCase = FirstTopAppUseCaseMock(firstTopApp())
        ),
        thenData = ThenData(mode = StartMode.Subsequent, runCount = 2)
      ),
      TestData(
        scenario = "App started for the second time after update -> Subsequent",
        givenData = GivenData(
          repository = AppStartRepositoryMock(runCount = 1, updatedAfter = 5.days())
        ),
        thenData = ThenData(mode = StartMode.Subsequent, runCount = 2)
      ),
      TestData(
        scenario = "App started for the second time after update with UTM -> Subsequent",
        givenData = GivenData(
          repository = AppStartRepositoryMock(runCount = 1, updatedAfter = 5.days()),
          firstUtmUseCase = FirstUtmUseCaseMock(firstUtm())
        ),
        thenData = ThenData(mode = StartMode.Subsequent, runCount = 2)
      ),
      TestData(
        scenario = "App started for the second time after update with top app -> Subsequent",
        givenData = GivenData(
          repository = AppStartRepositoryMock(runCount = 1, updatedAfter = 5.days()),
          firstTopAppUseCase = FirstTopAppUseCaseMock(firstTopApp())
        ),
        thenData = ThenData(mode = StartMode.Subsequent, runCount = 2)
      ),
      TestData(
        scenario = "App started for the second time after update with UTM and top app -> Subsequent",
        givenData = GivenData(
          repository = AppStartRepositoryMock(runCount = 1, updatedAfter = 5.days()),
          firstUtmUseCase = FirstUtmUseCaseMock(firstUtm()),
          firstTopAppUseCase = FirstTopAppUseCaseMock(firstTopApp())
        ),
        thenData = ThenData(mode = StartMode.Subsequent, runCount = 2)
      )
    )
  }

  internal data class TestData(
    val scenario: String,
    val givenData: GivenData,
    val thenData: ThenData,
  ) {
    override fun toString() = scenario
  }
  internal data class GivenData(
    val repository: AppStartRepositoryMock = AppStartRepositoryMock(),
    val firstUtmUseCase: FirstUtmUseCase = FirstUtmUseCaseMock(),
    val firstTopAppUseCase: FirstTopAppUseCase = FirstTopAppUseCaseMock(),
  )

  internal data class ThenData(
    val mode: StartMode = StartMode.First,
    val runCount: Int = 1,
  )
}

private fun Number.days() = TimeUnit.DAYS.toMillis(this.toLong())
private fun Number.daysAgo() = System.currentTimeMillis() - this.days()

internal class AppStartRepositoryMock(
  var runCount: Int = 0,
  private val InstallAgo: Long = 15.daysAgo(),
  private val updatedAfter: Long = 0,
) : AppStartRepository {
  override suspend fun getRunCount(): Int {
    delay(100)
    return runCount
  }

  override suspend fun saveRunCount(count: Int) {
    delay(200)
    this.runCount = count
  }

  override suspend fun getFirstInstallTime(): Long {
    delay(100)
    return InstallAgo
  }

  override suspend fun getLastUpdateTime(): Long {
    delay(100)
    return InstallAgo + updatedAfter
  }
}

class FirstUtmUseCaseMock(
  private val firstUtm: StartMode.FirstUtm? = null
) : FirstUtmUseCase {
  override suspend operator fun invoke(): StartMode.FirstUtm? {
    delay(800)
    return firstUtm
  }
}

class FirstTopAppUseCaseMock(
  private val firstUtm: StartMode.FirstTopApp? = null
) : FirstTopAppUseCase {

  override suspend operator fun invoke(): StartMode.FirstTopApp? {
    delay(400)
    return firstUtm
  }
}
