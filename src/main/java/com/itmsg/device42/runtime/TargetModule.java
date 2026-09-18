package com.itmsg.device42.runtime;

import java.util.Map;

/** 선택된 타겟이 제공하는 작업. 작업 이름과 종류는 각 조립부가 소유한다. */
public interface TargetModule {
    String id();
    Map<String, IntegrationJob> jobs();
}
