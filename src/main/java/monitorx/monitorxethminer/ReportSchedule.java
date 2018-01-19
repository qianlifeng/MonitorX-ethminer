package monitorx.monitorxethminer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    Pattern GPUPowerPattern = Pattern.compile("(.*?) W \\(average GPU\\)");

    private Date lastUploadDate;

    private Map<String, String> xhInfo;

    @Value("${code}")
    private String code;

    @Value("${url}")
    private String url;

    @Value("${gpuFolder}")
    private String gpuFolder;

    @Value("${wallet}")
    private String wallet;

    @Scheduled(fixedDelay = 3000)
    public void report() {
        upload();
    }

    @PostConstruct
    public void init() {
        getXHInfo();
    }

    /**
     * 获得星火矿池信息
     */
    @Scheduled(fixedDelay = 60000)
    private void getXHInfo() {
        if (StringUtils.isNotEmpty(wallet)) {
            String walletStr = wallet.replaceAll("0x", "");
            String url = "https://eth.ethfans.org/api/page/miner?value=" + walletStr;
            try {
                Map<String, String> map = new HashMap<>();
                JSONObject info = JSON.parseObject(HTTPUtil.sendGet(url.toLowerCase()));
                String balanceStr = info.getJSONObject("balance").getJSONObject("data").getString("balance");
                String balance = new BigDecimal(balanceStr).divide(BigDecimal.valueOf(1000000000000000000L)).setScale(3, RoundingMode.HALF_UP).toString();
                map.put("balance", balance);

                Long hashrate = info.getJSONObject("hashrate").getJSONObject("data").getLong("meanHashrate24H");
                String meanHashrate24H = new BigDecimal(hashrate).divide(BigDecimal.valueOf(1000000)).setScale(0, RoundingMode.HALF_UP).toString();
                map.put("meanHashrate24H", meanHashrate24H);

                xhInfo = map;
            } catch (IOException e) {
                logger.error("get ethfans info error: " + e.getMessage());
            }
        }
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
            sb.append("<table class='table table-bordered table-condensed'>");
            sb.append("     <tr>");
            sb.append("         <th width='60'>序号</th>");
            sb.append("         <th>温度</th>");
            sb.append("         <th>负载</th>");
            sb.append("         <th>平均功率</th>");
            sb.append("     </tr>");
            for (Map<String, String> gpu : gpuInfo) {
                sb.append("     <tr>");
                sb.append("         <td>" + gpu.get("index") + "</td>");
                sb.append("         <td>" + gpu.get("temperature") + "℃</td>");
                sb.append("         <td>" + gpu.get("load") + "％</td>");
                sb.append("         <td>" + gpu.get("power") + "W</td>");
                sb.append("     </tr>");
            }
            sb.append("</table>");
            gpuMetric.setValue(sb.toString());
            gpuMetric.setContext(JSON.toJSONString(gpuInfo));
            metrics.add(gpuMetric);

            if (xhInfo != null) {
                Metric xhMetric = new Metric();
                xhMetric.setTitle("星火帐户余额");
                xhMetric.setType("text");
                xhMetric.setValue("<div style='font-weight: 700;font-size: 90px;font-family: 黑体!important;height: 230px;display: flex;align-items: center;justify-content: center;'>" + xhInfo.get("balance") + "</div>");
                metrics.add(xhMetric);

                Metric meanHashRateMetric = new Metric();
                meanHashRateMetric.setTitle("星火24小时平均算力");
                meanHashRateMetric.setType("number");
                meanHashRateMetric.setValue(xhInfo.get("meanHashrate24H"));
                metrics.add(meanHashRateMetric);
            }

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
                    Matcher powerMatcher = GPUPowerPattern.matcher(content);
                    if (temperatureMatcher.find() && loadMatcher.find() && powerMatcher.find()) {
                        Map<String, String> infoMap = new HashMap<>();
                        String temperature = temperatureMatcher.group(1).trim();
                        String load = loadMatcher.group(1).trim();
                        String power = powerMatcher.group(1).trim();

                        infoMap.put("index", i + "");
                        infoMap.put("temperature", temperature);
                        infoMap.put("load", load);
                        infoMap.put("power", power);
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
