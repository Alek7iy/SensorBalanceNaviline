package com.naviline.project.service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class NavtelecomProtocolHelper {

    /**
     * Сборка пакета команды телетекста NTCB для терминала Навтелеком СМАРТ S-2433
     * без использования библиотек Netty.
     */
    public static byte[] buildUssdCommandPacket(String ussdCode) {
        String commandText = "*USSD " + ussdCode + "*";
        byte[] payload = commandText.getBytes(StandardCharsets.UTF_8);

        // Структура: @NTC (4 байта) + длина (2 байта) + тип (2 байта) + тело + CRC16 (2 байта)
        int totalLength = 4 + 2 + 2 + payload.length + 2;
        ByteBuffer buffer = ByteBuffer.allocate(totalLength).order(ByteOrder.LITTLE_ENDIAN);

        // Преамбула @NTC
        buffer.put(new byte[]{0x40, 0x4E, 0x54, 0x43});

        // Длина поля данных (тип сообщения + тело)
        buffer.putShort((short) (payload.length + 2));

        // Тип сообщения (0x01, 0x00 - телетекст)
        buffer.put((byte) 0x01);
        buffer.put((byte) 0x00);

        // Текст команды
        buffer.put(payload);

        // Расчет CRC16
        byte[] rawBytes = buffer.array();
        int crc = calculateCrc16(rawBytes, 0, totalLength - 2);
        buffer.putShort((short) crc);

        return buffer.array();
    }

    public static String extractUssdResponse(String rawPacketText) {
        if (rawPacketText.contains("*USSD:")) {
            int start = rawPacketText.indexOf("*USSD:") + 6;
            int end = rawPacketText.lastIndexOf("*");
            if (end > start) {
                return rawPacketText.substring(start, end).trim();
            }
            return rawPacketText.substring(start).trim();
        }
        return rawPacketText;
    }

    private static int calculateCrc16(byte[] data, int offset, int length) {
        int crc = 0xFFFF;
        for (int i = offset; i < length; i++) {
            crc ^= (data[i] & 0xFF);
            for (int j = 0; j < 8; j++) {
                if ((crc & 1) != 0) {
                    crc = (crc >>> 1) ^ 0xA001;
                } else {
                    crc = (crc >>> 1);
                }
            }
        }
        return crc & 0xFFFF;
    }
}