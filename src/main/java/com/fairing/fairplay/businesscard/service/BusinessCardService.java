package com.fairing.fairplay.businesscard.service;

import com.fairing.fairplay.businesscard.dto.BusinessCardRequestDto;
import com.fairing.fairplay.businesscard.dto.BusinessCardResponseDto;
import com.fairing.fairplay.businesscard.dto.CollectedCardResponseDto;
import com.fairing.fairplay.businesscard.entity.BusinessCard;
import com.fairing.fairplay.businesscard.entity.CollectedBusinessCard;
import com.fairing.fairplay.businesscard.repository.BusinessCardRepository;
import com.fairing.fairplay.businesscard.repository.CollectedBusinessCardRepository;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.event.entity.Location;
import com.fairing.fairplay.event.repository.LocationRepository;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessCardService {

    private final BusinessCardRepository businessCardRepository;
    private final CollectedBusinessCardRepository collectedCardRepository;
    private final UserRepository userRepository;
    private final BusinessCardQRService qrService;
    private final LocationRepository locationRepository;

    /**
     * 사용자의 전자명함 조회 (본인용 - 모든 필드 포함)
     */
    @Transactional(readOnly = true)
    public BusinessCardResponseDto getMyBusinessCard(Long userId) {
        Users user = getUserById(userId);
        BusinessCard businessCard = businessCardRepository.findByUser(user).orElse(null);
        return BusinessCardResponseDto.from(businessCard);
    }

    /**
     * 전자명함 생성/수정
     */
    @Transactional
    public BusinessCardResponseDto saveBusinessCard(Long userId, BusinessCardRequestDto requestDto) {
        Users user = getUserById(userId);
        
        BusinessCard businessCard = businessCardRepository.findByUser(user)
                .orElse(new BusinessCard());
        
        // 새로운 카드인 경우 사용자 설정
        if (businessCard.getCardId() == null) {
            businessCard.setUser(user);
        }
        
        // 필드 업데이트
        updateBusinessCardFields(businessCard, requestDto);
        
        BusinessCard savedCard = businessCardRepository.save(businessCard);
        log.info("전자명함 저장 완료 - 사용자 ID: {}, 카드 ID: {}", userId, savedCard.getCardId());
        
        return BusinessCardResponseDto.from(savedCard);
    }

    /**
     * 공개용 전자명함 조회 (타인이 볼 때 - 비어있는 필드 제외)
     */
    @Transactional(readOnly = true)
    public BusinessCardResponseDto getPublicBusinessCard(Long cardOwnerId) {
        Users cardOwner = getUserById(cardOwnerId);
        BusinessCard businessCard = businessCardRepository.findByUser(cardOwner)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "전자명함을 찾을 수 없습니다."));
        
        return BusinessCardResponseDto.from(businessCard).filterEmptyFields();
    }

    /**
     * QR 코드 생성
     */
    @Transactional(readOnly = true)
    public String generateQRCode(Long userId) {
        Users user = getUserById(userId);
        
        // 전자명함이 있는지 확인
        if (!businessCardRepository.existsByUser(user)) {
            throw new CustomException(HttpStatus.NOT_FOUND, "전자명함을 먼저 생성해주세요.");
        }
        
        return qrService.generateBusinessCardQR(userId);
    }

    /**
     * 전자명함 수집 (QR 스캔)
     */
    @Transactional
    public void collectBusinessCard(Long collectorId, Long cardOwnerId, String memo) {
        if (collectorId.equals(cardOwnerId)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "자신의 명함은 수집할 수 없습니다.");
        }
        
        Users collector = getUserById(collectorId);
        Users cardOwner = getUserById(cardOwnerId);
        
        // 명함이 존재하는지 확인
        if (!businessCardRepository.existsByUser(cardOwner)) {
            throw new CustomException(HttpStatus.NOT_FOUND, "해당 사용자의 전자명함이 존재하지 않습니다.");
        }
        
        // 이미 수집했는지 확인
        if (collectedCardRepository.existsByCollectorAndCardOwner(collector, cardOwner)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "이미 수집한 명함입니다.");
        }
        
        CollectedBusinessCard collected = new CollectedBusinessCard();
        collected.setCollector(collector);
        collected.setCardOwner(cardOwner);
        collected.setMemo(memo);
        
        collectedCardRepository.save(collected);
        log.info("전자명함 수집 완료 - 수집자 ID: {}, 카드 소유자 ID: {}", collectorId, cardOwnerId);
    }

    /**
     * 수집한 명함 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CollectedCardResponseDto> getCollectedCards(Long userId) {
        Users user = getUserById(userId);
        List<CollectedBusinessCard> collectedCards = 
                collectedCardRepository.findByCollectorWithBusinessCard(user);
        
        return collectedCards.stream()
                .map(CollectedCardResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 수집한 명함 삭제
     */
    @Transactional
    public void deleteCollectedCard(Long userId, Long collectedCardId) {
        Users user = getUserById(userId);
        CollectedBusinessCard collected = collectedCardRepository.findById(collectedCardId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "수집한 명함을 찾을 수 없습니다."));
        
        if (!collected.getCollector().getUserId().equals(userId)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }
        
        collectedCardRepository.delete(collected);
        log.info("수집한 명함 삭제 완료 - 사용자 ID: {}, 수집 카드 ID: {}", userId, collectedCardId);
    }

    /**
     * 수집한 명함 메모 수정
     */
    @Transactional
    public void updateCollectedCardMemo(Long userId, Long collectedCardId, String memo) {
        Users user = getUserById(userId);
        CollectedBusinessCard collected = collectedCardRepository.findById(collectedCardId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "수집한 명함을 찾을 수 없습니다."));
        
        if (!collected.getCollector().getUserId().equals(userId)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");
        }
        
        collected.setMemo(memo);
        collectedCardRepository.save(collected);
    }

    private Users getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private void updateBusinessCardFields(BusinessCard businessCard, BusinessCardRequestDto requestDto) {
        businessCard.setName(requestDto.getName());
        businessCard.setCompany(requestDto.getCompany());
        businessCard.setPosition(requestDto.getPosition());
        businessCard.setDepartment(requestDto.getDepartment());
        businessCard.setPhoneNumber(requestDto.getPhoneNumber());
        businessCard.setEmail(requestDto.getEmail());
        businessCard.setWebsite(requestDto.getWebsite());
        businessCard.setDetailAddress(requestDto.getDetailAddress());
        businessCard.setDescription(requestDto.getDescription());
        businessCard.setLinkedIn(requestDto.getLinkedIn());
        businessCard.setInstagram(requestDto.getInstagram());
        businessCard.setFacebook(requestDto.getFacebook());
        businessCard.setTwitter(requestDto.getTwitter());
        businessCard.setGithub(requestDto.getGithub());
        businessCard.setProfileImageUrl(requestDto.getProfileImageUrl());
        
        // Location 처리
        if (requestDto.getAddress() != null && requestDto.getLatitude() != null && requestDto.getLongitude() != null) {
            Location location = new Location();
            location.setAddress(requestDto.getAddress());
            location.setBuildingName(requestDto.getBuildingName());
            location.setPlaceName(requestDto.getPlaceName());
            location.setLatitude(requestDto.getLatitude());
            location.setLongitude(requestDto.getLongitude());
            location.setPlaceUrl(requestDto.getPlaceUrl());
            
            Location savedLocation = locationRepository.save(location);
            businessCard.setLocation(savedLocation);
        } else if (businessCard.getLocation() != null && 
                  (requestDto.getAddress() == null || requestDto.getLatitude() == null || requestDto.getLongitude() == null)) {
            // 기존 location이 있지만 새로운 요청에서 location 정보가 없으면 null로 설정
            businessCard.setLocation(null);
        }
    }
}