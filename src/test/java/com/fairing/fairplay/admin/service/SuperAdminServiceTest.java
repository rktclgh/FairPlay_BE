package com.fairing.fairplay.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import com.fairing.fairplay.admin.entity.AccountLevel;
import com.fairing.fairplay.admin.entity.FunctionLevel;
import com.fairing.fairplay.admin.repository.AccountLevelRepository;
import com.fairing.fairplay.admin.repository.EmailTemplatesRepository;
import com.fairing.fairplay.admin.repository.FunctionLevelRepository;
import com.fairing.fairplay.core.etc.FunctionLevelEnum;
import com.fairing.fairplay.core.service.UserSessionRevocationService;
import com.fairing.fairplay.history.repository.ChangeHistoryRepository;
import com.fairing.fairplay.history.repository.LoginHistoryRepository;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.AbstractResource;

@ExtendWith(MockitoExtension.class)
class SuperAdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LevelService levelService;

    @Mock
    private AccountLevelRepository accountLevelRepository;

    @Mock
    private FunctionLevelRepository functionLevelRepository;

    @Mock
    private LoginHistoryRepository loginHistoryRepository;

    @Mock
    private ChangeHistoryRepository changeHistoryRepository;

    @Mock
    private EmailTemplatesRepository emailTemplatesRepository;

    @Mock
    private UserSessionRevocationService userSessionRevocationService;

    @Test
    void readsTemplateContentWithoutRequiringFilesystemResource() throws Exception {
        var resource = new InMemoryHtmlResource("<html>ok</html>");

        String content = SuperAdminService.readTemplateContent(resource);

        assertThat(content).isEqualTo("<html>ok</html>");
    }

    @Test
    void disableUserRevokesSessionsAndRefreshToken() {
        Users user = new Users();
        user.setUserId(42L);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        service().disableUser(42L);

        assertThat(user.getDeletedAt()).isNotNull();
        verify(userRepository).save(user);
        verify(userSessionRevocationService).revokeAfterCommit(42L);
    }

    @Test
    void modifyAuthRevokesSessionsAndRefreshToken() {
        FunctionLevel removableLevel = new FunctionLevel();
        removableLevel.setLevel(new BigDecimal(FunctionLevelEnum.DISABLE_USER.getBit()));

        AccountLevel accountLevel = new AccountLevel();
        accountLevel.setLevel(BigDecimal.ZERO);

        when(functionLevelRepository.findAll()).thenReturn(List.of(removableLevel));
        when(accountLevelRepository.findByUserId(42L)).thenReturn(accountLevel);

        service().modifyAuth(42L, List.of(FunctionLevelEnum.GET_USERS.getFunctionName()));

        verify(accountLevelRepository).save(accountLevel);
        verify(userSessionRevocationService).revokeAfterCommit(42L);
    }

    private SuperAdminService service() {
        return new SuperAdminService(
                userRepository,
                levelService,
                accountLevelRepository,
                functionLevelRepository,
                loginHistoryRepository,
                changeHistoryRepository,
                emailTemplatesRepository,
                userSessionRevocationService
        );
    }

    private static class InMemoryHtmlResource extends AbstractResource {
        private final byte[] content;

        InMemoryHtmlResource(String content) {
            this.content = content.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String getDescription() {
            return "in-memory html resource";
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public File getFile() throws IOException {
            throw new FileNotFoundException("jar nested resource");
        }
    }
}
