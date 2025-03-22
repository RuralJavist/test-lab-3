package org.example.service
import org.example.exceptions.ElementNotFoundException
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import service.UserAnalyticsService
import service.UserStatusService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Comparator
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserStatusServiceTest {

    @BeforeAll
    fun init() {
        MockitoAnnotations.openMocks(this)
    }

    @Mock
    private lateinit var userAnalyticsService: UserAnalyticsService

    @InjectMocks
    private lateinit var userStatusService: UserStatusService

    @ParameterizedTest(name = "Тестирование метода getUserStatus")
    @CsvSource(value = ["123:Inactive",
                        "456:Active",
                        "789:Highly active",
                        "987:Inactive"], delimiter = ':')
    fun testGetUserStatus(userId: String, result: String) {

        Mockito.`when`(userAnalyticsService.getTotalActivityTime("123")).thenReturn(59L)
        Mockito.`when`(userAnalyticsService.getTotalActivityTime("456")).thenReturn(60L)
        Mockito.`when`(userAnalyticsService.getTotalActivityTime("789")).thenReturn(120L)
        Mockito.`when`(userAnalyticsService.getTotalActivityTime("987")).thenReturn(0L)

        val userStatus = userStatusService.getUserStatus(userId)

        Mockito.verify(userAnalyticsService).getTotalActivityTime(userId)
        assertEquals(result, userStatus, "Incorrect user status, expect - $result, given - $userStatus")
    }

    @ParameterizedTest(name = "Тестирование метода getUserStatus")
    @ValueSource(strings = ["123", "789", "456"])
    fun testGetUserLastSessionDate(userId: String) {
        val localDate = LocalDateTime.now()
        val dateTimeLogin1 = LocalDateTime.of(2020, 1, 1, 12, 0)
        val dateTimeLogin2 = localDate.minusDays(1)
        val dateTimeLogin3 = localDate.minusHours(10)

        val correctSessions: MutableList<UserAnalyticsService.Session> = mutableListOf(
            UserAnalyticsService.Session(dateTimeLogin2, dateTimeLogin2.plusHours(17)),
            UserAnalyticsService.Session(dateTimeLogin1, dateTimeLogin1.plusDays(1)),
            UserAnalyticsService.Session(dateTimeLogin3, dateTimeLogin3.plusHours(5))
        )

        val maxLogoutForDateTimes = correctSessions.stream().max(Comparator.comparing(UserAnalyticsService.Session::getLogoutTime)).get()
        val maxLogoutForDateTimeFormated = maxLogoutForDateTimes.logoutTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        Mockito.doThrow(ElementNotFoundException::class.java).`when`(userAnalyticsService).getUserSessions("123")
        Mockito.`when`(userAnalyticsService.getUserSessions("456")).thenReturn(correctSessions)
        Mockito.`when`(userAnalyticsService.getUserSessions("789")).thenReturn(emptyList())

        val getMaxLogoutSession = { userStatusService.getUserLastSessionDate(userId) }

        when (userId) {
            "123" -> assertThrows(ElementNotFoundException::class.java) { getMaxLogoutSession() }
            "789" -> assertTrue(getMaxLogoutSession().isEmpty , "Extend empty optional")
            else -> {
                val maxLogoutTime = getMaxLogoutSession().get()
                assertEquals(maxLogoutTime, maxLogoutForDateTimeFormated,
                    "Incorrect result, expect - $maxLogoutForDateTimeFormated, given - $maxLogoutTime")
            }
        }
        Mockito.verify(userAnalyticsService).getUserSessions(userId)
    }
}
