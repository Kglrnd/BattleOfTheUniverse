package de.kugi.dev.battleoftheuniverse.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppSettingsService {

    private final AppSettingsRepository appSettingsRepository;

    public boolean isRegistrationEnabled() {
        return settings().isRegistrationEnabled();
    }

    @Transactional
    public AppSettings setRegistrationEnabled(boolean enabled) {
        AppSettings settings = settings();
        settings.setRegistrationEnabled(enabled);
        return appSettingsRepository.save(settings);
    }

    private AppSettings settings() {
        return appSettingsRepository.findById(AppSettings.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("App settings row is missing"));
    }
}
