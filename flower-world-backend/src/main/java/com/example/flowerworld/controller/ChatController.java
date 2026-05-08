package com.example.flowerworld.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_TEXT_FILE_BYTES = 1024L * 1024;

    private final WebClient webClient;
    private final SessionController sessionController;
    private final ObjectMapper objectMapper;
    private final String apiToken;
    private final String model;

    public ChatController(
            WebClient.Builder webClientBuilder,
            SessionController sessionController,
            ObjectMapper objectMapper,
            @Value("${openclaw.base-url:http://localhost:35108}") String openClawBaseUrl,
            @Value("${openclaw.api-token:}") String apiToken,
            @Value("${openclaw.model:openclaw/default}") String model) {
        this.webClient = webClientBuilder.baseUrl(openClawBaseUrl).build();
        this.sessionController = sessionController;
        this.objectMapper = objectMapper;
        this.apiToken = apiToken;
        this.model = model;
    }

    @PostMapping(value = "/chat", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter handleChat(
            @RequestParam(value = "prompt", required = false, defaultValue = "") String prompt,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(2).toMillis());
        AtomicReference<Disposable> upstreamRef = new AtomicReference<>();

        String normalizedPrompt = normalizePrompt(prompt, file);
        Object userContent;
        try {
            userContent = buildUserContent(normalizedPrompt, file);
        } catch (Exception e) {
            log.warn("Invalid uploaded file", e);
            completeWithMessage(emitter, "文件处理失败：" + e.getMessage());
            return emitter;
        }

        // 1. 将用户消息存入历史记录
        sessionController.addMessageToHistory(sessionId, "user", buildHistoryPrompt(normalizedPrompt, file));

        // 2. 准备发给OpenClaw的当前用户消息
        Map<String, Object> currentUserMessage = Map.of("role", "user", "content", userContent);

        // 3. 构建请求体，添加 stream: true 启用流式响应
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(currentUserMessage),
                "stream", true
        );

        // 4. 发起流式调用，使用 bodyToFlux 接收流式响应
        Flux<String> responseFlux = webClient.post()
                .uri("/v1/chat/completions")
                .headers(headers -> {
                    if (apiToken != null && !apiToken.isBlank()) {
                        headers.setBearerAuth(apiToken);
                    }
                    headers.set("x-openclaw-session-key", sessionId);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("OpenClaw request failed")
                                .map(body -> new IllegalStateException(
                                        "OpenClaw returned " + response.statusCode() + ": " + body)))
                .bodyToFlux(String.class)
                .timeout(Duration.ofMinutes(2));

        // 5. 订阅并逐块处理流式响应
        StringBuilder fullResponse = new StringBuilder();
        
        Disposable upstream = responseFlux.subscribe(chunk -> {
            try {
                // 检查是否是结束标记
                if ("[DONE]".equals(chunk.trim())) {
                    return; // 等待订阅完成回调处理
                }
                
                // 解析 OpenClaw 的流式响应，提取 delta.content
                String content = parseOpenClawChunk(chunk.trim());
                
                if (content != null && !content.isEmpty()) {
                    // 发送纯文本内容给前端（SseEmitter 会自动添加 data: 前缀）
                    emitter.send(SseEmitter.event().data(content));
                    fullResponse.append(content);
                }
            } catch (Exception e) {
                log.warn("Error processing OpenClaw chunk", e);
            }
        }, error -> {
            log.warn("Error in streaming API call", error);
            try {
                emitter.send(SseEmitter.event().data("请求 OpenClaw 失败：" + error.getMessage()));
            } catch (Exception ignored) {
                // The client may already be gone.
            } finally {
                emitter.completeWithError(error);
            }
        }, () -> {
            try {
                // 流结束时，将完整的AI回复存入历史
                String fullContent = fullResponse.toString();
                if (!fullContent.isEmpty()) {
                    sessionController.addMessageToHistory(sessionId, "assistant", fullContent);
                }
            } catch (Exception e) {
                log.warn("Error saving chat history", e);
            } finally {
                // 发送 [DONE] 标记告知前端流结束，然后结束SSE连接
                try {
                    emitter.send(SseEmitter.event().data("[DONE]"));
                } catch (Exception e) {
                    // ignore
                }
                emitter.complete();
            }
        });

        upstreamRef.set(upstream);
        emitter.onCompletion(() -> disposeUpstream(upstreamRef));
        emitter.onTimeout(() -> {
            disposeUpstream(upstreamRef);
            emitter.complete();
        });
        emitter.onError(error -> disposeUpstream(upstreamRef));

        return emitter;
    }

    private String normalizePrompt(String prompt, MultipartFile file) {
        String normalized = prompt == null ? "" : prompt.trim();
        if (!normalized.isEmpty() || file == null || file.isEmpty()) {
            return normalized;
        }

        String contentType = resolveContentType(file);
        if (contentType.startsWith("image/")) {
            return "请描述这张图片";
        }

        return "请阅读这个文件并总结主要内容";
    }

    private Object buildUserContent(String prompt, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return prompt;
        }

        String contentType = resolveContentType(file);
        String fileName = file.getOriginalFilename() == null ? "uploaded-file" : file.getOriginalFilename();

        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", prompt));

        if (contentType.startsWith("image/")) {
            if (file.getSize() > MAX_IMAGE_BYTES) {
                throw new IllegalArgumentException("图片不能超过 10MB");
            }
            String dataUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(file.getBytes());
            content.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", dataUrl)
            ));
            return content;
        }

        if (isTextFile(contentType, fileName)) {
            if (file.getSize() > MAX_TEXT_FILE_BYTES) {
                throw new IllegalArgumentException("文本文件不能超过 1MB");
            }
            String fileText = new String(file.getBytes(), StandardCharsets.UTF_8);
            content.add(Map.of(
                    "type", "text",
                    "text", "\n\n附件文件名：" + fileName + "\n附件内容：\n" + fileText
            ));
            return content;
        }

        throw new IllegalArgumentException("当前仅支持图片文件或 UTF-8 文本文件");
    }

    private boolean isTextFile(String contentType, String fileName) {
        if (contentType != null && (contentType.startsWith("text/") || contentType.equals(MediaType.APPLICATION_JSON_VALUE))) {
            return true;
        }

        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".txt")
                || lowerName.endsWith(".md")
                || lowerName.endsWith(".json")
                || lowerName.endsWith(".csv")
                || lowerName.endsWith(".yaml")
                || lowerName.endsWith(".yml")
                || lowerName.endsWith(".xml")
                || lowerName.endsWith(".html")
                || lowerName.endsWith(".css")
                || lowerName.endsWith(".js")
                || lowerName.endsWith(".java");
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }

        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (fileName.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        if (fileName.endsWith(".gif")) {
            return MediaType.IMAGE_GIF_VALUE;
        }
        if (fileName.endsWith(".webp")) {
            return "image/webp";
        }
        if (isTextFile(null, fileName)) {
            return MediaType.TEXT_PLAIN_VALUE;
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private String buildHistoryPrompt(String prompt, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return prompt;
        }

        String fileName = file.getOriginalFilename() == null ? "uploaded-file" : file.getOriginalFilename();
        return prompt + "\n\n[已上传文件：" + fileName + "]";
    }

    private void completeWithMessage(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().data(message));
            emitter.send(SseEmitter.event().data("[DONE]"));
        } catch (Exception ignored) {
            // The client may already be gone.
        } finally {
            emitter.complete();
        }
    }

    /**
     * 解析 OpenClaw 的流式响应，提取 delta.content
     */
    private String parseOpenClawChunk(String line) {
        try {
            // 移除可能的 "data:" 前缀
            String json = line.trim();
            if (json.startsWith("data:")) {
                json = json.substring(5).trim();
            }
            
            // 跳过 [DONE] 标记
            if ("[DONE]".equals(json)) {
                return null;
            }
            
            // 解析 JSON
            Map<String, Object> chunk = objectMapper.readValue(json, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                if (delta != null && delta.containsKey("content")) {
                    return (String) delta.get("content");
                }
            }
        } catch (Exception e) {
            // Ignore malformed chunks
        }
        return null;
    }

    private void disposeUpstream(AtomicReference<Disposable> upstreamRef) {
        Disposable upstream = upstreamRef.getAndSet(null);
        if (upstream != null && !upstream.isDisposed()) {
            upstream.dispose();
        }
    }
}
