package com.itmsg.device42.integration.asset;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class DpaOsIntegrate implements AssetIntegrationTask {

    @Override
    public void integrate() {
        System.out.println("===============oh!=======================");
    }
}
