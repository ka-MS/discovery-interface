# 종료된 사항

## ISSUE-1 SOURCEID가 서버 간 불일치

**결론:** 이슈 아님.

데모 Device42 서버마다 같은 장비의 `device_pk`가 다르다. 운영은 단일 Device42
서버를 사용하므로 서버 간 ID 일치는 요구사항이 아니다.

## ISSUE-3 PDU가 COMPUTER로 분류됨

**결론:** 해결.

`physicalsubtype = 'PDU'`가 COMPUTER로 분류됐다. Maximo에 대응 ASSETCLASS와
DPA 테이블이 없어 `DEPLOYEDASSET`과 COMPUTER 자식의 조회 대상에서 제외했다.

## ISSUE-4 자식 태스크의 조회 조건이 부모와 다름

**결론:** 해결.

COMPUTER 자식이 부모에서 제외한 Docker Container 등을 조회했다. 부모의 타입,
Docker Container, PDU 제외 조건을 동일하게 적용한 뒤 COMPUTER 대상만 조회한다.

## 가상 장비 vendor 없음

**결론:** 이슈 아님.

`MAXATTRIBUTE.DEFAULTVALUE`가 `UNKNOWN`으로 정의되어 있어 정상 결과다.
