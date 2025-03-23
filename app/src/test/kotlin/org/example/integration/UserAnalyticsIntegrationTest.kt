package org.example.integration

import groovy.lang.Tuple3
import io.javalin.Javalin
import io.javalin.http.HttpStatus
import io.restassured.RestAssured
import io.restassured.response.Response
import org.example.controller.UserAnalyticsController.createApp
import org.hamcrest.Matchers
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class UserAnalyticsIntegrationTest {

    private var app: Javalin? = null
    private val port = 7070

    @BeforeAll
    fun setUp() {
        app = createApp()
        app!!.start(port)
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = port
    }

    @AfterAll
    fun tearDown() {
        app!!.stop()
    }

    companion object {

        private val FAKE_ID = Pair("user_id", "777")
        private val correctLoginDateTime1 = LocalDateTime.of(2025, Month.MARCH.value, 7, 11, 30, 0)
        private val correctLogoutDateTime1 = correctLoginDateTime1.plusHours(3)

        private val correctLoginDateTime2 = LocalDateTime.of(2025, Month.MARCH.value, 6, 11, 30, 0)
        private val correctLogoutDateTime2 = correctLoginDateTime1.plusDays(1)

        private val correctLoginDateTime3 = correctLogoutDateTime2.plusHours(3)
        private val correctLogoutDateTime3 = correctLoginDateTime3.plusHours(5)

        private val correctLoginDateTime4 = correctLoginDateTime1.minusYears(1).plusMonths(1)
        private val correctLogoutDateTime4 = correctLoginDateTime4.plusMonths(1)

        private val correctLoginDateTime5 = correctLoginDateTime2.minusDays(1)
        private val correctLogoutDateTime5 = correctLoginDateTime5.plusHours(5)

        private val formater: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        //вложенная сессия
        private val correctLoginDateTimeFormated1 = correctLoginDateTime1.format(formater)
        private val correctLogoutDateTimeFormated1 = correctLogoutDateTime1.format(formater)

        //поглощающая сессия
        private val correctLoginDateTimeFormated2 = correctLoginDateTime2.format(formater)
        private val correctLogoutDateTimeFormated2 = correctLogoutDateTime2.format(formater)

        //сессия с разрывом (после)
        private val correctLoginDateTimeFormated3 = correctLoginDateTime3.format(formater)
        private val correctLogoutDateTimeFormated3 = correctLogoutDateTime3.format(formater)

        //другой год
        private val correctLoginDateTimeFormated4 = correctLoginDateTime4.format(formater)
        private val correctLogoutDateTimeFormated4 = correctLogoutDateTime4.format(formater)

        //сессия с разрывом (перед)
        private val correctLoginDateTimeFormated5 = correctLoginDateTime5.format(formater)
        private val correctLogoutDateTimeFormated5 = correctLogoutDateTime5.format(formater)



        @JvmStatic
        private fun getUserRegistrationArguments(): List<Arguments> {
           return listOf(
               Arguments.of(Pair("123", "Alice"),
                   Pair("user_id", "user_name"),
                   HttpStatus.BAD_REQUEST.code,
                   "Missing parameters"
               ),
               Arguments.of(Pair("123", "Alice"),
                   Pair("userId", "userName"),
                   HttpStatus.OK.code,
                   "User registered: true"
               ),
               Arguments.of(Pair("123", "Alice"),
                   Pair("userId", "userName"),
                   HttpStatus.CONFLICT.code,
                   "User already exist"
               ),
                Arguments.of(Pair("122", "Bob"),
                    Pair("userId", "userName"),
                    HttpStatus.OK.code,
                    "User registered: true"
                ))
        }

        @JvmStatic
        private fun getRecordSessionArguments(): List<Arguments> {
            val correctParamsName: Tuple3<String, String, String> = Tuple3.tuple("userId", "loginTime", "logoutTime")

            val correctParamsValue1: Tuple3<String, String, String> = Tuple3.tuple("123",
                correctLoginDateTimeFormated1,
                correctLogoutDateTimeFormated1)

            val correctParamsValue2: Tuple3<String, String, String> = Tuple3.tuple("123",
                correctLoginDateTimeFormated2,
                correctLogoutDateTimeFormated2)

            val correctParamsValue3: Tuple3<String, String, String> = Tuple3.tuple("123",
                correctLoginDateTimeFormated3,
                correctLogoutDateTimeFormated3)

            val correctParamsValue4: Tuple3<String, String, String> = Tuple3.tuple("123",
                correctLoginDateTimeFormated4,
                correctLogoutDateTimeFormated4)

            val correctParamsValue5: Tuple3<String, String, String> = Tuple3.tuple("123",
                correctLoginDateTimeFormated5,
                correctLogoutDateTimeFormated5)

            return listOf(
                Arguments.of(
                    Tuple3.tuple(FAKE_ID.second, correctLoginDateTimeFormated1, correctLogoutDateTimeFormated1),
                    correctParamsName,
                    HttpStatus.NOT_FOUND.code,
                    "User not found"
                ),
                Arguments.of(
                    correctParamsValue1,
                    Tuple3.tuple("user_id", "login_time", "logout_time"),
                    HttpStatus.BAD_REQUEST.code,
                    "Missing parameters"
                ),
                Arguments.of(
                    Tuple3.tuple("123", "2025-03-08 13:00:00", "2025-03-08 15:00:00"),
                    correctParamsName,
                    HttpStatus.BAD_REQUEST.code,
                    "Invalid data: "
                ),
                Arguments.of(
                    Tuple3.tuple("123", correctLogoutDateTimeFormated1, correctLoginDateTimeFormated1),
                    correctParamsName,
                    HttpStatus.BAD_REQUEST.code,
                    "Invalid data: "
                ),
                Arguments.of(
                    Tuple3.tuple("122", correctLoginDateTimeFormated1, correctLogoutDateTimeFormated1),
                    correctParamsName,
                    HttpStatus.OK.code,
                    "Session recorded"
                ),
                Arguments.of(
                    correctParamsValue1,
                    correctParamsName,
                    HttpStatus.OK.code,
                    "Session recorded"
                ),
                Arguments.of(
                    correctParamsValue2,
                    correctParamsName,
                    HttpStatus.OK.code,
                    "Session recorded"
                ),
                Arguments.of(
                    correctParamsValue3,
                    correctParamsName,
                    HttpStatus.OK.code,
                    "Session recorded"
                ),
                Arguments.of(
                    correctParamsValue4,
                    correctParamsName,
                    HttpStatus.OK.code,
                    "Session recorded"
                ),
                Arguments.of(
                    correctParamsValue5,
                    correctParamsName,
                    HttpStatus.OK.code,
                    "Session recorded"
                ),
            )
        }

        @JvmStatic
        private fun getTotalActivityArguments(): List<Arguments> {

            //корректное кол-во минут для общего периода
            val activityFor24Year = Duration.between(correctLoginDateTime4, correctLogoutDateTime4).toMinutes();
            val activityForPreBasicDateTime = Duration.between(correctLoginDateTime5, correctLogoutDateTime5).toMinutes()
            val activityForAfterBasicDateTime = Duration.between(correctLoginDateTime3, correctLogoutDateTime3).toMinutes()
            val totalActivityForUser = Duration.between(correctLoginDateTime2, correctLogoutDateTime2).toMinutes()
                .plus(activityFor24Year)
                .plus(activityForPreBasicDateTime)
                .plus(activityForAfterBasicDateTime)

            return listOf(
                Arguments.of(
                    Pair("userId", FAKE_ID.second),
                    HttpStatus.NOT_FOUND.code,
                    "Sessions not found for user"
                ),
                Arguments.of(
                    Pair(FAKE_ID.first, "123"),
                    HttpStatus.BAD_REQUEST.code,
                    "Missing userId"
                ),
                Arguments.of(
                    Pair("userId", "123"),
                    HttpStatus.OK.code,
                    "Total activity: $totalActivityForUser minutes"
                )

            )
        }

        @JvmStatic
        private fun getInactiveUserArguments(): List<Arguments> {
            return listOf(
                Arguments.of(
                    Pair("days", "-2"),
                    HttpStatus.BAD_REQUEST.code,
                    "Days param can't be negative"
                ),
                Arguments.of(
                    Pair("DAYS", "2"),
                    HttpStatus.BAD_REQUEST.code,
                    "Missing days parameter"
                ),
                Arguments.of(
                    Pair("days", "1"),
                    HttpStatus.OK.code,
                    Pair(1, setOf("123"))
                ),
                Arguments.of(
                    Pair("days", "0"),
                    HttpStatus.OK.code,
                    Pair(2, setOf("123, 122"))
                )
            )
        }

        @JvmStatic
        private fun getMonthActivityArguments(): List<Arguments> {
            val monthFormater: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
            val dateTimeFormater: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            return listOf(
                Arguments.of(
                    Pair("use_id", "MONTH"),
                    Pair(FAKE_ID.second, Month.MARCH.value),
                    HttpStatus.BAD_REQUEST.code,
                    "Missing parameters"
                ),
                Arguments.of(
                    Pair("userId", "month"),
                    Pair(FAKE_ID.first, YearMonth.of(2025, Month.MARCH.value).format(monthFormater)),
                    HttpStatus.NOT_FOUND.code,
                    "No sessions found for user"
                ),
                Arguments.of(
                    Pair("userId", "month"),
                    Pair("123", Month.MARCH.value),
                    HttpStatus.BAD_REQUEST.code,
                    "Invalid params"
                ),
                Arguments.of(
                    Pair("userId", "month"),
                    Pair("123", YearMonth.of(2025, Month.MARCH.value).format(monthFormater)),
                    HttpStatus.OK.code,
                    mapOf(Pair(correctLoginDateTime2.format(dateTimeFormater), Duration.between(correctLoginDateTime5,
                        correctLogoutDateTime3).toMinutes()),
                        Pair(correctLoginDateTime1.format(dateTimeFormater), Duration.between(correctLoginDateTime1, correctLogoutDateTime1).toMinutes()))
                )
            )
        }
    }

    /**
     * Тестирование контроллеров
     */
    @Order(1)
    @DisplayName("Тест регистрации пользователя")
    @ParameterizedTest
    @MethodSource(value = ["getUserRegistrationArguments"])
    fun testUserRegistration(user: Pair<String, String>,
                             params: Pair<String, String>,
                             code: Int,
                             messageBody: String) {

        RestAssured.given()
            .queryParams(params.first, user.first,params.second, user.second)
            .post("/register")
            .then()
            .statusCode(code)
            .body(Matchers.equalTo(messageBody))

    }

    @Order(2)
    @DisplayName("Тест записи сессии")
    @ParameterizedTest
    @MethodSource(value = ["getRecordSessionArguments"])
    fun testRecordSession(
        paramsValue: Tuple3<String, String, String>,
        paramsName: Tuple3<String, String, String>,
        code: Int,
        messageBody: String
    ) {
        RestAssured.given()
            .queryParam(paramsName.v1, paramsValue.v1)
            .queryParam(paramsName.v2, paramsValue.v2)
            .queryParam(paramsName.v3, paramsValue.v3)
            .post("/recordSession")
            .then()
            .statusCode(code)
            .body(Matchers.containsString(messageBody))
    }

    @Order(3)
    @DisplayName("Тест получения общего времени активности")
    @ParameterizedTest
    @MethodSource(value = ["getTotalActivityArguments"])
    fun testGetTotalActivity(params: Pair<String, String>, code: Int, messageBody: String) {
        RestAssured.given()
            .queryParam(params.first, params.second)
            .get("/totalActivity")
            .then()
            .statusCode(code)
            .body(Matchers.containsString(messageBody))
    }


    @Order(4)
    @DisplayName("Тест получения пользователей, логаут которых больше установленного" +
            "кол-ва дней")
    @ParameterizedTest
    @MethodSource(value = ["getInactiveUserArguments"])
    fun testInactiveUsers(params: Pair<String, Int>, code: Int, messageBody: Any) {
         val response = RestAssured.given()
            .queryParams(params.first, params.second)
            .get("/inactiveUsers")
            .then()
            .statusCode(code)
            .extract()
            .response()

        val assertions: (List<String>) -> Unit = {
            val jsonTestParams = (messageBody as? Pair<*, *>)?.second as? Set<String>
            jsonTestParams?.let {
                assertAll("Группа для тестирования json метода inactiveUsers",
                    { assertEquals(it.size, jsonTestParams.size, "Incorrect number of elements: ${it.size}, " +
                            "expected: ${jsonTestParams.size}") },
                    { assertTrue(jsonTestParams.containsAll(it), "Request return incorrect data") }
                )
            }
        }
        assertComplexBody(response, messageBody, assertions)
    }

    @Order(5)
    @DisplayName("Тест получения месячной активности")
    @ParameterizedTest
    @MethodSource(value = ["getMonthActivityArguments"])
    fun testUserMonthActivity(paramsName: Pair<String, String>,
              paramsValue: Pair<String, String>,
              code: Int,
              messageBody: Any) {
        val response = RestAssured.given()
            .queryParams(paramsName.first, paramsValue.first,
                paramsName.second, paramsValue.second)
            .get("/monthlyActivity")
            .then()
            .statusCode(code)
            .extract()
            .response()

        val assertions: (Map<String, Long>) -> Unit = {
            val jsonTestParams = messageBody as? Map<String, Long>
            jsonTestParams?.let {
                assertAll("Группа для тестирования json метода monthlyActivity",
                    { assertEquals(it.size, jsonTestParams.size, "Incorrect number of elements: ${it.size}, " +
                            "expected: ${jsonTestParams.size}") },
                    { assertEquals(jsonTestParams, it, "Request return incorrect data") }
                )
            }
        }
        assertComplexBody(response, messageBody, assertions)
    }

    /**
     * Метод для извлечения данных из джейсона
     */
    private fun <T> getUserJson(response: Response): T? {
        if (response.contentType.contains("json")) {
            val usersList = response.jsonPath().get<T>("$")
            return usersList
        }
        return null
    }

    /**
     * Метод для валидации json
     */
    private fun <T> assertComplexBody(response: Response, messageBody: Any, assertions: (T) -> Unit) {
        val userJsonData: T? = getUserJson(response)
        if (userJsonData != null) {
            assertions(userJsonData)
        }
        else {
            assertTrue(((messageBody as? String) ?: "") == (response.body.asString()), "Extend message: $messageBody")
        }
    }

}
