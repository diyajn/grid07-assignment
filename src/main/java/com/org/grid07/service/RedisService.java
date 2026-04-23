package com.org.grid07.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    // ─── VIRALITY SCORE ───────────────────────────────────────────

    public void addViralityScore(Long postId, int points) {
        String key = "post:" + postId + ":virality_score";
        redisTemplate.opsForValue().increment(key, points);
    }

    // ─── HORIZONTAL CAP (max 100 bot replies per post) ────────────

    /**
     * Returns true if bot comment is ALLOWED (count was under 100).
     * Uses INCR atomically — even 200 concurrent threads won't exceed 100.
     */
    public boolean tryIncrementBotCount(Long postId) {
        String key = "post:" + postId + ":bot_count";
        Long newCount = redisTemplate.opsForValue().increment(key);
        if (newCount > 100) {
            // Roll back the increment — we overshot
            redisTemplate.opsForValue().decrement(key);
            return false;
        }
        return true;
    }

    // ─── COOLDOWN CAP (bot can't interact with same human < 10 min) ─

    public boolean isCooldownActive(Long botId, Long humanId) {
        String key = "cooldown:bot_" + botId + ":human_" + humanId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void setCooldown(Long botId, Long humanId) {
        String key = "cooldown:bot_" + botId + ":human_" + humanId;
        redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(10));
    }

    // ─── NOTIFICATION ENGINE ──────────────────────────────────────

    public boolean hasRecentNotification(Long userId) {
        String key = "notif_cooldown:" + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void setNotificationCooldown(Long userId) {
        redisTemplate.opsForValue().set(
                "notif_cooldown:" + userId, "1", Duration.ofMinutes(15)
        );
    }

    public void pushPendingNotification(Long userId, String message) {
        redisTemplate.opsForList().rightPush("user:" + userId + ":pending_notifs", message);
    }

    public List<String> popAllPendingNotifications(Long userId) {
        String key = "user:" + userId + ":pending_notifs";
        Long size = redisTemplate.opsForList().size(key);
        if (size == null || size == 0) return List.of();
        List<String> notifs = redisTemplate.opsForList().range(key, 0, size - 1);
        redisTemplate.delete(key);
        return notifs;
    }

    public Set<String> getAllPendingNotifKeys() {
        return redisTemplate.keys("user:*:pending_notifs");
    }
}
