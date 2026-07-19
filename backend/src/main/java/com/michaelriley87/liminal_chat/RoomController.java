package com.michaelriley87.liminal_chat;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rooms")
public class RoomController {
  private final RoomService roomService;

  public RoomController(RoomService roomService) {
    this.roomService = roomService;
  }

  @PostMapping
  public Room createRoom() {
    return roomService.createRoom();
  }

  @GetMapping("/{code}")
  public ResponseEntity<Room> getRoom(@PathVariable String code) {
    Room room = roomService.getRoom(code);

    if (room == null) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(room);
  }
}
