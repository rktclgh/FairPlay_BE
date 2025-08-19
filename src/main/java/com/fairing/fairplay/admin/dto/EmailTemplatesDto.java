package com.fairing.fairplay.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailTemplatesDto {
    private Long id;
    private String name;
    private String content;
}
