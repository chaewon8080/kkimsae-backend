package com.example.kkimsae_backend.service;

import com.example.kkimsae_backend.dto.PublicCaseResponse;
import com.example.kkimsae_backend.dto.PublicCaseSearchResponse;

import com.example.kkimsae_backend.entity.CasePublication;
import com.example.kkimsae_backend.entity.PublicationStatus;

import com.example.kkimsae_backend.repository.CasePublicationRepository;

import jakarta.persistence.criteria.Predicate;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.data.jpa.domain.Specification;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicCaseService {

    private final CasePublicationRepository publicationRepository;

    public PublicCaseSearchResponse search(
            String query,
            int page,
            int size
    ) {
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "검색어를 입력해주세요."
            );
        }

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "page는 0 이상, size는 1~100 사이여야 합니다."
            );
        }

        String normalizedQuery = query.trim();

        List<String> keywords = Arrays.stream(
                        normalizedQuery
                                .toLowerCase(Locale.ROOT)
                                .split("\\s+")
                )
                .filter(keyword -> !keyword.isBlank())
                .distinct()
                .toList();

        Specification<CasePublication> specification =
                createSearchCondition(keywords);

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("reviewedAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<CasePublication> result = publicationRepository.findAll(
                specification,
                pageable
        );

        List<PublicCaseResponse> content = result
                .getContent()
                .stream()
                .map(PublicCaseResponse::from)
                .toList();

        return new PublicCaseSearchResponse(
                normalizedQuery,
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    public PublicCaseResponse detail(Long publicationId) {
        CasePublication publication = publicationRepository
                .findByIdAndStatus(
                        publicationId,
                        PublicationStatus.APPROVED
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "공개된 사례를 찾을 수 없습니다."
                ));

        return PublicCaseResponse.from(publication);
    }

    private Specification<CasePublication> createSearchCondition(
            List<String> keywords
    ) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> conditions = new ArrayList<>();

            // 승인된 사례만 검색
            conditions.add(
                    cb.equal(
                            root.get("status"),
                            PublicationStatus.APPROVED
                    )
            );

            for (String keyword : keywords) {
                String pattern = "%" + escapeLike(keyword) + "%";

                /*
                 * 단어 하나는 아래 네 필드 중
                 * 어느 하나에 들어 있으면 됩니다.
                 */
                Predicate keywordCondition = cb.or(
                        cb.like(
                                cb.lower(root.<String>get("publicTitle")),
                                pattern,
                                '!'
                        ),
                        cb.like(
                                cb.lower(root.<String>get("publicChannel")),
                                pattern,
                                '!'
                        ),
                        cb.like(
                                cb.lower(root.<String>get("publicDemand")),
                                pattern,
                                '!'
                        ),
                        cb.like(
                                cb.lower(root.<String>get("publicSummary")),
                                pattern,
                                '!'
                        )
                );

                conditions.add(keywordCondition);
            }

            // 검색한 모든 단어의 조건을 만족해야 합니다.
            return cb.and(conditions.toArray(new Predicate[0]));
        };
    }

    private String escapeLike(String value) {
        /*
         * 사용자가 입력한 %, _를 LIKE의 특수기호가 아니라
         * 실제 문자로 검색하도록 처리합니다.
         */
        return value
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}