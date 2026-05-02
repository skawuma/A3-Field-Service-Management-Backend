package com.a3solutions.fsm.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.common
 * @project A3 Field Service Management Backend
 * @date 11/19/25
 */
@Schema(description = "Generic paginated response wrapper.")
public class PageResponse <T> {

    @Schema(description = "Page content items.")
    private List<T> content;
    @Schema(description = "Zero-based page index.", example = "0")
    private int page;
    @Schema(description = "Requested page size.", example = "10")
    private int size;
    @Schema(description = "Total number of matching items.", example = "57")
    private long totalElements;
    @Schema(description = "Total number of pages.", example = "6")
    private int totalPages;

    public PageResponse() {}

    public PageResponse(List<T> content, int page, int size, long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public static <T> PageResponse<T> of(
            List<T> content,
            int page,
            int size,
            long totalElements
    ) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }

    public List<T> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }
    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> pageData) {
        return new PageResponse<>(
                pageData.getContent(),
                pageData.getNumber(),
                pageData.getSize(),
                pageData.getTotalElements(),
                pageData.getTotalPages()
        );
    }

}
