package com.saju.engine

import org.springframework.stereotype.Service

@Service
class SajuService(
    private val engine: SajuEngine     // ⚡ 엔진 클래스 DI
) {

    fun calculate(request: SajuRequest): SajuResponse {
        // 모든 계산은 엔진에서 단일 처리
        val result = engine.calculate(request)

        return SajuResponse(
            ganji = result.ganji,
            sibsung = result.sibsung,
            branchSibsung = result.branchSibsung,
            twelve = result.twelve,
            daewoon = result.daewoon,
            relations = result.relations,
            seun = result.seun,
            wolun = result.wolun
        )
    }
}
