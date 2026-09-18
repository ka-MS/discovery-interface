package com.itmsg.device42.target.maximo.ci;

/** 관계 한 쌍. SWAPPED·CHANGEBY·CHANGEDATE는 Writer가 상수·페이지 단위 시각으로 채운다. */
public record ActCiRelationUpsert(String sourceCiNum, String targetCiNum, String relationNum) {
}
