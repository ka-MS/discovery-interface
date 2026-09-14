package com.itmsg.device42.dto.maximo.ci;

import java.util.List;

/** Computer 한 대의 ACTCI와 ACTCISPEC 적재 데이터. */
public record ComputerCiUpsert(ActCiUpsert actCi, List<ActCiSpecUpsert> specs) {
}
