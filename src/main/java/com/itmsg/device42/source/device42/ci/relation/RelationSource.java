package com.itmsg.device42.source.device42.ci.relation;

/** 원천 연결의 양 끝 PK. 타겟 식별자와 관계 코드는 포함하지 않는다. */
public record RelationSource(String fromPk, String toPk) {}
