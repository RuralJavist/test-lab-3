package org.example.controller


import io.javalin.Javalin
import service.UserAnalyticsService
import java.time.LocalDateTime
import java.time.YearMonth
import io.javalin.http.Context
import org.example.exceptions.ElementNotFoundException

object UserAnalyticsController {
    @JvmStatic
    fun createApp(): Javalin {
        val service = UserAnalyticsService()
        val app = Javalin.create()

        app.post("/register") { ctx: Context ->
            val userId = ctx.queryParam("userId")
            val userName = ctx.queryParam("userName")
            if (userId == null || userName == null) {
                ctx.status(400).result("Missing parameters")
                return@post
            }
            try {
                val success = service.registerUser(userId, userName)
                ctx.result("User registered: $success")
            } catch (argumentException: IllegalArgumentException) {
                ctx.status(409)
                    .result("User already exist") //добавил обработчик
            }
        }

        app.post("/recordSession") { ctx: Context ->
            val userId = ctx.queryParam("userId")
            val loginTime = ctx.queryParam("loginTime")
            val logoutTime = ctx.queryParam("logoutTime")
            if (userId == null || loginTime == null || logoutTime == null) {
                ctx.status(400).result("Missing parameters")
                return@post
            }
            try {
                val login = LocalDateTime.parse(loginTime)
                val logout = LocalDateTime.parse(logoutTime)
                service.recordSession(userId, login, logout)
                ctx.result("Session recorded")
            } catch (e: ElementNotFoundException) {
                ctx.status(404).result(e.message) //добавил обработку ошибки not found
            } catch (e: Exception) {
                ctx.status(400).result("Invalid data: " + e.message)
            }
        }

        app.get("/totalActivity") { ctx: Context ->
            val userId = ctx.queryParam("userId")
            if (userId == null) {
                ctx.status(400).result("Missing userId")
                return@get
            }
            try {
                val minutes = service.getTotalActivityTime(userId)
                ctx.result("Total activity: $minutes minutes")
            } catch (e: ElementNotFoundException) {
                ctx.status(404).result(e.message)
            }
        }

        app.get("/inactiveUsers") { ctx: Context ->
            val daysParam = ctx.queryParam("days")
            if (daysParam == null) {
                ctx.status(400).result("Missing days parameter")
                return@get
            }
            try {
                val days = daysParam.toInt()
                if (days < 0) { //проверка но отриц. кол-во дней
                    ctx.status(400).result("Days param can't be negative")
                    return@get
                }
                val inactiveUsers = service.findInactiveUsers(days)
                ctx.json(inactiveUsers)
            } catch (e: NumberFormatException) {
                ctx.status(400).result("Invalid number format for days")
            }
        }

        app.get("/monthlyActivity") { ctx: Context ->
            val userId = ctx.queryParam("userId")
            val monthParam = ctx.queryParam("month")
            if (userId == null || monthParam == null) {
                ctx.status(400).result("Missing parameters")
                return@get
            }
            try {
                val month = YearMonth.parse(monthParam)
                val activity =
                    service.getMonthlyActivityMetric(userId, month)
                ctx.json(activity)
            } catch (e: ElementNotFoundException) {
                ctx.status(404).result(e.message)
            } catch (e: Exception) {
                ctx.status(400).result("Invalid params")
            }
        }
        return app
    }
}

