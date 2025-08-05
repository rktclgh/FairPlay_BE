package com.fairing.fairplay.attendee.repository;

import com.fairing.fairplay.attendee.entity.AttendeeTypeCode;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface AttendeeTypeCodeRepository extends CrudRepository<AttendeeTypeCode, Long> {
  Optional<AttendeeTypeCode> findByCode(String code);
  Optional<AttendeeTypeCode> findById(Integer id);
}
