package com.org.grid07.controller;

import com.org.grid07.entity.Comment;
import com.org.grid07.entity.Post;
import com.org.grid07.repository.CommentRepository;
import com.org.grid07.repository.PostRepository;
import com.org.grid07.repository.UserRepository;
import com.org.grid07.service.NotificationService;
import com.org.grid07.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostRepository postRepo;
    private final CommentRepository commentRepo;
    private final UserRepository userRepo;
    private final RedisService redisService;
    private final NotificationService notifService;

    // Create Post
    @PostMapping
    public ResponseEntity<Post> createPost(@RequestBody Post post) {
        return ResponseEntity.ok(postRepo.save(post));
    }

    // Add Comment
    @PostMapping("/{postId}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable Long postId,
            @RequestBody Comment comment) {

        // Set post ID
        comment.setPostId(postId);

        // GUARDRAIL 1: Vertical cap — max depth 20
        if (comment.getDepthLevel() > 20) {
            return ResponseEntity.status(429)
                    .body("Thread too deep. Max depth is 20.");
        }

        // GUARDRAIL 2: If it's a bot comment, check horizontal cap
        if ("BOT".equals(comment.getAuthorType())) {

            // Check cooldown (bot vs human post author)
            Post post = postRepo.findById(postId).orElseThrow();
            if (isCooldownViolation(comment.getAuthorId(), post.getAuthorId(), post.getAuthorType())) {
                return ResponseEntity.status(429).body("Cooldown active. Try after 10 minutes.");
            }

            // Atomic horizontal cap check
            if (!redisService.tryIncrementBotCount(postId)) {
                return ResponseEntity.status(429).body("Post bot reply limit reached (100).");
            }

            // Update virality score
            redisService.addViralityScore(postId, 1);

            // Trigger notification
            notifService.handleBotInteraction(post.getAuthorId(), comment.getAuthorId());

            // Set cooldown
            redisService.setCooldown(comment.getAuthorId(), post.getAuthorId());
        }

        // Add this block for HUMAN comments (after the BOT block)
        if ("USER".equals(comment.getAuthorType())) {
            redisService.addViralityScore(postId, 50);  // +50 for human comment
        }

        Comment saved = commentRepo.save(comment);
        return ResponseEntity.ok(saved);
    }

    // Like a Post
    @PostMapping("/{postId}/like")
    public ResponseEntity<String> likePost(
            @PathVariable Long postId,
            @RequestParam Long userId) {

        redisService.addViralityScore(postId, 20);
        return ResponseEntity.ok("Post liked! Virality +20");
    }

    private boolean isCooldownViolation(Long botId, Long authorId, String authorType) {
        return "USER".equals(authorType) && redisService.isCooldownActive(botId, authorId);
    }
}
