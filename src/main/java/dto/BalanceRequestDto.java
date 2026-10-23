package dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BalanceRequestDto {
    @NotNull(message = "IMEI обязателен")
    @Size(min = 1, max = 20, message = "IMEI должен быть 1–20 символов")
    private String imei;

    @NotNull(message = "Баланс обязателен")
    @DecimalMin(value = "0.0", inclusive = true, message = "Баланс не может быть отрицательным")
    private BigDecimal balance;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    // геттеры, сеттеры


    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
