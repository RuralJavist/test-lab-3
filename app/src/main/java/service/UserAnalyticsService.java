package service;

import org.example.exceptions.ElementNotFoundException;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class UserAnalyticsService {

    private final Map<String, User> users = new HashMap<>();
    private final Map<String, List<Session>> userSessions = new HashMap<>();


    public boolean registerUser(String userId, String userName) {
        if (users.containsKey(userId)) {
            throw new IllegalArgumentException("User already exists");
        }
        users.put(userId, new User(userId, userName));
        return true;
    }

    public void recordSession(String userId, LocalDateTime loginTime, LocalDateTime logoutTime) {
        if (!users.containsKey(userId)) {
            throw new ElementNotFoundException("User not found");
        }
        if (loginTime.isAfter(logoutTime)) {
            throw new IllegalArgumentException("Login time is earlier than logout time");
        }
        Session session = new Session(loginTime, logoutTime);
        userSessions.computeIfAbsent(userId, k -> new ArrayList<>()).add(session);
    }

    public long getTotalActivityTime(String userId) {
        if (!userSessions.containsKey(userId)) {
            throw new ElementNotFoundException("Sessions not found for user");
        }
        List<Session> sortedSessionsList = userSessions.get(userId).stream().sorted().toList();
        return getActivityTime(sortedSessionsList);
    }

    /**
     * Переписал алгоритм подсчета активного времени, считает и параллельные сессии и сессии в разные дни,
     * не беря в общую сумму промежутки без сессий
     * @param sessions - сессии для анализа
     * @return - кол-во проведенного времени в минутах
     */
    private long getActivityTime(List<Session> sessions) {
        Session firstSession = sessions.getFirst();
        LocalDateTime minLoginTime = firstSession.getLoginTime();
        LocalDateTime maxLogoutTime = firstSession.getLogoutTime();
        long totalActivityTime = 0;

        for (Session session : sessions) {
            LocalDateTime sessionLoginTime = session.getLoginTime();
            LocalDateTime sessionLogoutTime = session.getLogoutTime();

            if (sessionLoginTime.isBefore(maxLogoutTime) || sessionLoginTime.isEqual(maxLogoutTime)) {
                maxLogoutTime = maxLogoutTime.isAfter(sessionLogoutTime) ? maxLogoutTime : sessionLogoutTime;
            }
            if (sessionLoginTime.isAfter(maxLogoutTime)) {
                totalActivityTime += Duration.between(minLoginTime, maxLogoutTime).toMinutes();
                minLoginTime = sessionLoginTime;
                maxLogoutTime = sessionLogoutTime;
            }
        }
        totalActivityTime += Duration.between(minLoginTime, maxLogoutTime).toMinutes();
        return totalActivityTime;
    }

    public List<String> findInactiveUsers(int days) {
        List<String> inactiveUsers = new ArrayList<>();
        for (Map.Entry<String, List<Session>> entry : userSessions.entrySet()) {
            String userId = entry.getKey();
            List<Session> sessions = entry.getValue();
            if (sessions.isEmpty()) continue;

            LocalDateTime lastSessionTime = sessions.stream().max(Comparator.comparing(Session::getLogoutTime))
                    .get().logoutTime; //правильнее взять макс значение по логауту

            long daysInactive = ChronoUnit.DAYS.between(lastSessionTime, LocalDateTime.now());
            if (daysInactive > days) {
                inactiveUsers.add(userId);
            }
        }
        return inactiveUsers;
    }

    /**
     * Возвращает метрику активности пользователя по дням за месяц.
     *
     * @param userId Идентификатор пользователя.
     * @param month Месяц для анализа активности.
     * @return Словарь, где ключ — это дата (в формате "yyyy-MM-dd"), а значение — общее время активности пользователя в этот день (в минутах).
     *
     * @throws IllegalArgumentException если сессии для пользователя не найдены.
     */
    public Map<String, Long> getMonthlyActivityMetric(String userId, YearMonth month) {
        if (!userSessions.containsKey(userId)) {
            throw new ElementNotFoundException("No sessions found for user");
        }
        Map<String, Long> activityByDay = new HashMap<>();
        userSessions.get(userId).stream()
                .filter(session -> isSessionInMonth(session, month))
                .forEach(session -> {
                    String dayKey = session.getLoginTime().toLocalDate().toString();

                    long minutes;
                    LocalDateTime startMonthTime = LocalDate.of(month.getYear(), month.getMonth(), 1).atStartOfDay();
                    LocalDateTime endMonthTime = startMonthTime.plusMonths(1);

                    LocalDateTime sessionLogoutTime = session.getLogoutTime();
                    LocalDateTime sessionLoginTime = session.getLoginTime();

                    LocalDateTime effectiveStart = sessionLoginTime.isBefore(startMonthTime) ? startMonthTime : sessionLoginTime;
                    LocalDateTime effectiveEnd = sessionLogoutTime.isAfter(endMonthTime) ? endMonthTime : sessionLogoutTime;

                    minutes = Duration.between(effectiveStart, effectiveEnd).toMinutes();
                    activityByDay.put(dayKey, activityByDay.getOrDefault(dayKey, 0L) + minutes);
                });
        return activityByDay;
    }


    /**
     * Проверяет, находится ли сессия в пределах заданного месяца.
     *
     * @param session Сессия для проверки.
     * @param month Месяц для проверки.
     * @return true, если сессия входит в данный месяц, иначе false.
     */
    private boolean isSessionInMonth(Session session, YearMonth month) {
        LocalDateTime start = session.getLoginTime();
        LocalDateTime end = session.getLogoutTime();
        int monthYear = month.getYear();
        int monthMonth = month.getMonthValue();

        if (start.getYear() > monthYear || end.getYear() < monthYear) {
            return false;
        }

        return start.getMonthValue() <= monthMonth && end.getMonthValue() >= monthMonth;
    }

    public User getUser(String userId) {
        return users.get(userId);
    }

    /**
     * Добавил проверку на наличие сессий для пользователя
     * @param userId - идентификатор пользователя
     * @return - список сессий
     */
    public List<Session> getUserSessions(String userId) {
        if (!userSessions.containsKey(userId)) {
            throw new ElementNotFoundException("No sessions found for user");
        }
        return userSessions.get(userId);
    }

    public static class User {
        private final String userId;
        private final String userName;

        public User(String userId, String userName) {
            this.userId = userId;
            this.userName = userName;
        }

        public String getUserId() {
            return userId;
        }

        public String getUserName() {
            return userName;
        }
    }

    public static class Session implements Comparable<Session> {
        private final LocalDateTime loginTime;
        private final LocalDateTime logoutTime;

        public Session(LocalDateTime loginTime, LocalDateTime logoutTime) {
            this.loginTime = loginTime;
            this.logoutTime = logoutTime;
        }

        public LocalDateTime getLoginTime() {
            return loginTime;
        }

        public LocalDateTime getLogoutTime() {
            return logoutTime;
        }

        @Override
        public int compareTo(@NotNull Session o) {
            return this.loginTime.compareTo(o.loginTime);
        }
    }
}
