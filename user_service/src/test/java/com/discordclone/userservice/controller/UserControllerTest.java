package com.discordclone.userservice.controller;

import com.discordclone.userservice.dto.response.UserResponse;
import com.discordclone.userservice.service.CloudinaryService;
import com.discordclone.userservice.service.MeiliSearchService;
import com.discordclone.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private MeiliSearchService meiliSearchService;

    @MockBean
    private CloudinaryService cloudinaryService;

    @Test
    public void testSearchUsers_Success() throws Exception {
        String query = "discord";
        UUID currentUserId = UUID.randomUUID();
        UserResponse mockUser = UserResponse.builder()
                .id(UUID.randomUUID())
                .username("discord_user")
                .displayName("Discord User")
                .build();

        Mockito.when(userService.searchUsers(Mockito.eq(query), Mockito.any(UUID.class)))
                .thenReturn(List.of(mockUser));

        mockMvc.perform(get("/api/users/search")
                        .param("q", query)
                        .header("X-User-Id", currentUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].username").value("discord_user"));
    }

    @Test
    public void testSearchUsers_MissingHeader() throws Exception {
        Mockito.when(userService.searchUsers(Mockito.eq("test"), Mockito.isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/users/search")
                        .param("q", "test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
}
