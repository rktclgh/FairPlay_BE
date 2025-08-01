/*package com.fairing.fairplay.banner.service;

import com.fairing.fairplay.banner.dto.*;
import com.fairing.fairplay.banner.entity.*;
import com.fairing.fairplay.banner.repository.*;
import com.fairing.fairplay.admin.entity.AdminAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;
    private final BannerStatusCodeRepository bannerStatusCodeRepository;
    private final BannerActionCodeRepository bannerActionCodeRepository;
    private final BannerLogRepository bannerLogRepository;

    // 배너 등록
    @Transactional
    public BannerResponseDto createBanner(BannerRequestDto dto, AdminAccount admin) {
        BannerStatusCode statusCode = getStatusCode(dto.getStatusCode());

        Banner banner = new Banner(
                dto.getTitle(),
                dto.getImageUrl(),
                dto.getLinkUrl(),
                dto.getPriority(),
                dto.getStartDate(),
                dto.getEndDate(),
                statusCode
        );

        Banner saved = bannerRepository.save(banner);
        logBannerAction(saved, admin, "CREATE");

        return toDto(saved);
    }

    // 배너 수정
    @Transactional
    public BannerResponseDto updateBanner(Long bannerId, BannerRequestDto dto, AdminAccount admin) {
        Banner banner = getBanner(bannerId);
        BannerStatusCode statusCode = getStatusCode(dto.getStatusCode());

        banner.updateInfo(dto.getTitle(), dto.getImageUrl(), dto.getLinkUrl(),
                dto.getStartDate(), dto.getEndDate(), dto.getPriority());
        banner.updateStatus(statusCode);

        logBannerAction(banner, admin, "UPDATE");

        return toDto(banner);
    }

    // 상태 전환
    @Transactional
    public void changeStatus(Long bannerId, BannerStatusUpdateDto dto, AdminAccount admin) {
        Banner banner = getBanner(bannerId);
        BannerStatusCode statusCode = getStatusCode(dto.getStatusCode());

        banner.updateStatus(statusCode);
        logBannerAction(banner, admin, "UPDATE");
    }

    // 우선순위 변경
    @Transactional
    public void changePriority(Long bannerId, BannerPriorityUpdateDto dto, AdminAccount admin) {
        Banner banner = getBanner(bannerId);
        banner.updatePriority(dto.getPriority());

        logBannerAction(banner, admin, "PRIORITY_CHANGE");
    }

    // 홈화면 배너 목록 조회
    @Transactional(readOnly = true)
    public List<BannerResponseDto> getAllActiveBanners() {
        LocalDateTime now = LocalDateTime.now();
        return bannerRepository
                .findAllByBannerStatusCode_CodeAndStartDateBeforeAndEndDateAfterOrderByPriorityDescStartDateDesc("ACTIVE", now, now)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // 배너 목록 조회(조건 x, 모든)
    @Transactional(readOnly = true)
    public List<BannerResponseDto> getAllBanners() {
        return bannerRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private void logBannerAction(Banner banner, AdminAccount admin, String actionCodeStr) {
        BannerActionCode actionCode = bannerActionCodeRepository.findByCode(actionCodeStr)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배너 액션 코드: " + actionCodeStr));

        BannerLog log = BannerLog.builder()
                .banner(banner)
                .changedBy(admin)
                .actionCode(actionCode)
                .build();

        bannerLogRepository.save(log);
    }

    private Banner getBanner(Long id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배너 ID: " + id));
    }

    private BannerStatusCode getStatusCode(String code) {
        return bannerStatusCodeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상태 코드: " + code));
    }

    private BannerResponseDto toDto(Banner banner) {
        return BannerResponseDto.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .imageUrl(banner.getImageUrl())
                .linkUrl(banner.getLinkUrl())
                .priority(banner.getPriority())
                .startDate(banner.getStartDate())
                .endDate(banner.getEndDate())
                .statusCode(banner.getBannerStatusCode().getCode())
                .build();
    }
}
*/
        package com.fairing.fairplay.banner.service;

