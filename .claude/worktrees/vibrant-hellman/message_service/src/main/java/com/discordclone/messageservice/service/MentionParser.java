package com.discordclone.messageservice.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class MentionParser {
    private static final Pattern USER_MENTION =
            Pattern.compile("<@([0-9a-fA-F\\-]{36})>");
    private static final Pattern EVERYONE_MENTION =
            Pattern.compile("(^|\\s)@everyone(?=\\s|$|[,.!?])");
    private static final Pattern HERE_MENTION =
            Pattern.compile("(^|\\s)@here(?=\\s|$|[,.!?])");

    public MentionParseResult parse(String content) {
        if (content == null || content.isBlank()) {
            return new MentionParseResult(List.of(), false, false);
        }

        LinkedHashSet<UUID> userIds = new LinkedHashSet<>();
        Matcher matcher = USER_MENTION.matcher(content);

        while (matcher.find()) {
            try {
                userIds.add(UUID.fromString(matcher.group(1)));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return new MentionParseResult(
                userIds.stream().limit(20).toList(),
                EVERYONE_MENTION.matcher(content).find(),
                HERE_MENTION.matcher(content).find()
        );
    }
}
