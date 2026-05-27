package com.fairing.fairplay.event.controller;

import com.fairing.fairplay.core.service.SessionService;
import com.fairing.fairplay.event.entity.ApplyStatusCode;
import com.fairing.fairplay.event.entity.EventApply;
import com.fairing.fairplay.event.entity.Location;
import com.fairing.fairplay.event.entity.MainCategory;
import com.fairing.fairplay.event.entity.SubCategory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.datasource.url=jdbc:h2:mem:fairplay-event-apply-osiv-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;NON_KEYWORDS=HOUR",
        "spring.jpa.open-in-view=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.main.lazy-initialization=true"
})
@AutoConfigureMockMvc
class EventApplyOsivFalseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockBean
    private SessionService sessionService;

    @Test
    void checksApplicationStatusWithoutLazyInitializationExceptionWhenOpenInViewIsFalse() throws Exception {
        transactionTemplate.executeWithoutResult(status -> persistEventApplyFixture());

        mockMvc.perform(get("/api/events/apply/check")
                        .param("eventEmail", "apply-osiv@example.com")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("PENDING"))
                .andExpect(jsonPath("$.statusName").value("처리 대기"))
                .andExpect(jsonPath("$.locationName").value("OSIV 테스트홀"))
                .andExpect(jsonPath("$.mainCategoryName").value("박람회"))
                .andExpect(jsonPath("$.subCategoryName").value("IT"));
    }

    private void persistEventApplyFixture() {
        ApplyStatusCode pending = new ApplyStatusCode();
        ReflectionTestUtils.setField(pending, "code", "PENDING");
        ReflectionTestUtils.setField(pending, "name", "처리 대기");
        entityManager.persist(pending);

        MainCategory mainCategory = new MainCategory(10, "박람회");
        entityManager.persist(mainCategory);

        SubCategory subCategory = new SubCategory(101, mainCategory, "IT");
        entityManager.persist(subCategory);

        Location location = new Location();
        location.setAddress("서울시 테스트로 1");
        location.setPlaceName("OSIV 테스트홀");
        location.setLatitude(new BigDecimal("37.5665000"));
        location.setLongitude(new BigDecimal("126.9780000"));
        entityManager.persist(location);

        EventApply eventApply = new EventApply();
        eventApply.setStatusCode(pending);
        eventApply.setEventEmail("apply-osiv@example.com");
        eventApply.setBusinessNumber("123-45-67890");
        eventApply.setBusinessName("OSIV 테스트 주식회사");
        eventApply.setBusinessDate(LocalDate.of(2026, 1, 1));
        eventApply.setVerified(true);
        eventApply.setManagerName("신청 담당자");
        eventApply.setEmail("manager@example.com");
        eventApply.setContactNumber("010-1111-2222");
        eventApply.setTitleKr("OSIV 행사 신청");
        eventApply.setTitleEng("OSIV Event Application");
        eventApply.setFileUrl("");
        eventApply.setBannerUrl("");
        eventApply.setThumbnailUrl("");
        eventApply.setLocation(location);
        eventApply.setLocationDetail("3층");
        eventApply.setStartDate(LocalDate.of(2026, 7, 1));
        eventApply.setEndDate(LocalDate.of(2026, 7, 2));
        eventApply.setMainCategory(mainCategory);
        eventApply.setSubCategory(subCategory);
        entityManager.persist(eventApply);
        entityManager.flush();
        entityManager.clear();
    }
}
