package com.example.kkimsae_backend.service;

import com.example.kkimsae_backend.dto.IncidentCreateRequest;
import com.example.kkimsae_backend.dto.IncidentResponse;
import com.example.kkimsae_backend.dto.IncidentSummaryResponse;

import com.example.kkimsae_backend.entity.ClueType;
import com.example.kkimsae_backend.entity.Incident;
import com.example.kkimsae_backend.entity.IncidentClue;
import com.example.kkimsae_backend.entity.Member;

import com.example.kkimsae_backend.repository.IncidentClueRepository;
import com.example.kkimsae_backend.repository.IncidentRepository;
import com.example.kkimsae_backend.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentClueRepository incidentClueRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public IncidentResponse create(
            Long memberId,
            IncidentCreateRequest request
    ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "다시 로그인해주세요."
                ));

        Incident incident = new Incident(
                member,
                request.getTitle().trim(),
                request.getDescription().trim(),
                trimToNull(request.getChannel()),
                trimToNull(request.getDemand()),
                trimToNull(request.getCompany())
        );

        incidentRepository.save(incident);

        List<IncidentClue> clues = request.getClues()
                .stream()
                .map(clue -> createClue(incident, clue))
                .toList();

        List<IncidentClue> savedClues =
                incidentClueRepository.saveAll(clues);

        return IncidentResponse.from(incident, savedClues);
    }

    @Transactional(readOnly = true)
    public List<IncidentSummaryResponse> getMyIncidents(
            Long memberId
    ) {
        return incidentRepository
                .findByMember_IdOrderByCreatedAtDescIdDesc(memberId)
                .stream()
                .map(IncidentSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public IncidentResponse getMyIncident(
            Long memberId,
            Long incidentId
    ) {
        Incident incident = incidentRepository
                .findByIdAndMember_Id(incidentId, memberId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사건을 찾을 수 없습니다."
                ));

        List<IncidentClue> clues = incidentClueRepository
                .findByIncident_IdOrderByIdAsc(incidentId);

        return IncidentResponse.from(incident, clues);
    }

    private IncidentClue createClue(
            Incident incident,
            IncidentCreateRequest.ClueRequest request
    ) {
        String bankName = null;

        if (request.getType() == ClueType.ACCOUNT) {
            bankName = trimToNull(request.getBankName());

            if (bankName == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "계좌번호에는 은행명을 입력해주세요."
                );
            }
        }

        return new IncidentClue(
                incident,
                request.getType(),
                request.getValue().trim(),
                bankName
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }


    @Transactional
    public IncidentResponse update(
            Long memberId,
            Long incidentId,
            IncidentCreateRequest request
    ) {
        Incident incident = incidentRepository
                .findByIdAndMember_Id(incidentId, memberId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사건을 찾을 수 없습니다."
                ));

        // 새 단서의 유효성을 먼저 확인
        List<IncidentClue> newClues = request.getClues()
                .stream()
                .map(clue -> createClue(incident, clue))
                .toList();

        incident.update(
                request.getTitle().trim(),
                request.getDescription().trim(),
                trimToNull(request.getChannel()),
                trimToNull(request.getDemand()),
                trimToNull(request.getCompany())
        );

        // 기존 단서를 삭제하고 새 목록으로 교체
        incidentClueRepository.deleteByIncident_Id(incidentId);
        incidentClueRepository.flush();

        List<IncidentClue> savedClues =
                incidentClueRepository.saveAll(newClues);

        return IncidentResponse.from(incident, savedClues);
    }

    @Transactional
    public void delete(
            Long memberId,
            Long incidentId
    ) {
        Incident incident = incidentRepository
                .findByIdAndMember_Id(incidentId, memberId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사건을 찾을 수 없습니다."
                ));

        // 외래 키로 연결된 단서부터 삭제
        incidentClueRepository.deleteByIncident_Id(incidentId);
        incidentClueRepository.flush();

        incidentRepository.delete(incident);
    }


}