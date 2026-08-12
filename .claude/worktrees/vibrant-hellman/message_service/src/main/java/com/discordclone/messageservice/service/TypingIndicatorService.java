package com.discordclone.messageservice.service;

  import com.discordclone.messageservice.dto.request.TypingIndicatorRequest;
  import com.discordclone.messageservice.dto.response.TypingIndicatorEvent;
  import com.discordclone.messageservice.entity.Conversation;
  import com.discordclone.messageservice.repository.ConversationRepository;
  import lombok.RequiredArgsConstructor;
  import org.springframework.data.redis.core.StringRedisTemplate;
  import org.springframework.messaging.simp.SimpMessagingTemplate;
  import org.springframework.stereotype.Service;

  import java.time.Duration;
  import java.time.Instant;
  import java.util.Locale;
  import java.util.UUID;

  @Service
  @RequiredArgsConstructor
  public class TypingIndicatorService {

      private static final Duration TYPING_TTL = Duration.ofSeconds(5);
      private static final Duration RATE_TTL = Duration.ofSeconds(2);

      private final StringRedisTemplate redisTemplate;
      private final SimpMessagingTemplate messagingTemplate;
      private final PermissionService permissionService;
      private final ConversationRepository conversationRepository;

      public void handleTyping(UUID userId, String username, TypingIndicatorRequest request) {
          if (userId == null) {
              return;
          }

          String scope = request.scope().toUpperCase(Locale.ROOT);
          String targetId = request.targetId();

          validateAccess(scope, targetId, userId);

          String rateKey = "typing-rate:%s:%s:%s".formatted(scope, targetId, userId);
          Boolean firstEvent = redisTemplate.opsForValue()
                  .setIfAbsent(rateKey, "1", RATE_TTL);

          if (!Boolean.TRUE.equals(firstEvent) && request.typing()) {
              return;
          }

          String typingKey = "typing:%s:%s:%s".formatted(scope, targetId, userId);

          if (request.typing()) {
              redisTemplate.opsForValue().set(typingKey, username == null ? "" : username, TYPING_TTL);
          } else {
              redisTemplate.delete(typingKey);
          }

          TypingIndicatorEvent event = TypingIndicatorEvent.builder()
                  .scope(scope)
                  .targetId(targetId)
                  .userId(userId)
                  .username(username)
                  .typing(request.typing())
                  .expiresAt(Instant.now().plus(TYPING_TTL))
                  .build();

          messagingTemplate.convertAndSend(destination(scope, targetId), event);
      }

      public void clearTyping(String scope, String targetId, UUID userId, String username) {
          handleTyping(userId, username, new TypingIndicatorRequest(scope, targetId, false));
      }

      private void validateAccess(String scope, String targetId, UUID userId) {
          if ("CHANNEL".equals(scope)) {
              Long channelId = Long.valueOf(targetId);
              if (!permissionService.canAccessChannel(channelId, userId)) {
                  throw new IllegalArgumentException("No channel access");
              }
              return;
          }

          if ("DM".equals(scope)) {
              Long conversationId = Long.valueOf(targetId);
              Conversation conversation = conversationRepository.findById(conversationId)
                      .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
              boolean member = conversation.getParticipantId1().equals(userId)
                      || conversation.getParticipantId2().equals(userId);
              if (!member) {
                  throw new IllegalArgumentException("No DM access");
              }
              return;
          }

          throw new IllegalArgumentException("Invalid typing scope");
      }

      private String destination(String scope, String targetId) {
          if ("CHANNEL".equals(scope)) {
            return "/topic/channels." + targetId + ".typing";
          }
        return "/topic/dm." + targetId + ".typing";
      }
  }
