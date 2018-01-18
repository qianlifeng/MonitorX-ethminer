package monitorx.monitorxethminer;

import com.alibaba.fastjson.JSON;
import monitorx.monitorxethminer.statusReport.Metric;
import monitorx.monitorxethminer.statusReport.NodeStatus;
import monitorx.monitorxethminer.statusReport.NodeStatusUpload;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author qianlifeng
 */
@Component
public class ReportSchedule {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    EthMinerService ethMinerService;

    Pattern GPUTemperaturePattern = Pattern.compile("GPU Temperature:(.*)C");
    Pattern GPULoadPattern = Pattern.compile("GPU Load:(.*) %");

    private Date lastUploadDate;

    @Value("${code}")
    private String code;

    @Value("${url}")
    private String url;

    @Value("${gpuFolder}")
    private String gpuFolder;

    @Scheduled(fixedDelay = 3000)
    public void report() {
        upload();
    }

    private void upload() {
        try {
            logger.info("reporting to monitorx");
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
            metrics.add(currentHandlingOrders);
            Date lastTailDate = ethMinerService.getLastTailDate();
            Integer lastTailMh = ethMinerService.getLastTailMh();
            if (lastTailDate != null && (lastUploadDate == null || lastUploadDate.compareTo(lastTailDate) != 0)) {
                if (ethMinerService.getLastTailMh() != null) {
                    lastUploadDate = lastTailDate;
                    currentHandlingOrders.setValue(lastTailMh.toString());
                }
            } else {
                currentHandlingOrders.setValue("0");
            }

            Metric gpuMetric = new Metric();
            List<Map<String, String>> gpuInfo = getGPUInfo();
            gpuMetric.setTitle("GPU信息");
            gpuMetric.setType("text");
            StringBuilder sb = new StringBuilder();
            sb.append("<table class='table table-bordered'>");
            sb.append("     <tr>");
            sb.append("         <th width='60'>序号</th>");
            sb.append("         <th>温度</th>");
            sb.append("         <th>负载</th>");
            sb.append("     </tr>");
            for (Map<String, String> gpu : gpuInfo) {
                sb.append("     <tr>");
                sb.append("         <td>" + gpu.get("index") + "</td>");
                sb.append("         <td>" + gpu.get("temperature") + "</td>");
                sb.append("         <td>" + gpu.get("load") + "</td>");
                sb.append("     </tr>");
            }
            sb.append("</table>");

            gpuMetric.setValue(sb.toString());
            gpuMetric.setContext(JSON.toJSONString(gpuInfo));
            metrics.add(gpuMetric);

            HTTPUtil.sendBodyPost(url, JSON.toJSONString(statusUpload));
        } catch (IOException e) {
            logger.error("upload to monitorx error: " + e.getMessage(), e);
        }
    }

    private List<Map<String, String>> getGPUInfo() {
        List<Map<String, String>> info = new ArrayList<>();
        if (StringUtils.isNotEmpty(gpuFolder)) {
            for (int i = 1; i < 7; i++) {
                Path path = Paths.get(gpuFolder, i + "", "amdgpu_pm_info");
                try {
                    String content = new String(Files.readAllBytes(path));
                    Matcher temperatureMatcher = GPUTemperaturePattern.matcher(content);
                    Matcher loadMatcher = GPULoadPattern.matcher(content);
                    if (temperatureMatcher.find() && loadMatcher.find()) {
                        Map<String, String> infoMap = new HashMap<>();
                        String temperature = temperatureMatcher.group(1).trim();
                        String load = loadMatcher.group(1).trim();

                        infoMap.put("index", i + "");
                        infoMap.put("temperature", temperature + " C");
                        infoMap.put("load", load + "%");
                        info.add(infoMap);
                    } else {
                        logger.info("didn't find");
                    }
                } catch (NoSuchFileException e) {
                    break;
                } catch (IOException e) {
                    logger.error("read gpu info failed, path={}", path.toString(), e);
                }
            }
        }

        return info;
    }
}
