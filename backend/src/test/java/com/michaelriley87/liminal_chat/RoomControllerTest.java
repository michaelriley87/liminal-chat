package com.michaelriley87.liminal_chat;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RoomController.class)
class RoomControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private RoomService roomService;

  @Test
  void createsRoom() throws Exception {
    when(roomService.createRoom()).thenReturn(new Room("ABCDE"));

    mockMvc
        .perform(post("/rooms"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("ABCDE"));
  }

  @Test
  void getsExistingRoom() throws Exception {
    when(roomService.getRoom("ABCDE")).thenReturn(new Room("ABCDE"));

    mockMvc
        .perform(get("/rooms/ABCDE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("ABCDE"));
  }

  @Test
  void returnsNotFoundForMissingRoom() throws Exception {
    mockMvc.perform(get("/rooms/ABCDE")).andExpect(status().isNotFound());
  }
}
