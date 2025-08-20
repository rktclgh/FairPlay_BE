package com.fairing.fairplay.admin.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fairing.fairplay.admin.dto.AdminAuthDto;
import com.fairing.fairplay.admin.dto.FunctionNameDto;
import com.fairing.fairplay.admin.entity.AccountLevel;
import com.fairing.fairplay.admin.entity.EmailTemplates;
import com.fairing.fairplay.admin.entity.FunctionLevel;
import com.fairing.fairplay.admin.repository.AccountLevelRepository;
import com.fairing.fairplay.admin.repository.EmailTemplatesRepository;
import com.fairing.fairplay.admin.repository.FunctionLevelRepository;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.etc.FunctionLevelEnum;
import com.fairing.fairplay.history.dto.ChangeHistoryDto;
import com.fairing.fairplay.history.dto.LoginHistoryDto;
import com.fairing.fairplay.history.entity.ChangeHistory;
import com.fairing.fairplay.history.entity.LoginHistory;
import com.fairing.fairplay.history.etc.ChangeHistorySpec;
import com.fairing.fairplay.history.etc.LoginHistorySpec;
import com.fairing.fairplay.history.repository.ChangeHistoryRepository;
import com.fairing.fairplay.history.repository.LoginHistoryRepository;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;

import jakarta.annotation.PostConstruct;

@Service
public class SuperAdminService {

    private static final String USER_NOT_FOUND_MSG = "사용자를 찾을 수 없습니다.";

    private final UserRepository userRepository;
    private final AccountLevelRepository accountLevelRepository;
    private final FunctionLevelRepository functionLevelRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final ChangeHistoryRepository changeHistoryRepository;
    private final LevelService levelService;
    private final EmailTemplatesRepository emailTemplatesRepository;

    public SuperAdminService(UserRepository userRepository,
            LevelService levelService,
            AccountLevelRepository accountLevelRepository, FunctionLevelRepository functionLevelRepository,
            LoginHistoryRepository loginHistoryRepository, ChangeHistoryRepository changeHistoryRepository,
            EmailTemplatesRepository emailTemplatesRepository) {
        this.changeHistoryRepository = changeHistoryRepository;
        this.userRepository = userRepository;
        this.accountLevelRepository = accountLevelRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.functionLevelRepository = functionLevelRepository;
        this.emailTemplatesRepository = emailTemplatesRepository;
        this.levelService = levelService;
    }

