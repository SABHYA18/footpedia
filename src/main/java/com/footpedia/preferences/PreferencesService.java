package com.footpedia.preferences;

import com.footpedia.preferences.dto.PreferencesResponse;
import com.footpedia.preferences.dto.UpdatePreferencesRequest;

public interface PreferencesService {

    PreferencesResponse get(Long userId);

    PreferencesResponse update(Long userId, UpdatePreferencesRequest request);
}
