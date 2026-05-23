package com.discordclone.userservice.service;

import com.discordclone.userservice.entity.User;
import com.discordclone.userservice.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.SearchRequest;
import com.meilisearch.sdk.model.Settings;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeiliSearchService {

    private final Client meiliClient;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${meilisearch.index-name}")
    private String indexName;

    @PostConstruct
    public void init() {
        syncAllUsers();
    }

    /**
     * Đồng bộ toàn bộ User từ DB sang Meilisearch
     */
    public int syncAllUsers() {
        try {
            log.info("Starting Meilisearch sync for index: {}", indexName);
            
            try {
                meiliClient.createIndex(indexName, "id");
            } catch (Exception e) {
                // Index exists
            }

            Index index = meiliClient.index(indexName);
            
            Settings settings = new Settings();
            settings.setSearchableAttributes(new String[]{"username", "displayName", "email"});
            index.updateSettings(settings);

            List<User> allUsers = userRepository.findAll();
            if (!allUsers.isEmpty()) {
                List<Map<String, Object>> docs = allUsers.stream().map(this::toDocument).toList();
                index.addDocuments(objectMapper.writeValueAsString(docs));
                log.info(">>> SUCCESS: Synced {} users to Meilisearch", docs.size());
                return docs.size();
            }
            log.info(">>> No users in DB to sync");
            return 0;
        } catch (Exception e) {
            log.error(">>> ERROR during Meilisearch sync: {}", e.getMessage());
            return -1;
        }
    }

    public void indexNewUser(User user) {
        try {
            Index index = meiliClient.index(indexName);
            index.addDocuments(objectMapper.writeValueAsString(List.of(toDocument(user))));
            log.info(">>> Indexed new user: {}", user.getUsername());
        } catch (Exception e) {
            log.error(">>> Failed to index user {}: {}", user.getUsername(), e.getMessage());
        }
    }

    public List<Map<String, Object>> searchUsers(String query, int limit) {
        try {
            Index index = meiliClient.index(indexName);
            SearchRequest request = new SearchRequest(query).setLimit(limit);
            com.meilisearch.sdk.model.Searchable result = index.search(request);
            
            List<Map<String, Object>> returnHits = new ArrayList<>();
            if (result != null && result.getHits() != null) {
                for (Object hitObj : result.getHits()) {
                    if (hitObj instanceof Map) {
                        returnHits.add((Map<String, Object>) hitObj);
                    }
                }
            }
            log.info("Meilisearch found {} hits for query: '{}'", returnHits.size(), query);
            return returnHits;
        } catch (Exception e) {
            log.error(">>> Meilisearch search error: {}", e.getMessage());
            return List.of();
        }
    }

    private Map<String, Object> toDocument(User user) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("id", user.getId().toString());
        doc.put("username", user.getUsername());
        doc.put("displayName", user.getDisplayName());
        doc.put("email", user.getEmail());
        doc.put("avatar", user.getAvatarUrl());
        return doc;
    }
}
