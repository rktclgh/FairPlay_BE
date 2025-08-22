package com.fairing.fairplay.temp.dto.host;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StockDto {
    private Integer remainingStock;
    private Integer saleQuantity;
}
