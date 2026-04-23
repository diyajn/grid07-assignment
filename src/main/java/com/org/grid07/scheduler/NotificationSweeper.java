package com.org.grid07.scheduler;

import com.org.grid07.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class NotificationSweeper {

    private final RedisService redisService;

    @Scheduled(fixedRate = 300000) // every 5 minutes
    public void sweepPendingNotifications() {
        System.out.println("🧹 CRON Sweeper running...");

        Set<String> keys = redisService.getAllPendingNotifKeys();
        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            // Extract userId from key pattern: user:{id}:pending_notifs
            String[] parts = key.split(":");
            Long userId = Long.parseLong(parts[1]);

            List<String> notifs = redisService.popAllPendingNotifications(userId);
            if (notifs.isEmpty()) continue;

            String first = notifs.get(0);
            int others = notifs.size() - 1;

            System.out.println("📦 Summarized Push Notification for User " + userId
                    + ": " + first + (others > 0 ? " and " + others + " others interacted with your posts." : ""));
        }
    }
}
