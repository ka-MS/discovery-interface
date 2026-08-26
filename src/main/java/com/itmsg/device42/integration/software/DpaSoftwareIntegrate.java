package com.itmsg.device42.integration.software;

import com.itmsg.device42.integration.IntegrationJob;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component("software")
public class DpaSoftwareIntegrate implements IntegrationJob {

    public long getTotalCount() {
        return 2;
    }

    public List<String> getData(long offset, int limit) {
        List<String> data = Arrays.asList("software1", "software2");
        int fromIndex = (int) Math.min(offset, data.size());
        int toIndex = Math.min(fromIndex + limit, data.size());
        return data.subList(fromIndex, toIndex);
    }

    public void putData(List<String> datas) {
        for (String data : datas)
            System.out.println(data.toString());
    }

    @Override
    public void run() {

    }
}
