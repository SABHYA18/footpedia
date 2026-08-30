package com.footpedia.web;

import com.footpedia.domain.AppUser;
import com.footpedia.web.dto.HomeResponse;
import com.footpedia.web.service.HomeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeQueryService homeQueryService;

    @GetMapping
    public HomeResponse get(@AuthenticationPrincipal AppUser user) {
        return homeQueryService.getHome(user.getId());
    }
}
