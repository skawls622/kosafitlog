package com.kosa.fitlog.bodycomposition.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.kosa.fitlog.bodycomposition.dto.BodyCompositionDTO;
import com.kosa.fitlog.bodycomposition.mapper.BodyCompositionMapper;

@Service
public class BodyCompositionService {

    private final BodyCompositionMapper bodyCompositionMapper;

    public BodyCompositionService(BodyCompositionMapper bodyCompositionMapper) {
        this.bodyCompositionMapper = bodyCompositionMapper;
    }

    public void register(Long memberId, BodyCompositionDTO bodyComposition) {
        if (memberId == null || bodyComposition == null) {
            throw new IllegalArgumentException("로그인 회원과 체성분 기록 정보가 필요합니다.");
        }

        LocalDate measuredDate = bodyComposition.getMeasuredDate();
        if (measuredDate == null) {
            throw new IllegalArgumentException("측정일을 입력해 주세요.");
        }
        if (measuredDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("미래 날짜는 측정일로 등록할 수 없습니다.");
        }

        Double weight = bodyComposition.getWeight();
        Double skeletalMuscle = bodyComposition.getSkeletalMuscle();
        Double bodyFatPercentage = bodyComposition.getBodyFatPercentage();
        if (weight == null && skeletalMuscle == null && bodyFatPercentage == null) {
            throw new IllegalArgumentException("체중, 골격근량, 체지방률 중 하나 이상 입력해 주세요.");
        }

        validatePositiveMeasurement(weight, "체중");
        validatePositiveMeasurement(skeletalMuscle, "골격근량");
        if (bodyFatPercentage != null) {
            if (!Double.isFinite(bodyFatPercentage)
                    || bodyFatPercentage < 0 || bodyFatPercentage > 100) {
                throw new IllegalArgumentException("체지방률은 0~100 사이로 입력해 주세요.");
            }
            validateScale(bodyFatPercentage, "체지방률");
        }

        if (bodyCompositionMapper.countByMemberIdAndMeasuredDate(memberId, measuredDate) > 0) {
            throw new IllegalArgumentException(
                    "해당 날짜의 체성분 기록이 이미 존재합니다. 기존 기록을 삭제한 뒤 다시 등록해 주세요.");
        }

        // 요청에 memberId가 포함되어 있어도 사용하지 않고 세션에서 받은 값으로 덮어쓴다.
        bodyComposition.setMemberId(memberId);
        if (bodyCompositionMapper.insert(bodyComposition) != 1) {
            throw new IllegalStateException("체성분 기록 저장에 실패했습니다.");
        }
    }

    public List<BodyCompositionDTO> findByMemberId(Long memberId) {
        if (memberId == null) {
            return Collections.emptyList();
        }
        return bodyCompositionMapper.findByMemberId(memberId);
    }

    public boolean delete(Long bodyCompositionId, Long memberId) {
        if (bodyCompositionId == null || memberId == null) {
            return false;
        }
        return bodyCompositionMapper.deleteByIdAndMemberId(bodyCompositionId, memberId) == 1;
    }

    private void validatePositiveMeasurement(Double value, String label) {
        if (value == null) {
            return;
        }
        if (!Double.isFinite(value) || value <= 0 || value > 999.99) {
            throw new IllegalArgumentException(label + "은 0보다 크고 999.99 이하여야 합니다.");
        }
        validateScale(value, label);
    }

    private void validateScale(Double value, String label) {
        if (BigDecimal.valueOf(value).scale() > 2) {
            throw new IllegalArgumentException(label + "은 소수 둘째 자리까지 입력해 주세요.");
        }
    }
}
