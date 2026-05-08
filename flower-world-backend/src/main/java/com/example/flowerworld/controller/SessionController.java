package com.example.flowerworld.controller;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final Map<String, String> sessions = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, String>>> sessionHistories = new ConcurrentHashMap<>();

    @PostMapping
    public Map<String, String> createSession() {
        String sessionId = "session-" + UUID.randomUUID().toString();
        String sessionName = "新对话 " + (sessions.size() + 1);
        sessions.put(sessionId, sessionName);
        sessionHistories.put(sessionId, Collections.synchronizedList(new ArrayList<>()));
        return Map.of("id", sessionId, "name", sessionName);
    }

    @GetMapping
    public List<Map<String, String>> getAllSessions() {
        List<Map<String, String>> sessionList = new ArrayList<>();
        sessions.forEach((id, name) -> sessionList.add(Map.of("id", id, "name", name)));
        return sessionList;
    }

    @GetMapping("/{sessionId}/history")
    public Map<String, Object> getSessionHistory(@PathVariable String sessionId) {
        List<Map<String, String>> messages = sessionHistories.getOrDefault(sessionId, new ArrayList<>());
        synchronized (messages) {
            return Map.of("messages", new ArrayList<>(messages));
        }
    }

    // 供ChatController调用的内部方法
    public void addMessageToHistory(String sessionId, String role, String content) {
        sessionHistories
                .computeIfAbsent(sessionId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(Map.of("role", role, "content", content));
    }
}
