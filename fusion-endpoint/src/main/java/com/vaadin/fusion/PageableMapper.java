package com.vaadin.fusion;

import com.vaadin.fusion.ObjectTypeMapper.Mapper;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PageableMapper implements Mapper<Pageable, PageableDTO> {

    private SortMapper sortMapper = new SortMapper();

    @Override
    public Class<? extends Pageable> getEndpointType() {
        return Pageable.class;
    }

    @Override
    public Class<? extends PageableDTO> getTransferType() {
        return PageableDTO.class;
    }

    @Override
    public PageableDTO toTransferType(Pageable pageable) {
        PageableDTO dto = new PageableDTO();
        dto.setPageNumber(pageable.getPageNumber());
        dto.setPageSize(pageable.getPageSize());
        dto.setSort(sortMapper.toTransferType(pageable.getSort()));

        return dto;
    }

    @Override
    public Pageable toEndpointType(PageableDTO dto) {
        Sort sort = sortMapper.fromTransferType(dto.getSort());
        Pageable pageable = PageRequest.of(dto.getPageNumber(),
                dto.getPageSize(), sort);
        return pageable;
    }
}
