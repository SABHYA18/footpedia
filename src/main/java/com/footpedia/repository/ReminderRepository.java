package com.footpedia.repository;

import com.footpedia.domain.Reminder;
import com.footpedia.domain.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    Optional<Reminder> findByUserRefAndFixtureId(String userRef, Long fixtureId);

    List<Reminder> findByUserRefOrderByNotifyAtAsc(String userRef);

    List<Reminder> findByStatus(ReminderStatus status);

    @Modifying
    @Query("delete from Reminder r where r.userRef = :userRef")
    void deleteByUserRef(String userRef);
}
