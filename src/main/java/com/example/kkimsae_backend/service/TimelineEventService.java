package com.example.kkimsae_backend.service;

import com.example.kkimsae_backend.dto.TimelineEventRequest;
import com.example.kkimsae_backend.dto.TimelineEventResponse;

import com.example.kkimsae_backend.entity.Incident;
import com.example.kkimsae_backend.entity.TimelineEvent;

import com.example.kkimsae_backend.repository.IncidentRepository;
import com.example.kkimsae_backend.repository.TimelineEventRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TimelineEventService {

    private final IncidentRepository incidentRepository;
    private final TimelineEventRepository timelineEventRepository;

    @Transactional
    public TimelineEventResponse create(
            Long memberId,
            Long incidentId,
            TimelineEventRequest request
    ) {
        Incident incident = findMyIncident(memberId, incidentId);

        TimelineEvent event = new TimelineEvent(
                incident,
                request.getOccurredAt(),
                request.getContent().trim(),
                trimToNull(request.getTag())
        );

        timelineEventRepository.saveAndFlush(event);

        return TimelineEventResponse.from(event);
    }

    @Transactional(readOnly = true)
    public List<TimelineEventResponse> list(
            Long memberId,
            Long incidentId
    ) {
        findMyIncident(memberId, incidentId);

        return timelineEventRepository
                .findByIncident_IdOrderByOccurredAtAscIdAsc(incidentId)
                .stream()
                .map(TimelineEventResponse::from)
                .toList();
    }

    @Transactional
    public TimelineEventResponse update(
            Long memberId,
            Long incidentId,
            Long eventId,
            TimelineEventRequest request
    ) {
        findMyIncident(memberId, incidentId);

        TimelineEvent event = findEvent(incidentId, eventId);

        event.setOccurredAt(request.getOccurredAt());
        event.setContent(request.getContent().trim());
        event.setTag(trimToNull(request.getTag()));

        // 수정 시각까지 반영한 뒤 응답
        timelineEventRepository.flush();

        return TimelineEventResponse.from(event);
    }

    @Transactional
    public void delete(
            Long memberId,
            Long incidentId,
            Long eventId
    ) {
        findMyIncident(memberId, incidentId);

        TimelineEvent event = findEvent(incidentId, eventId);

        timelineEventRepository.delete(event);
    }

    private Incident findMyIncident(
            Long memberId,
            Long incidentId
    ) {
        return incidentRepository
                .findByIdAndMember_Id(incidentId, memberId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사건을 찾을 수 없습니다."
                ));
    }

    private TimelineEvent findEvent(
            Long incidentId,
            Long eventId
    ) {
        return timelineEventRepository
                .findByIdAndIncident_Id(eventId, incidentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "타임라인 기록을 찾을 수 없습니다."
                ));
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}