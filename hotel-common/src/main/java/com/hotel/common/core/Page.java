package com.hotel.common.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Page<T> implements Serializable {
    private long total;
    private long page;
    private long size;
    private List<T> records;

    public static <T> Page<T> of(long total, long page, long size, List<T> records) {
        return new Page<>(total, page, size, records);
    }

    public static <T> Page<T> empty() {
        return new Page<>(0, 0, 0, List.of());
    }
}
