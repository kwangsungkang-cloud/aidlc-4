package com.tableorder.auth.service;

import com.tableorder.auth.dto.*;
import com.tableorder.common.exception.BusinessException;
import com.tableorder.common.exception.ErrorCode;
import com.tableorder.common.security.JwtTokenProvider;
import com.tableorder.store.entity.Store;
import com.tableorder.store.repository.StoreRepository;
import com.tableorder.table.entity.StoreTable;
import com.tableorder.table.entity.TableSession;
import com.tableorder.table.repository.StoreTableRepository;
import com.tableorder.table.repository.TableSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final StoreRepository storeRepository;
    private final StoreTableRepository storeTableRepository;
    private final TableSessionRepository tableSessionRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TableLoginResponse loginTable(TableLoginRequest request) {
        Store store = storeRepository.findByStoreCode(request.getStoreCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        StoreTable table = storeTableRepository
                .findByStoreIdAndTableNumber(store.getId(), request.getTableNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), table.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "비밀번호가 일치하지 않습니다");
        }

        var existingSession = tableSessionRepository
                .findByStoreTableIdAndStatus(table.getId(), TableSession.Status.ACTIVE);

        boolean isNewSession;
        TableSession session;

        if (existingSession.isPresent()) {
            session = existingSession.get();
            if (session.isExpired()) {
                session.complete();
                tableSessionRepository.save(session);
                session = createNewSession(table);
                isNewSession = true;
            } else {
                isNewSession = false;
            }
        } else {
            session = createNewSession(table);
            isNewSession = true;
        }

        String token = jwtTokenProvider.generateTableToken(
                session.getId(), table.getId(), store.getId(), table.getTableNumber());

        return TableLoginResponse.builder()
                .token(token)
                .storeName(store.getStoreName())
                .storeCode(store.getStoreCode())
                .tableNumber(table.getTableNumber())
                .sessionId(session.getId())
                .isNewSession(isNewSession)
                .build();
    }

    private TableSession createNewSession(StoreTable table) {
        TableSession session = TableSession.builder()
                .storeTable(table)
                .sessionToken(UUID.randomUUID().toString())
                .status(TableSession.Status.ACTIVE)
                .startedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(16))
                .build();
        return tableSessionRepository.save(session);
    }
}
