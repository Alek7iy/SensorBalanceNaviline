package com.naviline.project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naviline.project.dto.BalanceCommandResponseDto;
import com.naviline.project.entity.Balance;
import com.naviline.project.repository.BalanceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TrackerBalanceService {

    private final BalanceRepository balanceRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${fortmonitor.url:https://f4.naviline.org}")
    private String fmUrl;

    // Новый активный SessionId
    @Value("${fortmonitor.session-id:672d778a-692d-4b19-9232-ee58c0f93a02}")
    private String sessionId;

    private static final Pattern BALANCE_PATTERN = Pattern.compile("[-+]?\\d+(?:[.,]\\d+)?");

    public TrackerBalanceService(BalanceRepository balanceRepository) {
        this.balanceRepository = balanceRepository;
    }

    public BalanceCommandResponseDto fetchLiveBalanceFromNetwork(String imei) {
        long objectId = 329L;

        long lastKnownId = getLatestMessageId();
        System.out.println(">>> [INIT] Последний известный ID в FM: " + lastKnownId);

        // Отправляем точную команду
        sendUssdCommand(objectId, "*100#");
        System.out.println(">>> [CMD] Команда отправлена для objectId=" + objectId);

        // 35 попыток по 2 секунды = 70 секунд (с запасом для сотовой сети)
        String rawUssdReply = pollForCommandReply(lastKnownId, 35, 2000);

        if (rawUssdReply == null) {
            throw new IllegalStateException("Таймаут: терминал или сеть МегаФон не прислали ответ на команду USSD вовремя");
        }

        BigDecimal realBalance = parseBalance(rawUssdReply);
        saveBalanceToDb(imei, realBalance);

        return new BalanceCommandResponseDto(
                imei,
                "SUCCESS",
                "Актуальный баланс получен: " + rawUssdReply,
                realBalance,
                LocalDateTime.now()
        );
    }

    private long getLatestMessageId() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("SessionId", sessionId);

            // Запрашиваем без date, только последние 10 сообщений
            String urlStr = String.format("%s/api/integration/v1/getcommandmsglimit?limit=10", fmUrl);
            URI uri = URI.create(urlStr);

            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode msgs = root.path("msgs");
                long maxId = 0;
                if (msgs.isArray()) {
                    for (JsonNode m : msgs) {
                        long id = m.path("id").asLong(0);
                        if (id > maxId) {
                            maxId = id;
                        }
                    }
                }
                return maxId;
            }
        } catch (Exception e) {
            System.err.println(">>> Ошибка при получении initial ID: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    private void sendUssdCommand(long objectId, String ussdCode) {
        // Точный синтаксис Навтелеком из NTC Configurator: *?USSD:*100#
        String commandText = "*?USSD:" + ussdCode;
        String encodedMessage = URLEncoder.encode(commandText, StandardCharsets.UTF_8);

        String urlStr = String.format("%s/api/integration/v1/createcommandmsg?object_id=%d&type=1&message=%s&channel=2",
                fmUrl, objectId, encodedMessage);

        HttpHeaders headers = new HttpHeaders();
        headers.set("SessionId", sessionId);

        ResponseEntity<String> response = restTemplate.exchange(
                URI.create(urlStr),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Ошибка отправки команды в FortMonitor: " + response.getStatusCode());
        }
    }

    private String pollForCommandReply(long minId, int maxAttempts, long delayMs) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("SessionId", sessionId);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        String urlStr = String.format("%s/api/integration/v1/getcommandmsglimit?limit=20", fmUrl);
        URI uri = URI.create(urlStr);

        for (int i = 0; i < maxAttempts; i++) {
            try {
                TimeUnit.MILLISECONDS.sleep(delayMs);

                ResponseEntity<String> response = restTemplate.exchange(
                        uri,
                        HttpMethod.GET,
                        entity,
                        String.class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode msgs = root.path("msgs");

                    if (msgs.isArray() && !msgs.isEmpty()) {
                        for (JsonNode msg : msgs) {
                            long id = msg.path("id").asLong(0);
                            String source = msg.path("source").asText("");
                            String text = msg.path("text").asText("");
                            String type = msg.path("type").asText("");

                            // Логируем все пакеты для отладки
                            System.out.println(">>> [DEBUG FM] id=" + id + ", source=[" + source + "], type=[" + type + "], text=[" + text + "]");

                            if (id > minId && "incoming".equalsIgnoreCase(source)) {
                                // Пропускаем служебные пакеты идентификации
                                if (text.startsWith("*#S:") || text.startsWith("*#ICCID")) {
                                    continue;
                                }

                                // Если пришел ответ на USSD
                                if (text.contains("USSD") || text.toLowerCase().contains("баланс")) {
                                    System.out.println(">>> [POLL SUCCESS] Баланс получен: " + text);
                                    return text;
                                }
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println(">>> [POLL ERROR]: " + e.getMessage());
            }
        }
        return null;
    }

    private BigDecimal parseBalance(String text) {
        if (text.contains("Неизвестный номер") || text.toLowerCase().contains("ошибка")) {
            throw new IllegalStateException("Оператор или модем вернул ошибку: " + text);
        }

        Matcher matcher = BALANCE_PATTERN.matcher(text);
        while (matcher.find()) {
            String candidate = matcher.group().replace(',', '.');
            try {
                BigDecimal value = new BigDecimal(candidate);
                // Баланс мобильного не может быть больше 1 000 000 и не должен быть равен 15-значному IMEI
                if (value.abs().compareTo(new BigDecimal("1000000")) < 0) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        throw new IllegalArgumentException("Не удалось выделить корректный баланс из строки: " + text);
    }

    @Transactional
    public void saveBalanceToDb(String imei, BigDecimal amount) {
        Balance balance = balanceRepository.findByImei(imei)
                .orElseGet(() -> new Balance(imei, amount));
        balance.setBalance(amount);
        balance.setUpdatedAt(LocalDateTime.now());
        balanceRepository.save(balance);
    }
}