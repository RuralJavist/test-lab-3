package service;

import java.util.Comparator;
import java.util.Optional;

public class UserStatusService {

    private final UserAnalyticsService userAnalyticsService;

    public UserStatusService(UserAnalyticsService userAnalyticsService) {
        this.userAnalyticsService = userAnalyticsService;
    }

    public String getUserStatus(String userId) {

        long totalActivityTime = userAnalyticsService.getTotalActivityTime(userId);

        if (totalActivityTime < 60) {
            return "Inactive";
        } else if (totalActivityTime < 120) {
            return "Active";
        } else {
            return "Highly active";
        }
    }

    /**
     * Немного подправил логику метода, возвращает дату самого последнего логаута
     * @param userId - идентификатор пользователя
     * @return - дата логаута
     */
    public Optional<String> getUserLastSessionDate(String userId) {
        return userAnalyticsService.getUserSessions(userId).stream()
                .max(Comparator.comparing(UserAnalyticsService.Session::getLogoutTime))
                .map(el -> el.getLogoutTime().toLocalDate().toString());
    }
}
