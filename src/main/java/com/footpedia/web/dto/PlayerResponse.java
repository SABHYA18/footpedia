package com.footpedia.web.dto;

import com.footpedia.domain.Player;

import java.time.LocalDate;

public record PlayerResponse(
        Long id,
        String name,
        String position,
        String nationality,
        LocalDate dateOfBirth,
        String photoUrl) {

    public static PlayerResponse from(Player p) {
        return new PlayerResponse(p.getId(), p.getName(), p.getPosition(), p.getNationality(), p.getDateOfBirth(), p.getPhotoUrl());
    }
}
