package com.example.mcp.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Date Utilities for Processing and Formatting
 * 日期处理和格式化工具类
 * 
 * This utility class provides methods for date processing, validation, and formatting
 * to support consistent date handling across the application. It supports both
 * English and Chinese locales and provides comprehensive date manipulation utilities.
 * 
 * 该工具类提供日期处理、验证和格式化方法，以支持整个应用程序中一致的日期处理。
 * 它支持英语和中文语言环境，并提供全面的日期操作工具。
 * 
 * <p>This implementation addresses the following requirements:</p>
 * <ul>
 *   <li><strong>NFR2: Security</strong> - Secure date validation and processing</li>
 *   <li><strong>FR3: Weekly Report Generation</strong> - Date range calculations for weekly reports</li>
 *   <li><strong>FR4: Risk Analysis Tool</strong> - Due date calculations and overdue detection</li>
 *   <li><strong>FR5: Completion Analysis Tool</strong> - Completion date tracking and metrics</li>
 * </ul>
 * 
 * @since 1.0
 * @author OpenProject MCP Team
 */
public class DateUtils {
    
    // Date formatters for different formats
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_DATE;
    private static final DateTimeFormatter ISO_DATETIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter CHINESE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
    private static final DateTimeFormatter CHINESE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss");
    
