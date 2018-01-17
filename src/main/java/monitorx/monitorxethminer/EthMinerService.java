package monitorx.monitorxethminer;

import monitorx.monitorxethminer.tail.LogTail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author qianlifeng
 */
@Service
public class EthMinerService {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${log}")
    String logFile;

    @Value("${wallet}")
    String walletAddress;

    private Date lastTailDate;
    private Integer lastTailMh;

    Pattern pattern = Pattern.compile(".*?Speed.*?36m(.*?) M.*");

    public Date getLastTailDate() {
        return lastTailDate;
    }

    public void setLastTailDate(Date lastTailDate) {
        this.lastTailDate = lastTailDate;
    }

    public Integer getLastTailMh() {
        return lastTailMh;
    }

    public void setLastTailMh(Integer lastTailMh) {
        this.lastTailMh = lastTailMh;
    }

    public void run() throws IOException {
        LogTail tailer = new LogTail(1000, new File(logFile), false);
        tailer.add(msg -> {
            Integer mh = parseMh(msg);
            if (mh != null) {
                lastTailDate = new Date();
                lastTailMh = mh;
            }
        });
        new Thread(tailer).start();
    }

    private Integer parseMh(String line) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return new BigDecimal(matcher.group(1)).intValue();
        } else {
            logger.info("didn't find");
        }

        return null;
    }
}
