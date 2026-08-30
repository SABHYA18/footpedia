package com.footpedia.web.service;

import com.footpedia.web.dto.HomeResponse;

public interface HomeQueryService {
    HomeResponse getHome(Long userId);
}
