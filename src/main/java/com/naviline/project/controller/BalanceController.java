package com.naviline.project.controller;

import com.naviline.project.dto.BalanceCommandResponseDto;
import com.naviline.project.service.TrackerBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sensors")
@Tag(name = "Баланс SIM-карт", description = "Управление запросом баланса терминалов Навтелеком")
public class BalanceController {

    private final TrackerBalanceService balanceService;

    public BalanceController(TrackerBalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @Operation(
            summary = "Отправить USSD-запрос баланса (*100#)",
            description = "Формирует команду *?USSD 1 *100#*, передает её по GPRS через FortMonitor на датчик и отдает последний зафиксированный баланс."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Команда успешно отправлена",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BalanceCommandResponseDto.class))
    )
    @GetMapping("/balance")
    public ResponseEntity<BalanceCommandResponseDto> getBalance(
            @Parameter(description = "15-значный IMEI терминала", example = "868184064454575", required = true)
            @RequestParam String imei) {

        BalanceCommandResponseDto response = balanceService.fetchLiveBalanceFromNetwork(imei);
        return ResponseEntity.ok(response);
    }
}