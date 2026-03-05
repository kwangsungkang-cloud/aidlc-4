package com.tableorder.table.service;

import com.tableorder.common.exception.BusinessException;
import com.tableorder.common.exception.ErrorCode;
import com.tableorder.store.entity.Store;
import com.tableorder.store.service.StoreService;
import com.tableorder.table.dto.CreateTableRequest;
import com.tableorder.table.dto.CreateTableResponse;
import com.tableorder.table.dto.EndSessionResponse;
import com.tableorder.table.entity.StoreTable;
import com.tableorder.table.entity.TableSession;
import com.tableorder.table.repository.StoreTableRepository;
import com.tableorder.table.repository.TableSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TableSessionService {

    private final StoreTableRepository storeTableRepository;
    private final TableSessionRepository tableSessionRepository;
    private final StoreService storeService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public CreateTableResponse createTable(Long storeId, CreateTableRequest request) {
        Store store = storeService.getById(storeId);

        if (storeTableRepository.existsByStoreIdAndTableNumber(storeId, request.getTableNumber())) {
            throw new BusinessException(ErrorCode.TABLE_ALREADY_EXISTS);
        }

        StoreTable table = StoreTable.builder()
                .store(store)
                .tableNumber(request.getTableNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        table = storeTableRepository.save(table);

        return CreateTableResponse.builder()
                .tableId(table.getId())
                .storeId(storeId)
                .tableNumber(table.getTableNumber())
                .createdAt(table.getCreatedAt())
                .build();
    }

    @Transactional
    public EndSessionResponse endSession(Long tableId, Long storeId) {
        StoreTable table = storeTableRepository.findById(tableId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_NOT_FOUND));

        if (!table.getStore().getId().equals(storeId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        TableSession session = tableSessionRepository
                .findByStoreTableIdAndStatus(tableId, TableSession.Status.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_ACTIVE_SESSION));

        session.complete();
        tableSessionRepository.save(session);

        log.info("Session completed: sessionId={}, tableId={}, tableNumber={}",
                session.getId(), tableId, table.getTableNumber());

        return EndSessionResponse.builder()
                .sessionId(session.getId())
                .tableNumber(table.getTableNumber())
                .status("COMPLETED")
                .completedAt(session.getCompletedAt())
                .totalOrderAmount(0)
                .orderCount(0)
                .build();
    }

    @Transactional(readOnly = true)
    public TableSession getActiveSession(Long sessionId) {
        TableSession session = tableSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_CLOSED));

        if (!session.isActive()) {
            throw new BusinessException(ErrorCode.SESSION_CLOSED);
        }

        if (session.isExpired()) {
            throw new BusinessException(ErrorCode.SESSION_EXPIRED);
        }

        return session;
    }
}
