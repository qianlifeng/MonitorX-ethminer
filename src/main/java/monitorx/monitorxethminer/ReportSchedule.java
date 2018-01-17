package monitorx.monitorxethminer;

import com.alibaba.fastjson.JSON;
import monitorx.monitorxethminer.statusReport.Metric;
import monitorx.monitorxethminer.statusReport.NodeStatus;
import monitorx.monitorxethminer.statusReport.NodeStatusUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author qianlifeng
 */
@Component
public class ReportSchedule {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    EthMinerService ethMinerService;

    private Date lastUploadDate;

    @Value("${code}")
    private String code;

    @Value("${url}")
    private String url;

    @Scheduled(fixedDelay = 1000)
    public void doNothing() {
        Date lastTailDate = ethMinerService.getLastTailDate();
        if (lastTailDate != null && (lastUploadDate == null || lastUploadDate.compareTo(lastTailDate) != 0)) {
            if (ethMinerService.getLastTailMh() != null) {
                lastUploadDate = lastTailDate;

                upload(lastTailDate, ethMinerService.getLastTailMh());
            }
        }
    }

    private void upload(Date lastTailDate, Integer lastTailMh) {
        try {
            logger.info("reporting to monitorx: {} {}", lastTailMh, lastTailDate);
            NodeStatusUpload statusUpload = new NodeStatusUpload();
            NodeStatus status = new NodeStatus();
            statusUpload.setNodeCode(code);
            statusUpload.setNodeStatus(status);

            status.setStatus("up");
            List<Metric> metrics = new ArrayList<>();
            status.setMetrics(metrics);

            Metric currentHandlingOrders = new Metric();
            currentHandlingOrders.setTitle("实时算力");
            currentHandlingOrders.setType("number");
            currentHandlingOrders.setValue(lastTailMh.toString());
            metrics.add(currentHandlingOrders);

            HTTPUtil.sendBodyPost(url, JSON.toJSONString(statusUpload));
        } catch (IOException e) {
            logger.error("upload to monitorx error: " + e.getMessage(), e);
        }
    }
}
