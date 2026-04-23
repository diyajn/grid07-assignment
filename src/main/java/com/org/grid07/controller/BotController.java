package com.org.grid07.controller;

import com.org.grid07.entity.Bot;
import com.org.grid07.repository.BotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bots")
@RequiredArgsConstructor
public class BotController {

    private final BotRepository botRepository;

    @PostMapping
    public ResponseEntity<Bot> createBot(@RequestBody Bot bot) {
        return ResponseEntity.ok(botRepository.save(bot));
    }

    @GetMapping
    public ResponseEntity<List<Bot>> getAllBots() {
        return ResponseEntity.ok(botRepository.findAll());
    }
}
