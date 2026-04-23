package com.org.grid07.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final RedisService redisService;

    public void handleBotInteraction(Long userId, Long botId) {
        String message = "Bot " + botId + " replied to your post";

        if (redisService.hasRecentNotification(userId)) {
            // User already notified recently — queue it
            redisService.pushPendingNotification(userId, message);
        } else {
            // Send immediately and set cooldown
            System.out.println("🔔 Push Notification Sent to User " + userId + ": " + message);
            redisService.setNotificationCooldown(userId);
        }
    }
}
