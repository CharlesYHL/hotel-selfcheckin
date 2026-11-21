package com.example.demo;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Tools exposed to the model.
 *
 * <p>Every parameter carries an explicit {@code @ToolParam(name = ..., description = ...)}:
 * {@code name} is mandatory (it becomes the JSON-schema property name) and {@code description}
 * is what the model actually reads when deciding how to fill the argument.
 *
 * <p>Both tools are {@code readOnly = true} and {@code concurrencySafe = true}, so the agent may
 * invoke them in parallel without permission prompts.
 */
public class DemoTools {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.ROOT);

    /** Stub data so the example runs without any external service. */
    private static final Map<String, String> WEATHER = Map.of(
            "杭州", "晴，25°C，东南风 2 级",
            "上海", "多云，23°C，东风 3 级",
            "北京", "小雨，18°C，北风 4 级");

    /**
     * Returns a stub weather report for a Chinese city name.
     *
     * <p>In a real system replace the body with an HTTP call and map failures to a
     * {@code ToolResultState.ERROR} result instead of throwing.
     *
     * @param city city name supplied by the model
     * @return human readable report
     */
    @Tool(name = "get_weather", description = "查询指定城市的当前天气", readOnly = true, concurrencySafe = true)
    public String getWeather(
            @ToolParam(name = "city", description = "城市中文名，例如 杭州") String city) {
        String key = city == null ? "" : city.trim();
        String report = WEATHER.get(key);
        if (report == null) {
            return "暂无 " + key + " 的天气数据（演示数据仅覆盖杭州、上海、北京）";
        }
        return key + "：" + report;
    }

    /**
     * Returns the current wall-clock time in the requested IANA time zone.
     *
     * @param timezone IANA zone id; blank falls back to {@code Asia/Shanghai}
     * @return formatted timestamp, or a readable error message for unknown zones
     */
    @Tool(name = "get_current_time", description = "获取指定时区的当前时间", readOnly = true, concurrencySafe = true)
    public String getCurrentTime(
            @ToolParam(name = "timezone", description = "IANA 时区名，例如 Asia/Shanghai") String timezone) {
        ZoneId zone;
        try {
            zone = (timezone == null || timezone.isBlank())
                    ? DEFAULT_ZONE
                    : ZoneId.of(timezone.trim());
        } catch (RuntimeException ex) {
            return "无法识别的时区：" + timezone + "，请使用 IANA 时区名（例如 Asia/Shanghai）";
        }
        return ZonedDateTime.now(zone).format(TIME_FORMAT);
    }
}
