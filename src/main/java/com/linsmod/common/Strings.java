package com.linsmod.common;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

public final class Strings {
    public static boolean isNullOrEmpty(String s) {
        return Objects.isNull(s) || s.equals("");
    }

    public static String[] split(String str, String regex, boolean removeEmptyEntries) {
        // 使用Java的String.split方法进行分割
        // -1 作为limit参数意味着不限制返回的数组长度
        String[] splitResult = str.split(regex, -1);

        // 如果需要移除空字符串条目
        if (removeEmptyEntries) {
            // 使用Java 8的Stream API来过滤空字符串
            splitResult = Arrays.stream(splitResult)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
        }
        // 返回结果数组
        return splitResult;
    }

    public static String dateNow(String format) {
        // 将Date转换为Instant
        Instant instant = new Date().toInstant();

        // 假设我们想要使用系统默认时区，则可以使用ZoneId.systemDefault()
        // 如果你知道特定的时区，可以用ZoneId.of("时区ID")来替换
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());

        // 转换为LocalDateTime（这将忽略时区信息，只保留日期和时间）
        LocalDateTime localDateTime = zonedDateTime.toLocalDateTime();

        // 创建DateTimeFormatter对象
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

        // 格式化日期并返回字符串
        return localDateTime.format(formatter);
    }

    public static String date(Date d, String format) {
        // 将Date转换为Instant
        Instant instant = d.toInstant();

        // 假设我们想要使用系统默认时区，则可以使用ZoneId.systemDefault()
        // 如果你知道特定的时区，可以用ZoneId.of("时区ID")来替换
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());

        // 转换为LocalDateTime（这将忽略时区信息，只保留日期和时间）
        LocalDateTime localDateTime = zonedDateTime.toLocalDateTime();

        // 创建DateTimeFormatter对象
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

        // 格式化日期并返回字符串
        return localDateTime.format(formatter);
    }
}