    // Patterns for date validation
    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern ISO_DATETIME_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})?$");
    private static final Pattern DISPLAY_DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    
    /**
     * Parses a date string in ISO format (yyyy-MM-dd)
     * 解析ISO格式的日期字符串 (yyyy-MM-dd)
     * 
     * @param dateString The date string to parse
     * @return LocalDate object, or null if parsing fails
     */
    public static LocalDate parseIsoDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        
        try {
            return LocalDate.parse(dateString, ISO_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    /**
     * Parses a datetime string in ISO format (yyyy-MM-dd'T'HH:mm:ss)
     * 解析ISO格式的日期时间字符串 (yyyy-MM-dd'T'HH:mm:ss)
     * 
     * @param dateTimeString The datetime string to parse
     * @return LocalDateTime object, or null if parsing fails
     */
    public static LocalDateTime parseIsoDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }
        
        try {
            return LocalDateTime.parse(dateTimeString, ISO_DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    /**
     * Formats a LocalDate for display (yyyy-MM-dd)
     * 格式化LocalDate用于显示 (yyyy-MM-dd)
     * 
     * @param date The date to format
     * @return Formatted date string
     */
    public static String formatDisplayDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DISPLAY_DATE_FORMATTER);
    }
    
    /**
     * Formats a LocalDateTime for display (yyyy-MM-dd HH:mm:ss)
     * 格式化LocalDateTime用于显示 (yyyy-MM-dd HH:mm:ss)
     * 
     * @param dateTime The datetime to format
     * @return Formatted datetime string
     */
    public static String formatDisplayDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DISPLAY_DATETIME_FORMATTER);
    }
    
    /**
     * Formats a LocalDate in Chinese format (yyyy年MM月dd日)
     * 格式化LocalDate为中文格式 (yyyy年MM月dd日)
     * 
     * @param date The date to format
     * @return Formatted Chinese date string
     */
    public static String formatChineseDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(CHINESE_DATE_FORMATTER);
    }
    
    /**
     * Formats a LocalDateTime in Chinese format (yyyy年MM月dd日 HH:mm:ss)
     * 格式化LocalDateTime为中文格式 (yyyy年MM月dd日 HH:mm:ss)
     * 
     * @param dateTime The datetime to format
     * @return Formatted Chinese datetime string
     */
    public static String formatChineseDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(CHINESE_DATETIME_FORMATTER);
    }
    
    /**
     * Gets the start of a week (Monday) for a given date
     * 获取给定日期的周开始时间（周一）
     * 
     * @param date The reference date
     * @return LocalDate representing the start of the week
     */
    public static LocalDate getWeekStart(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
    
    /**
     * Gets the end of a week (Sunday) for a given date
     * 获取给定日期的周结束时间（周日）
     * 
     * @param date The reference date
     * @return LocalDate representing the end of the week
     */
    public static LocalDate getWeekEnd(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }
    
    /**
     * Gets the start of a month for a given date
     * 获取给定日期的月开始时间
     * 
     * @param date The reference date
     * @return LocalDate representing the start of the month
     */
    public static LocalDate getMonthStart(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.with(TemporalAdjusters.firstDayOfMonth());
    }
    
    /**
     * Gets the end of a month for a given date
     * 获取给定日期的月结束时间
     * 
     * @param date The reference date
     * @return LocalDate representing the end of the month
     */
    public static LocalDate getMonthEnd(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.with(TemporalAdjusters.lastDayOfMonth());
    }
    
    /**
     * Calculates the number of days between two dates
     * 计算两个日期之间的天数
     * 
     * @param startDate The start date
     * @param endDate The end date
     * @return Number of days between dates (positive if endDate > startDate)
     */
    public static long getDaysBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(startDate, endDate);
    }
    
    /**
     * Calculates the number of working days between two dates (excluding weekends)
     * 计算两个日期之间的工作日数（排除周末）
     * 
     * @param startDate The start date
     * @param endDate The end date
     * @return Number of working days between dates
     */
    public static long getWorkingDaysBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween <= 0) {
            return 0;
        }
        
        long workingDays = 0;
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            if (current.getDayOfWeek() != DayOfWeek.SATURDAY && 
                current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        
        return workingDays;
    }
    
    /**
     * Checks if a date is overdue relative to current date
     * 检查日期是否已过期
     * 
     * @param dueDate The due date to check
     * @return true if the date is overdue, false otherwise
     */
    public static boolean isOverdue(LocalDate dueDate) {
        if (dueDate == null) {
            return false;
        }
        return dueDate.isBefore(LocalDate.now());
    }
    
    /**
     * Gets the number of days until a due date (negative if overdue)
     * 获取距离截止日期的天数（如果已过期则为负数）
     * 
     * @param dueDate The due date
     * @return Number of days until due date
     */
    public static long getDaysUntilDue(LocalDate dueDate) {
        if (dueDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }
    
    /**
     * Gets the number of days since a date (negative if in future)
     * 获取距离某个日期的天数（如果在未来则为负数）
     * 
     * @param referenceDate The reference date
     * @return Number of days since the reference date
     */
    public static long getDaysSince(LocalDate referenceDate) {
        if (referenceDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(referenceDate, LocalDate.now());
    }
    
    /**
     * Checks if a date is within a specified range
     * 检查日期是否在指定范围内
     * 
     * @param date The date to check
     * @param startDate The start of the range
     * @param endDate The end of the range
     * @return true if the date is within the range, false otherwise
     */
    public static boolean isDateInRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        if (date == null || startDate == null || endDate == null) {
            return false;
        }
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
    
    /**
     * Adds working days to a date (skipping weekends)
     * 向日期添加工作日（跳过周末）
     * 
     * @param date The starting date
     * @param workingDays The number of working days to add
     * @return LocalDate after adding working days
     */
    public static LocalDate addWorkingDays(LocalDate date, int workingDays) {
        if (date == null || workingDays == 0) {
            return date;
        }
        
        LocalDate result = date;
        int daysToAdd = workingDays;
        
        while (daysToAdd > 0) {
            result = result.plusDays(1);
            if (result.getDayOfWeek() != DayOfWeek.SATURDAY && 
                result.getDayOfWeek() != DayOfWeek.SUNDAY) {
                daysToAdd--;
            }
        }
        
        while (daysToAdd < 0) {
            result = result.minusDays(1);
            if (result.getDayOfWeek() != DayOfWeek.SATURDAY && 
                result.getDayOfWeek() != DayOfWeek.SUNDAY) {
                daysToAdd++;
            }
        }
        
        return result;
    }
    
    /**
     * Gets a list of dates in a specified range
     * 获取指定范围内的日期列表
     * 
     * @param startDate The start date
     * @param endDate The end date
     * @return List of LocalDate objects in the range
     */
    public static List<LocalDate> getDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return new ArrayList<>();
        }
        
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            dates.add(current);
            current = current.plusDays(1);
        }
        
        return dates;
    }
    
    /**
     * Gets the week number for a given date (ISO standard)
     * 获取给定日期的周数（ISO标准）
     * 
     * @param date The date to get week number for
     * @return Week number (1-53)
     */
    public static int getWeekNumber(LocalDate date) {
        if (date == null) {
            return 0;
        }
        return date.get(WeekFields.ISO.weekOfYear());
    }
    
    /**
     * Gets the quarter for a given date
     * 获取给定日期的季度
     * 
     * @param date The date to get quarter for
     * @return Quarter number (1-4)
     */
    public static int getQuarter(LocalDate date) {
        if (date == null) {
            return 0;
        }
        return (date.getMonthValue() - 1) / 3 + 1;
    }
    
    /**
     * Gets the fiscal year end date (assuming fiscal year ends in December)
     * 获取财年结束日期（假设财年在12月结束）
     * 
     * @param year The fiscal year
     * @return LocalDate representing the fiscal year end
     */
    public static LocalDate getFiscalYearEnd(int year) {
        return LocalDate.of(year, 12, 31);
    }
    
    /**
     * Gets the fiscal year start date (assuming fiscal year starts in January)
     * 获取财年开始日期（假设财年在1月开始）
     * 
     * @param year The fiscal year
     * @return LocalDate representing the fiscal year start
     */
    public static LocalDate getFiscalYearStart(int year) {
        return LocalDate.of(year, 1, 1);
    }
    
    /**
     * Validates if a date string is in ISO format
     * 验证日期字符串是否为ISO格式
     * 
     * @param dateString The date string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidIsoDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return false;
        }
        return ISO_DATE_PATTERN.matcher(dateString).matches() && parseIsoDate(dateString) != null;
    }
    
    /**
     * Validates if a datetime string is in ISO format
     * 验证日期时间字符串是否为ISO格式
     * 
     * @param dateTimeString The datetime string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidIsoDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return false;
        }
        return ISO_DATETIME_PATTERN.matcher(dateTimeString).matches() && parseIsoDateTime(dateTimeString) != null;
    }
    
    /**
     * Gets the current date in ISO format
     * 获取当前日期的ISO格式
     * 
     * @return Current date as ISO string
     */
    public static String getCurrentIsoDate() {
        return LocalDate.now().format(ISO_DATE_FORMATTER);
    }
    
    /**
     * Gets the current datetime in ISO format
     * 获取当前日期时间的ISO格式
     * 
     * @return Current datetime as ISO string
     */
    public static String getCurrentIsoDateTime() {
        return LocalDateTime.now().format(ISO_DATETIME_FORMATTER);
    }
    
    /**
     * Gets a human-readable relative time description
     * 获取人类可读的相对时间描述
     * 
     * @param referenceDate The reference date
     * @return Human-readable description (e.g., "3 days ago", "in 2 weeks")
     */
    public static String getRelativeTimeDescription(LocalDate referenceDate) {
        if (referenceDate == null) {
            return "";
        }
        
        long daysUntil = getDaysUntilDue(referenceDate);
        
        if (daysUntil == 0) {
            return "今天 | Today";
        } else if (daysUntil == 1) {
            return "明天 | Tomorrow";
        } else if (daysUntil == -1) {
            return "昨天 | Yesterday";
        } else if (daysUntil > 0 && daysUntil <= 7) {
            return daysUntil + " 天后 | in " + daysUntil + " days";
        } else if (daysUntil < 0 && daysUntil >= -7) {
            return Math.abs(daysUntil) + " 天前 | " + Math.abs(daysUntil) + " days ago";
        } else if (daysUntil > 7 && daysUntil <= 30) {
            long weeks = daysUntil / 7;
            return weeks + " 周后 | in " + weeks + " weeks";
        } else if (daysUntil < -7 && daysUntil >= -30) {
            long weeks = Math.abs(daysUntil) / 7;
            return weeks + " 周前 | " + weeks + " weeks ago";
        } else if (daysUntil > 30 && daysUntil <= 365) {
            long months = daysUntil / 30;
            return months + " 月后 | in " + months + " months";
        } else if (daysUntil < -30 && daysUntil >= -365) {
            long months = Math.abs(daysUntil) / 30;
            return months + " 月前 | " + months + " months ago";
        } else {
            long years = daysUntil / 365;
            if (years > 0) {
                return years + " 年后 | in " + years + " years";
            } else {
                return Math.abs(years) + " 年前 | " + Math.abs(years) + " years ago";
            }
        }
    }
    
    /**
     * Converts a legacy Date object to LocalDate
     * 将旧的Date对象转换为LocalDate
     * 
     * @param legacyDate The legacy Date object
     * @return LocalDate equivalent
     */
    public static LocalDate toDate(java.util.Date legacyDate) {
        if (legacyDate == null) {
            return null;
        }
        return legacyDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
    
    /**
     * Converts a legacy Date object to LocalDateTime
     * 将旧的Date对象转换为LocalDateTime
     * 
     * @param legacyDate The legacy Date object
     * @return LocalDateTime equivalent
     */
    public static LocalDateTime toDateTime(java.util.Date legacyDate) {
        if (legacyDate == null) {
            return null;
        }
        return legacyDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
    
    /**
     * Converts LocalDate to legacy Date object
     * 将LocalDate转换为旧的Date对象
     * 
     * @param localDate The LocalDate to convert
     * @return Date equivalent
     */
    public static java.util.Date toDate(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return java.util.Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
    
    /**
     * Converts LocalDateTime to legacy Date object
     * 将LocalDateTime转换为旧的Date对象
     * 
     * @param localDateTime The LocalDateTime to convert
     * @return Date equivalent
     */
    public static java.util.Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return java.util.Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}