package LinkerBell.campus_market_spring.controller;

import LinkerBell.campus_market_spring.dto.AuthUserDto;
import LinkerBell.campus_market_spring.dto.ChatMessageResponseDto;
import LinkerBell.campus_market_spring.dto.CollectionResponse.ChatMessageCollectionResponseDto;
import LinkerBell.campus_market_spring.dto.GetMessageContentsRequestDto;
import LinkerBell.campus_market_spring.dto.ReadMessageRequestDto;
import LinkerBell.campus_market_spring.dto.RecentChatMessageResponseDto;
import LinkerBell.campus_market_spring.global.auth.Login;
import LinkerBell.campus_market_spring.service.ChatMessageService;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    // 최근 7일간 메시지 목록 가져오기
    @GetMapping("api/v1/chat/recent-message")
    public ResponseEntity<RecentChatMessageResponseDto> getRecentMessage(
        @Login AuthUserDto authUserDto) {
        RecentChatMessageResponseDto recentChatMessageResponseDto = chatMessageService.getRecentMessageList(
            authUserDto.getUserId());
        return ResponseEntity.ok(recentChatMessageResponseDto);
    }

    // 메시지 읽음 표시하기
    @PatchMapping("api/v1/chat/read-message")
    public ResponseEntity<Void> readMessage(
        @RequestBody ReadMessageRequestDto readMessageRequestDto) {
        if (readMessageRequestDto == null || readMessageRequestDto.getMessageId() == null) {
            return ResponseEntity.noContent().build();
        }
        chatMessageService.readMessage(readMessageRequestDto.getMessageId());
        return ResponseEntity.noContent().build();
    }

    // 메시지 내용들 가져오기
    @PostMapping("api/v1/chat/message")
    public ResponseEntity<ChatMessageCollectionResponseDto> getMessageContents(
        @RequestBody GetMessageContentsRequestDto getMessageContentsRequestDto) {
        if (getMessageContentsRequestDto == null || getMessageContentsRequestDto.getMessageIdList()
            .isEmpty()) {
            return ResponseEntity.ok(new ChatMessageCollectionResponseDto(Collections.emptyList()));
        }
        List<Long> messageIdList = getMessageContentsRequestDto.getMessageIdList();
        List<ChatMessageResponseDto> chatMessageResponseDtoList = chatMessageService.getMessageContents(
            messageIdList);
        return ResponseEntity.ok(ChatMessageCollectionResponseDto.from(chatMessageResponseDtoList));
    }
}
