package com.naviline.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Ответ на команду запроса баланса датчика")
public record BalanceCommandResponseDto(
        @Schema(description = "IMEI датчика", example = "868184064454575")
        String imei,

        @Schema(description = "Статус отправки команды", example = "COMMAND_SENT")
        String status,

        @Schema(description = "Информационное сообщение", example = "Команда *?USSD 1 *100#* успешно поставлена в очередь FortMonitor")
        String message,

        @Schema(description = "Последний зафиксированный баланс в БД", example = "154.20")
        BigDecimal lastKnownBalance,

        @Schema(description = "Время последней фиксации баланса в БД")
        LocalDateTime lastUpdatedAt
) {}