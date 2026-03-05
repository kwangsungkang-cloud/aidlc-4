package com.tableorder.store.service;

import com.tableorder.common.exception.BusinessException;
import com.tableorder.common.exception.ErrorCode;
import com.tableorder.store.entity.Store;
import com.tableorder.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;

    @Cacheable(value = "stores", key = "#storeId")
    public Store getById(Long storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));
    }

    public Store getByStoreCode(String storeCode) {
        return storeRepository.findByStoreCode(storeCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));
    }
}
