package com.itmsg.device42.target.maximo.ci;

import java.util.List;

/** CI 한 건의 ACTCI 본체와 ACTCISPEC 속성 적재 데이터. 유형과 무관하다. */
public record CiUpsert(ActCiUpsert actCi, List<ActCiSpecUpsert> specs) {
}
