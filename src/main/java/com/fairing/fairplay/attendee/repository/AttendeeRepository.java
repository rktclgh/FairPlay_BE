package com.fairing.fairplay.attendee.repository;

import com.fairing.fairplay.attendee.entity.Attendee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendeeRepository extends JpaRepository<Attendee, Long> {
}