import com.fairing.fairplay.banner.dto.*;
        import com.fairing.fairplay.banner.entity.*;
        import com.fairing.fairplay.banner.repository.*;
        import com.fairing.fairplay.admin.entity.AdminAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;
    private final BannerStatusCodeRepository bannerStatusCodeRepository;
    private final BannerActionCodeRepository bannerActionCodeRepository;
    private final BannerLogRepository bannerLogRepository;

    // 배너 등록
    @Transactional
    public BannerResponseDto createBanner(BannerRequestDto dto, Long adminId) {
        BannerStatusCode statusCode = getStatusCode(dto.getStatusCode());

        Banner banner = new Banner(
                dto.getTitle(),
                dto.getImageUrl(),
                dto.getLinkUrl(),
                dto.getPriority(),
                dto.getStartDate(),
                dto.getEndDate(),
                statusCode
        );

        Banner saved = bannerRepository.save(banner);
        logBannerAction(saved, adminId, "CREATE");

        return toDto(saved);
    }

    // 배너 수정
    @Transactional
    public BannerResponseDto updateBanner(Long bannerId, BannerRequestDto dto, Long adminId) {
        Banner banner = getBanner(bannerId);
        BannerStatusCode statusCode = getStatusCode(dto.getStatusCode());

        banner.updateInfo(dto.getTitle(), dto.getImageUrl(), dto.getLinkUrl(),
                dto.getStartDate(), dto.getEndDate(), dto.getPriority());
        banner.updateStatus(statusCode);

        logBannerAction(banner, adminId, "UPDATE");

        return toDto(banner);
    }

    // 상태 전환
    @Transactional
    public void changeStatus(Long bannerId, BannerStatusUpdateDto dto, Long adminId) {
        Banner banner = getBanner(bannerId);
        BannerStatusCode statusCode = getStatusCode(dto.getStatusCode());

        banner.updateStatus(statusCode);
        logBannerAction(banner, adminId, "UPDATE");
    }

    // 우선순위 변경
    @Transactional
    public void changePriority(Long bannerId, BannerPriorityUpdateDto dto, Long adminId) {
        Banner banner = getBanner(bannerId);
        banner.updatePriority(dto.getPriority());

        logBannerAction(banner, adminId, "PRIORITY_CHANGE");
    }

    // 홈화면 배너 목록 조회
    @Transactional(readOnly = true)
    public List<BannerResponseDto> getAllActiveBanners() {
        LocalDateTime now = LocalDateTime.now();
        return bannerRepository
                .findAllByBannerStatusCode_CodeAndStartDateBeforeAndEndDateAfterOrderByPriorityDescStartDateDesc("ACTIVE", now, now)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // 배너 목록 조회(조건 x, 모든)
    @Transactional(readOnly = true)
    public List<BannerResponseDto> getAllBanners() {
        return bannerRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private void logBannerAction(Banner banner, Long adminId, String actionCodeStr) {
        BannerActionCode actionCode = bannerActionCodeRepository.findByCode(actionCodeStr)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배너 액션 코드: " + actionCodeStr));

        // adminId만으로 proxy admin 객체 생성
        AdminAccount proxyAdmin = new AdminAccount(adminId);

        BannerLog log = BannerLog.builder()
                .banner(banner)
                .changedBy(proxyAdmin)
                .actionCode(actionCode)
                .build();

        bannerLogRepository.save(log);
    }

    private Banner getBanner(Long id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배너 ID: " + id));
    }

    private BannerStatusCode getStatusCode(String code) {
        return bannerStatusCodeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상태 코드: " + code));
    }

    private BannerResponseDto toDto(Banner banner) {
        return BannerResponseDto.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .imageUrl(banner.getImageUrl())
                .linkUrl(banner.getLinkUrl())
                .priority(banner.getPriority())
                .startDate(banner.getStartDate())
                .endDate(banner.getEndDate())
                .statusCode(banner.getBannerStatusCode().getCode())
                .build();
    }
}
