package com.example.kkimsae_backend.service;

import com.example.kkimsae_backend.dto.CasePublicationResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicationSummaryService {

    private final PublicationSummaryStateService stateService;
    private final PublicationSummaryAiService aiService;

    public CasePublicationResponse generate(
            Long memberId,
            Long incidentId
    ) {
        // 짧은 트랜잭션: 생성 중으로 변경하고 원문 가져오기
        PublicationSummaryStateService.SummaryJob job =
                stateService.begin(memberId, incidentId);

        PublicationSummaryAiService.SummaryDraft draft;

        try {
            // DB 트랜잭션 밖에서 외부 API 호출
            draft = aiService.generate(
                    job.title(),
                    job.description(),
                    job.channel(),
                    job.demand(),
                    job.timeline()
            );
        } catch (RuntimeException exception) {
            // 철회되거나 새 작업으로 바뀌었다면 상태를 덮어쓰지 않음
            stateService.fail(job);

            // API 키, 사건 원문, 외부 API 응답 본문은 로그에 남기지 않음
            log.warn(
                    "공개 사례 요약 생성 실패. publicationId={}, errorType={}",
                    job.publicationId(),
                    exception.getClass().getSimpleName()
            );

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 요약 생성에 실패했습니다. 설정과 입력 길이를 확인한 뒤 다시 시도해주세요."
            );
        }

        // 짧은 트랜잭션: 여전히 유효한 작업인지 확인 후 저장
        return stateService.complete(job, draft);
    }
}