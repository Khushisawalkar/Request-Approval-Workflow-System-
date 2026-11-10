package com.workflow.service;

import com.workflow.dto.RequestAuditLogDto;
import com.workflow.model.RequestAuditLog;
import com.workflow.model.RequestStatus;
import com.workflow.repository.RequestAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final RequestAuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public void record(Long requestId, String action, String actorUsername, String actorRole,
                       RequestStatus fromStatus, RequestStatus toStatus, String comment) {
        RequestAuditLog logEntry = RequestAuditLog.builder()
                .requestId(requestId)
                .action(action)
                .actorUsername(actorUsername)
                .actorRole(actorRole)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .comment(comment)
                .build();

        auditLogRepository.save(logEntry);
        log.debug("Audit recorded for request [{}]: action [{}], actor [{}]", requestId, action, actorUsername);
    }

    @Transactional(readOnly = true)
    public List<RequestAuditLogDto> getAuditLogsForRequest(Long requestId) {
        return auditLogRepository.findByRequestIdOrderByTimestampDesc(requestId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private RequestAuditLogDto mapToDto(RequestAuditLog log) {
        return RequestAuditLogDto.builder()
                .id(log.getId())
                .requestId(log.getRequestId())
                .action(log.getAction())
                .actorUsername(log.getActorUsername())
                .actorRole(log.getActorRole())
                .fromStatus(log.getFromStatus())
                .toStatus(log.getToStatus())
                .comment(log.getComment())
                .timestamp(log.getTimestamp())
                .build();
    }
}
