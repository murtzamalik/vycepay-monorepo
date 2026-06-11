package com.vycepay.admin.application.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small helper for bounded admin pagination responses. */
public final class Pagination {
    private Pagination() { }
    public static int page(Integer page) { return page == null || page < 0 ? 0 : page; }
    public static int size(Integer size) { return size == null || size < 1 ? 20 : Math.min(size, 100); }
    public static int offset(int page, int size) { return page * size; }
    public static Map<String, Object> response(List<Map<String, Object>> content, int page, int size, long total) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", content); response.put("page", page); response.put("size", size); response.put("total", total);
        response.put("totalPages", size == 0 ? 0 : (int) Math.ceil((double) total / size));
        return response;
    }
}
