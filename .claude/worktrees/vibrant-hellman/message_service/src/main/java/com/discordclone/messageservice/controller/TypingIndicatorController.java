 package com.discordclone.messageservice.controller;

  import com.discordclone.messageservice.dto.request.TypingIndicatorRequest;
  import com.discordclone.messageservice.service.TypingIndicatorService;
  import java.util.Map;
  import lombok.RequiredArgsConstructor;
  import org.springframework.messaging.handler.annotation.MessageMapping;
  import org.springframework.messaging.handler.annotation.Payload;
  import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
  import org.springframework.stereotype.Controller;

  import java.util.UUID;

  @Controller
  @RequiredArgsConstructor
  public class TypingIndicatorController {

      private final TypingIndicatorService typingIndicatorService;

      @MessageMapping("/typing")
      public void typing(
              @Payload TypingIndicatorRequest request,
              SimpMessageHeaderAccessor headers
      ) {
          Map<String, Object> sessionAttributes = headers.getSessionAttributes();
          if (sessionAttributes == null) {
              return;
          }

          UUID userId = (UUID) sessionAttributes.get("userId");
          String username = (String) sessionAttributes.get("username");

          typingIndicatorService.handleTyping(userId, username, request);
      }
  }
