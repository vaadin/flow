package com.vaadin.fusion.endpointransfermapper;

import com.vaadin.fusion.Nonnull;

import org.springframework.data.domain.Pageable;

/**
 * A DTO for {@link Pageable}.
 */
public class PageableDTO {
    private int pageNumber;
    private int pageSize;
    @Nonnull
    private SortDTO sort = new SortDTO();

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public SortDTO getSort() {
        return sort;
    }

    public void setSort(SortDTO sort) {
        this.sort = sort;
    }

}