    public void disableUser(Long targetId) {
        Users user = userRepository.findById(targetId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_MSG));
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public void setEventAdmin(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_MSG));
        AccountLevel accountLevel = new AccountLevel();
        accountLevel.setUser(user);
        BigDecimal decimal = new BigDecimal(BigInteger.ONE.shiftLeft(100).subtract(BigInteger.ONE));
        accountLevel.setLevel(decimal);
        accountLevelRepository.save(accountLevel);

    }

    public void setSuperAdmin(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_MSG));
        AccountLevel accountLevel = new AccountLevel();
        accountLevel.setUser(user);
        BigDecimal decimal = new BigDecimal(BigInteger.ONE.shiftLeft(200).subtract(BigInteger.ONE));
        accountLevel.setLevel(decimal);
        accountLevelRepository.save(accountLevel);
    }

    public void setBoothAdmin(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_MSG));
        AccountLevel accountLevel = new AccountLevel();
        accountLevel.setUser(user);
        BigDecimal decimal = new BigDecimal(BigInteger.ONE.shiftLeft(6).subtract(BigInteger.ONE));
        accountLevel.setLevel(decimal);
        accountLevelRepository.save(accountLevel);
    }

    public void modifyAuth(Long userId, List<String> authList) {
        List<FunctionLevel> functionLevels = functionLevelRepository.findAll();
        BigInteger originValue = BigInteger.ZERO;
        for (FunctionLevel functionLevel : functionLevels) {
            originValue = originValue.add(functionLevel.getLevel().toBigInteger());
        }
        BigInteger accountLevel = accountLevelRepository.findByUserId(userId).getLevel().toBigInteger();
        accountLevel = accountLevel.andNot(originValue);
        for (String auth : authList) {
            accountLevel = accountLevel.or(FunctionLevelEnum.fromFunctionName(auth).getBit());
        }
        AccountLevel accountLevelEntity = accountLevelRepository.findByUserId(userId);
        accountLevelEntity.setLevel(new BigDecimal(accountLevel));
        accountLevelRepository.save(accountLevelEntity);
    }

    public Page<LoginHistoryDto> getLoginLogs(int page, int size, String email, LocalDateTime from, LocalDateTime to) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Specification<LoginHistory> spec = Specification.where(LoginHistorySpec.searchByEmail(email))
                .and(LoginHistorySpec.searchByTime(from, to));

        Page<LoginHistoryDto> loginHistories = loginHistoryRepository.findAll(spec, pageable)
                .map(c -> new LoginHistoryDto(c));
        return loginHistories;
    }

    public Page<ChangeHistoryDto> getChangeLogs(int page, int size, String email, String type,
            LocalDateTime from, LocalDateTime to) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Specification<ChangeHistory> spec = Specification.where(ChangeHistorySpec.searchByEmail(email))
                .and(ChangeHistorySpec.searchByTargetType(type))
                .and(ChangeHistorySpec.searchByTime(from, to));
        Page<ChangeHistoryDto> changeHistories = changeHistoryRepository.findAll(spec, pageable)
                .map(c -> new ChangeHistoryDto(c));
        return changeHistories;
    }

    public List<AdminAuthDto> getAdmins() {
        List<Users> users = userRepository.findAdmin();

        List<FunctionLevel> functionLevels = functionLevelRepository.findAll();
        List<AdminAuthDto> adminAuthDtos = new ArrayList<>();

        for (Users user : users) {
            List<String> auths = new ArrayList<>();
            BigInteger accountLevel = levelService.getAccountLevel(user.getUserId());

            for (FunctionLevel functionLevel : functionLevels) {
                if (accountLevel.and(functionLevel.getLevel().toBigInteger())
                        .equals(functionLevel.getLevel().toBigInteger())) {
                    auths.add(functionLevel.getFunctionName());
                }
            }
            AdminAuthDto dto = new AdminAuthDto();
            dto.setUserId(user.getUserId());
            dto.setRole(user.getRoleCode().getName());
            dto.setNickname(user.getNickname());
            dto.setEmail(user.getEmail());
            dto.setAuthList(auths);
            adminAuthDtos.add(dto);
        }
        return adminAuthDtos;

    }

    public List<FunctionNameDto> getAuthList() {
        List<FunctionLevel> functionLevels = functionLevelRepository.findAll();
        List<FunctionNameDto> functionNameDtos = new ArrayList<>();

        for (FunctionLevel functionLevel : functionLevels) {
            FunctionNameDto dto = new FunctionNameDto();
            dto.setFunctionName(functionLevel.getFunctionName());
            dto.setFunctionNameKr(functionLevel.getFunctionNameKr());
            functionNameDtos.add(dto);
        }
        return functionNameDtos;
    }

    // public List<String> getTemplateList() throws IOException {
    // Path userTemplateDir = Paths.get("user-templates/email");
    // return Files.list(userTemplateDir)
    // .filter(path -> path.toString().endsWith(".html"))
    // .map(path -> path.getFileName().toString())
    // .toList();
    // }

    // public String getTemplate(String name) throws IOException {
    // Path userTemplateDir = Paths.get("user-templates/email");
    // Path templatePath = userTemplateDir.resolve(name);
    // return new String(Files.readString(templatePath, StandardCharsets.UTF_8));
    // }

    // public void saveTemplate(String name, String content) throws IOException {
    // Path userTemplateDir = Paths.get("user-templates/email");
    // Path templatePath = userTemplateDir.resolve(name);
    // Files.writeString(templatePath, content, StandardCharsets.UTF_8);
    // }

    public List<String> getTemplateList() {
        List<EmailTemplates> templates = emailTemplatesRepository.findAll();
        return templates.stream()
                .map(EmailTemplates::getName)
                .collect(Collectors.toList());
    }

    public String getTemplate(String name) {
        EmailTemplates template = emailTemplatesRepository.findByName(name);
        if (template == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "템플릿이 없음");
        }
        return template.getContent();
    }

    public void saveTemplate(String name, String content) {
        EmailTemplates template = emailTemplatesRepository.findByName(name);
        template.setContent(content);
        emailTemplatesRepository.save(template);
    }

    @PostConstruct
    public void createDefaultTemplates() throws Exception {
        List<EmailTemplates> defaultTemplates = emailTemplatesRepository.findAll();
        if (!defaultTemplates.isEmpty())
            return;
        Resource resource = new ClassPathResource("email");
        if (!resource.exists())
            return;
        Resource[] templates = new PathMatchingResourcePatternResolver()
                .getResources("classpath:email/*.html");
        for (Resource r : templates) {
            EmailTemplates template = new EmailTemplates();
            template.setName(r.getFilename());
            template.setContent(new String(Files.readAllBytes(r.getFile().toPath()), StandardCharsets.UTF_8));
            defaultTemplates.add(template);
            emailTemplatesRepository.save(template);
        }
    }

    // public void setAuth(Long userId, BigInteger functionLevel) {
    // Users user = userRepository.findById(userId)
    // .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
    // USER_NOT_FOUND_MSG));
    // BigInteger accountLevel = levelService.getAccountLevel(userId);
    // }

}
