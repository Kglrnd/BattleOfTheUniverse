package de.kugi.dev.battleoftheuniverse.user;

import de.kugi.dev.battleoftheuniverse.user.dto.AppSettingsView;
import de.kugi.dev.battleoftheuniverse.user.dto.UpdateRegistrationEnabledRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final AppSettingsService appSettingsService;

    @GetMapping
    public AppSettingsView get() {
        return new AppSettingsView(appSettingsService.isRegistrationEnabled());
    }

    @PatchMapping("/registration")
    public AppSettingsView updateRegistration(@Valid @RequestBody UpdateRegistrationEnabledRequest request) {
        AppSettings settings = appSettingsService.setRegistrationEnabled(request.registrationEnabled());
        return new AppSettingsView(settings.isRegistrationEnabled());
    }
}
