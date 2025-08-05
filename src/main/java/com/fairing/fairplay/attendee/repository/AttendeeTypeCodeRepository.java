package com.fairing.fairplay.attendee.repository;

import com.fairing.fairplay.attendee.entity.AttendeeTypeCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendeeTypeCodeRepository extends JpaRepository<AttendeeTypeCode, Integer> {
  Optional<AttendeeTypeCode> findByCode(String code);
}
