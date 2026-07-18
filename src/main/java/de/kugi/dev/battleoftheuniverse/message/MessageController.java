package de.kugi.dev.battleoftheuniverse.message;

import de.kugi.dev.battleoftheuniverse.message.dto.MessageView;
import de.kugi.dev.battleoftheuniverse.message.dto.SendMessageRequest;
import de.kugi.dev.battleoftheuniverse.message.dto.UnreadCountView;
import de.kugi.dev.battleoftheuniverse.user.AppUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/inbox")
    public List<MessageView> inbox(@AuthenticationPrincipal AppUserPrincipal principal) {
        return messageService.inbox(principal.getId(), principal.getUsername());
    }

    @GetMapping("/sent")
    public List<MessageView> sent(@AuthenticationPrincipal AppUserPrincipal principal) {
        return messageService.sent(principal.getId(), principal.getUsername());
    }

    @GetMapping("/unread-count")
    public UnreadCountView unreadCount(@AuthenticationPrincipal AppUserPrincipal principal) {
        return messageService.unreadCount(principal.getId());
    }

    @PostMapping
    public MessageView send(@Valid @RequestBody SendMessageRequest request, @AuthenticationPrincipal AppUserPrincipal principal) {
        return messageService.send(principal.getId(), principal.getUsername(), request);
    }

    @PostMapping("/{id}/read")
    public MessageView markRead(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        return messageService.markRead(principal.getId(), principal.getUsername(), id);
    }
}
