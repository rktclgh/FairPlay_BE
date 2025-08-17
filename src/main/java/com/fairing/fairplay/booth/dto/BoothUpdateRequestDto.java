package com.fairing.fairplay.booth.dto;

import com.fairing.fairplay.file.dto.TempFileUploadDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class BoothUpdateRequestDto {
    private String boothTitle;
    private String boothDescription;
    private LocalDate startDate;
    private LocalDate endDate;
    private String location;
    private List<TempFileUploadDto> tempFiles;
    private List<Long> deletedFileIds;
}
