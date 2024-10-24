package LinkerBell.campus_market_spring.controller;

import LinkerBell.campus_market_spring.dto.AuthUserDto;
import LinkerBell.campus_market_spring.dto.ChatRoomRequestDto;
import LinkerBell.campus_market_spring.dto.ChatRoomResponseDto;
import LinkerBell.campus_market_spring.global.auth.Login;
import LinkerBell.campus_market_spring.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    // 채팅방 만들기
    @PostMapping("api/v1/chat")
    public ResponseEntity<ChatRoomResponseDto> addChatRoom(@RequestBody ChatRoomRequestDto chatRoomRequestDto, @Login AuthUserDto user) {
        ChatRoomResponseDto chatRoomResponseDto = chatRoomService.addChatRoom(user, chatRoomRequestDto);
        return ResponseEntity.ok(chatRoomResponseDto);
    }
}
