package com.fairing.fairplay.history.dto;

import java.time.LocalDateTime;

import com.fairing.fairplay.history.entity.ChangeHistory;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChangeHistoryDto {
    private String name;
    private String email;
    private String content;
    private String nickname;
    private LocalDateTime modifyTime;
    private String targetType;

    public ChangeHistoryDto(ChangeHistory entity) {
        this.name = entity.getUser().getName();
        this.email = entity.getUser().getEmail();
        this.content = entity.getContent();
        this.nickname = entity.getUser().getNickname();
        this.modifyTime = entity.getModifyTime();
        this.targetType = entity.getTargetType();
    }

}
