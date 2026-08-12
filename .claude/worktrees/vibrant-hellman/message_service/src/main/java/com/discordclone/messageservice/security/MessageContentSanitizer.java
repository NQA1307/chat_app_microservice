package com.discordclone.messageservice.security;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

@Component
public class MessageContentSanitizer {

    private final PolicyFactory policy = new HtmlPolicyBuilder()
            .toFactory();

    public String sanitizePlainText(String input) {
        if (input == null) {
            return null;
        }

        String cleaned = policy.sanitize(input);
        return cleaned.trim();
    }
}