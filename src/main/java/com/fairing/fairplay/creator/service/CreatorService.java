package com.fairing.fairplay.creator.service;

import com.fairing.fairplay.creator.dto.CreatorRequestDto;
import com.fairing.fairplay.creator.dto.CreatorResponseDto;
import com.fairing.fairplay.creator.entity.Creator;
import com.fairing.fairplay.creator.repository.CreatorRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreatorService {

    private final CreatorRepository creatorRepository;
    private final ObjectMapper objectMapper;

    /**
     * 모든 제작자 조회 (관리자용 - 비활성화 포함)
     */
    @Transactional(readOnly = true)
    public List<CreatorResponseDto> getAllCreators() {
        List<Creator> creators = creatorRepository.findAllByOrderByDisplayOrderAsc();
        return creators.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 활성화된 제작자만 조회 (일반 사용자용)
     */
    @Transactional(readOnly = true)
    public List<CreatorResponseDto> getActiveCreators() {
        List<Creator> creators = creatorRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        return creators.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 제작자 조회
     */
    @Transactional(readOnly = true)
    public CreatorResponseDto getCreatorById(Long id) {
        Creator creator = creatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("제작자를 찾을 수 없습니다. ID: " + id));
        return toResponseDto(creator);
    }

    /**
     * 제작자 생성
     */
    @Transactional
    public CreatorResponseDto createCreator(CreatorRequestDto requestDto) {
        Creator creator = Creator.builder()
                .name(requestDto.getName())
                .email(requestDto.getEmail())
                .profileImageUrl(requestDto.getProfileImageUrl())
                .role(requestDto.getRole())
                .bio(requestDto.getBio())
                .responsibilities(serializeList(requestDto.getResponsibilities()))
                .githubUrl(requestDto.getGithubUrl())
                .linkedinUrl(requestDto.getLinkedinUrl())
                .instagramUrl(requestDto.getInstagramUrl())
                .twitterUrl(requestDto.getTwitterUrl())
                .websiteUrl(requestDto.getWebsiteUrl())
                .displayOrder(requestDto.getDisplayOrder())
                .isActive(requestDto.getIsActive() != null ? requestDto.getIsActive() : true)
                .build();

        Creator saved = creatorRepository.save(creator);
        log.info("제작자 생성 완료: {}", saved.getCreatorId());
        return toResponseDto(saved);
    }

    /**
     * 제작자 수정
     */
    @Transactional
    public CreatorResponseDto updateCreator(Long id, CreatorRequestDto requestDto) {
        Creator creator = creatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("제작자를 찾을 수 없습니다. ID: " + id));

        creator.setName(requestDto.getName());
        creator.setEmail(requestDto.getEmail());
        creator.setProfileImageUrl(requestDto.getProfileImageUrl());
        creator.setRole(requestDto.getRole());
        creator.setBio(requestDto.getBio());
        creator.setResponsibilities(serializeList(requestDto.getResponsibilities()));
        creator.setGithubUrl(requestDto.getGithubUrl());
        creator.setLinkedinUrl(requestDto.getLinkedinUrl());
        creator.setInstagramUrl(requestDto.getInstagramUrl());
        creator.setTwitterUrl(requestDto.getTwitterUrl());
        creator.setWebsiteUrl(requestDto.getWebsiteUrl());
        creator.setDisplayOrder(requestDto.getDisplayOrder());
        creator.setIsActive(requestDto.getIsActive());

        Creator updated = creatorRepository.save(creator);
        log.info("제작자 수정 완료: {}", updated.getCreatorId());
        return toResponseDto(updated);
    }

    /**
     * 제작자 삭제
     */
    @Transactional
    public void deleteCreator(Long id) {
        if (!creatorRepository.existsById(id)) {
            throw new RuntimeException("제작자를 찾을 수 없습니다. ID: " + id);
        }
        creatorRepository.deleteById(id);
        log.info("제작자 삭제 완료: {}", id);
    }

    /**
     * Entity -> DTO 변환
     */
    private CreatorResponseDto toResponseDto(Creator creator) {
        return CreatorResponseDto.builder()
                .id(creator.getCreatorId())
                .name(creator.getName())
                .email(creator.getEmail())
                .profileImage(creator.getProfileImageUrl())
                .role(creator.getRole())
                .bio(creator.getBio())
                .responsibilities(deserializeList(creator.getResponsibilities()))
                .github(creator.getGithubUrl())
                .linkedin(creator.getLinkedinUrl())
                .instagram(creator.getInstagramUrl())
                .twitter(creator.getTwitterUrl())
                .website(creator.getWebsiteUrl())
                .displayOrder(creator.getDisplayOrder())
                .isActive(creator.getIsActive())
                .createdAt(creator.getCreatedAt())
                .updatedAt(creator.getUpdatedAt())
                .build();
    }

    /**
     * List<String> -> JSON String 변환
     */
    private String serializeList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("JSON 직렬화 실패", e);
            return null;
        }
    }

    /**
     * JSON String -> List<String> 변환
     */
    private List<String> deserializeList(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("JSON 역직렬화 실패", e);
            return new ArrayList<>();
        }
    }
}
