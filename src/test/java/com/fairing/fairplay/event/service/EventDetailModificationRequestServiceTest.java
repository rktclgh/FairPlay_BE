package com.fairing.fairplay.event.service;

import com.fairing.fairplay.core.service.LocalFileService;
import com.fairing.fairplay.event.dto.EventDetailModificationDto;
import com.fairing.fairplay.event.dto.EventDetailRequestDto;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.entity.EventDetail;
import com.fairing.fairplay.event.entity.EventDetailModificationRequest;
import com.fairing.fairplay.event.entity.UpdateStatusCode;
import com.fairing.fairplay.event.repository.EventDetailModificationRequestRepository;
import com.fairing.fairplay.event.repository.EventDetailRepository;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.event.repository.EventStatusCodeRepository;
import com.fairing.fairplay.event.repository.ExternalLinkRepository;
import com.fairing.fairplay.event.repository.LocationRepository;
import com.fairing.fairplay.event.repository.MainCategoryRepository;
import com.fairing.fairplay.event.repository.RegionCodeRepository;
import com.fairing.fairplay.event.repository.SubCategoryRepository;
import com.fairing.fairplay.event.repository.UpdateStatusCodeRepository;
import com.fairing.fairplay.file.service.FileService;
import com.fairing.fairplay.user.repository.EventAdminRepository;
import com.fairing.fairplay.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventDetailModificationRequestServiceTest {

    @Mock
    private EventDetailModificationRequestRepository modificationRequestRepository;

    @Mock
    private UpdateStatusCodeRepository updateStatusCodeRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventDetailRepository eventDetailRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private MainCategoryRepository mainCategoryRepository;

    @Mock
    private SubCategoryRepository subCategoryRepository;

    @Mock
    private RegionCodeRepository regionCodeRepository;

    @Mock
    private EventAdminRepository eventAdminRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExternalLinkRepository externalLinkRepository;

    @Mock
    private EventStatusCodeRepository statusCodeRepository;

    @Mock
    private EventVersionService eventVersionService;

    @Mock
    private LocalFileService localFileService;

    @Mock
    private FileService fileService;

    private EventDetailModificationRequestService service;

    @BeforeEach
    void setUp() {
        service = new EventDetailModificationRequestService(
                modificationRequestRepository,
                updateStatusCodeRepository,
                eventRepository,
                eventDetailRepository,
                locationRepository,
                mainCategoryRepository,
                subCategoryRepository,
                regionCodeRepository,
                eventAdminRepository,
                userRepository,
                externalLinkRepository,
                statusCodeRepository,
                eventVersionService,
                localFileService,
                fileService
        );
    }

    @Test
    void createModificationRequestReplacesPreviewDownloadUrlAfterMovingPrivateTempFile() {
        String tempKey = "private/tmp/2026-05-18/banner.png";
        String previewUrl = "/api/uploads/download?key=private%2Ftmp%2F2026-05-18%2Fbanner.png";
        String permanentKey = "uploads/events/42/content_image/banner.png";
        String permanentUrl = "https://fair-play.test/uploads/" + permanentKey;

        Event event = eventWithDetail(42L);
        EventDetailModificationDto dto = new EventDetailModificationDto();
        dto.setContent("<img src=\"" + previewUrl + "\">");
        dto.setTempFiles(List.of(fileUpload(tempKey, "content_image")));

        when(eventRepository.findById(42L)).thenReturn(Optional.of(event));
        when(modificationRequestRepository.existsPendingRequestByEventId(42L)).thenReturn(false);
        when(localFileService.moveToPermanent(tempKey, "events/42/content_image")).thenReturn(permanentKey);
        when(localFileService.getCdnUrl(permanentKey)).thenReturn(permanentUrl);
        lenient().when(updateStatusCodeRepository.findByCode("PENDING")).thenReturn(Optional.of(pendingStatus()));
        lenient().when(externalLinkRepository.findByEvent(event)).thenReturn(List.of());
        lenient().when(modificationRequestRepository.save(any(EventDetailModificationRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> service.createModificationRequest(42L, dto, 100L))
                .doesNotThrowAnyException();

        ArgumentCaptor<EventDetailModificationRequest> requestCaptor =
                ArgumentCaptor.forClass(EventDetailModificationRequest.class);
        verify(modificationRequestRepository).save(requestCaptor.capture());
        EventDetailModificationDto savedDto = requestCaptor.getValue().getModifiedDataAsDto();
        assertThat(savedDto.getContent()).contains(permanentUrl);
        assertThat(savedDto.getContent()).doesNotContain(previewUrl);
        verify(localFileService, never()).getCdnUrl(tempKey);
    }

    private Event eventWithDetail(Long eventId) {
        Event event = new Event();
        event.setEventId(eventId);
        event.setTitleKr("행사");
        event.setTitleEng("Event");

        EventDetail detail = new EventDetail();
        detail.setContent("current content");
        detail.setPolicy("current policy");
        detail.setStartDate(LocalDate.of(2026, 5, 18));
        detail.setEndDate(LocalDate.of(2026, 5, 19));
        detail.setReentryAllowed(true);
        detail.setCheckInAllowed(false);
        detail.setCheckOutAllowed(false);
        detail.setAge(false);
        event.setEventDetail(detail);
        return event;
    }

    private EventDetailRequestDto.FileUploadDto fileUpload(String key, String usage) {
        EventDetailRequestDto.FileUploadDto fileUpload = new EventDetailRequestDto.FileUploadDto();
        fileUpload.setS3Key(key);
        fileUpload.setOriginalFileName("banner.png");
        fileUpload.setUsage(usage);
        return fileUpload;
    }

    private UpdateStatusCode pendingStatus() {
        UpdateStatusCode status = new UpdateStatusCode();
        status.setCode("PENDING");
        status.setName("대기");
        return status;
    }
}
