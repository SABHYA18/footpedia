package com.footpedia.preferences;

import com.footpedia.domain.AppUser;
import com.footpedia.preferences.dto.PreferencesResponse;
import com.footpedia.preferences.dto.UpdatePreferencesRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class PreferencesController {

    private final PreferencesService preferencesService;

    @GetMapping
    public PreferencesResponse get(@AuthenticationPrincipal AppUser user) {
        return preferencesService.get(user.getId());
    }

    @PutMapping
    public PreferencesResponse update(@AuthenticationPrincipal AppUser user, @Valid @RequestBody UpdatePreferencesRequest request) {
        return preferencesService.update(user.getId(), request);
    }
}
