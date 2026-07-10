package de.kugi.dev.battleoftheuniverse.user;

import de.kugi.dev.battleoftheuniverse.user.dto.AdminUserView;
import de.kugi.dev.battleoftheuniverse.user.dto.ChangeRoleRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public List<AdminUserView> list() {
        return userService.listAll();
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminUserView changeRole(@PathVariable Long id, @Valid @RequestBody ChangeRoleRequest request,
                                     @AuthenticationPrincipal AppUserPrincipal principal) {
        if (id.equals(principal.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot change your own role");
        }
        return userService.changeRole(id, request.role());
    }
}
