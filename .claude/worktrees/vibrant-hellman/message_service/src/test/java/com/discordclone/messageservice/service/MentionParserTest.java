package com.discordclone.messageservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class MentionParserTest {

    private final MentionParser parser = new MentionParser();

    @Test
    void parseExtractsDistinctUserMentionsInOrder() {
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");

        MentionParseResult result = parser.parse("hi <@" + first + "> and <@" + second + "> and <@" + first + ">");

        assertThat(result.userIds()).containsExactly(first, second);
        assertThat(result.mentionEveryone()).isFalse();
        assertThat(result.mentionHere()).isFalse();
    }

    @Test
    void parseDetectsHereAndEveryoneWithoutTreatingPlainAtNamesAsUserMentions() {
        MentionParseResult result = parser.parse("@everyone ping @here and @alice");

        assertThat(result.userIds()).isEmpty();
        assertThat(result.mentionEveryone()).isTrue();
        assertThat(result.mentionHere()).isTrue();
    }

    @Test
    void parseDoesNotMatchEveryoneInsideWords() {
        MentionParseResult result = parser.parse("email me at hello@everyone.example and not@here");

        assertThat(result.mentionEveryone()).isFalse();
        assertThat(result.mentionHere()).isFalse();
    }

    @Test
    void parseIgnoresInvalidStructuredMentions() {
        MentionParseResult result = parser.parse("hello <@not-a-uuid>");

        assertThat(result.userIds()).isEmpty();
    }
}
