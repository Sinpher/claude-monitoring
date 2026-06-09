package com.sinpher.claudemonitor.controller;

import com.sinpher.claudemonitor.dto.SessionDetailDto;
import com.sinpher.claudemonitor.model.Session;
import com.sinpher.claudemonitor.repository.SessionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 会话 API 控制器，提供会话列表和详情查询。
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionRepository sessionRepository;

    /**
     * 构造函数。
     *
     * @param sessionRepository 会话仓库
     */
    public SessionController(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * 查询会话列表，支持按日期范围筛选。
     *
     * @param start 开始日期（可选）
     * @param end   结束日期（可选）
     * @return 会话详情 DTO 列表
     */
    @GetMapping
    public ResponseEntity<List<SessionDetailDto>> listSessions(
            @RequestParam(required = false) LocalDate start,
            @RequestParam(required = false) LocalDate end) {
        List<Session> sessions;
        if (start != null && end != null) {
            sessions = sessionRepository.findByStartedAtBetweenOrderByStartedAtDesc(
                    start.atStartOfDay(), end.atTime(LocalTime.MAX));
        } else {
            sessions = sessionRepository.findAll();
        }
        List<SessionDetailDto> dtos = sessions.stream().map(this::toDto).toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * 查询单个会话详情。
     *
     * @param id 会话 ID
     * @return 会话详情 DTO，不存在时返回 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<SessionDetailDto> getSession(@PathVariable String id) {
        return sessionRepository.findById(id)
                .map(session -> ResponseEntity.ok(toDto(session)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 将 Session 实体转换为 DTO。
     *
     * @param session 会话实体
     * @return 会话详情 DTO
     */
    private SessionDetailDto toDto(Session session) {
        return SessionDetailDto.builder()
                .id(session.getId())
                .project(session.getProject())
                .model(session.getModel())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .totalInputTokens(session.getTotalInputTokens())
                .totalOutputTokens(session.getTotalOutputTokens())
                .totalCacheTokens(session.getTotalCacheTokens())
                .estimatedCost(session.getEstimatedCost())
                .toolCallCount(session.getToolCallCount())
                .status(session.getStatus())
                .build();
    }
}
