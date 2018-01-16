package monitorx.monitorxethminer;

import monitorx.monitorxethminer.tail.LogTail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
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
    private Double lastTailMh;

    Pattern pattern = Pattern.compile(".*?Speed.*?36m(.*?)\\^.*");

    public Date getLastTailDate() {
        return lastTailDate;
    }

    public void setLastTailDate(Date lastTailDate) {
        this.lastTailDate = lastTailDate;
    }

    public Double getLastTailMh() {
        return lastTailMh;
    }

    public void setLastTailMh(Double lastTailMh) {
        this.lastTailMh = lastTailMh;
    }

    public void run() throws IOException {
        LogTail tailer = new LogTail(1000, new File(logFile), false);
        tailer.add(msg -> {
            Double mh = parseMh(msg);
            if (mh != null) {
                lastTailDate = new Date();
                lastTailMh = mh;
            }
        });
        new Thread(tailer).start();
    }

    private Double parseMh(String line) {
        logger.info("parse msg: " + line);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            logger.info(matcher.group(1));
            return Double.parseDouble(matcher.group(1).trim());
        } else {
            logger.info("didn't find");
        }

        return null;
    }
}
