package monitorx.monitorxethminer;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * @author xma
 */
public class HTTPUtil {

    private static Logger logger = LoggerFactory.getLogger(HTTPUtil.class);

    private static Integer SUCCESS = 200;

    private static ContentType formUTF8 = ContentType.create("application/x-www-form-urlencoded", Consts.UTF_8);

    /**
     * 发送POST请求
     *
     * @param contentType    请求头的type
     * @param url            请求的地址
     * @param params         参数
     * @param socketTimeout  接口超时
     * @param connectTimeout 连接超时
     * @return 返回HTTP返回的信息
     * @throws IOException HTTP异常
     */
    public static String sendPost(ContentType contentType, String url, Map<String, Object> params, Integer socketTimeout, Integer connectTimeout) throws IOException {
        List<NameValuePair> parameters = new ArrayList<>();
        if (params != null) {
            for (String key : params.keySet()) {
                if (params.get(key) != null) {
                    parameters.add(new BasicNameValuePair(key, params.get(key).toString()));
                }
            }
        }

        HttpRequestRetryHandler myRetryHandler = (exception, executionCount, context) -> false;
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeout).setConnectionRequestTimeout(1000)
                .setSocketTimeout(socketTimeout).setMaxRedirects(0).build();
        CloseableHttpClient httpclient = HttpClients.custom().setRetryHandler(myRetryHandler).setDefaultRequestConfig(requestConfig).build();
        HttpPost post = new HttpPost(url);
        post.setHeader("Content-Type", contentType.toString());
        post.setEntity(new UrlEncodedFormEntity(parameters, contentType.getCharset()));
        CloseableHttpResponse response = httpclient.execute(post);
        checkStatus(response.getStatusLine().getStatusCode());
        HttpEntity entity = response.getEntity();
        return EntityUtils.toString(entity, contentType.getCharset());
    }

    /**
     * 发送POST请求，默认Content-Type application/json; charset=UTF-8
     *
     * @param url            请求的地址
     * @param body           body参数
     * @param socketTimeout  接口超时
     * @param connectTimeout 连接超时
     * @return 返回HTTP返回的信息
     * @throws IOException HTTP异常
     */
    public static String sendBodyPost(String url, String body, Integer socketTimeout, Integer connectTimeout) throws IOException {
        return sendBodyPost(ContentType.APPLICATION_JSON, url, body, socketTimeout, connectTimeout);
    }

    public static String sendBodyPost(ContentType contentType, String url, String body, Integer socketTimeout, Integer connectTimeout) throws IOException {
        HttpRequestRetryHandler myRetryHandler = (exception, executionCount, context) -> false;

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeout).setConnectionRequestTimeout(1000)
                .setSocketTimeout(socketTimeout).setMaxRedirects(0).build();
        CloseableHttpClient httpclient = HttpClients.custom().setRetryHandler(myRetryHandler).setDefaultRequestConfig(requestConfig).build();
        HttpPost post = new HttpPost(url);
        post.setHeader("Content-Type", contentType.toString());
        post.setEntity(new StringEntity(body, contentType));
        CloseableHttpResponse response = httpclient.execute(post);
        checkStatus(response.getStatusLine().getStatusCode());
        HttpEntity entity = response.getEntity();
        return EntityUtils.toString(entity, contentType.getCharset());
    }

    public static String sendBodyPost(ContentType contentType, String url, String body, String platform, String requestType) throws IOException {
        logger.info("请求" + platform + " 地址:" + url + " 请求类型:" + requestType + " body信息:" + body);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String res = sendBodyPost(contentType, url, body, 20000, 20000);
        stopWatch.stop();
        logger.info("返回值为:" + res);
        logger.info(platform + " " + requestType + " 本次请求耗时:(duration:" + stopWatch.getTime() + ")");
        return res;
    }

    /**
     * 发送HTTP POST请求，默认超时20s，默认Content-Type application/json; charset=UTF-8
     *
     * @param url  请求的地址
     * @param body body参数
     * @return 返回HTTP返回的信息
     * @throws IOException HTTP异常
     */
    public static String sendBodyPost(String url, String body) throws IOException {
        return sendBodyPost(url, body, 20000, 20000);
    }

    /**
     * 发送HTTP POST请求，默认超时20s，默认Content-Type application/x-www-form-urlencoded; charset=UTF-8
     *
     * @param url         请求的地址
     * @param params      参数
     * @param platform    平台类型
     * @param requestType 请求类型
     * @return 返回HTTP返回的信息
     * @throws IOException HTTP异常
     */
    public static String sendPost(String url, Map<String, Object> params, String platform, String requestType) throws IOException {
        return sendPost(formUTF8, url, params, platform, requestType);
    }

    /**
     * 发送HTTP POST请求，默认超时20s，默认Content-Type application/x-www-form-urlencoded; charset=UTF-8
     *
     * @param url         请求的地址
     * @param platform    平台类型
     * @param requestType 请求类型
     * @return 返回HTTP返回的信息
     * @throws IOException HTTP异常
     */
    public static String sendPost(String url, String platform, String requestType) throws IOException {
        logger.info("请求" + platform + " 地址:" + url + " 请求类型:" + requestType);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String res = sendPost(formUTF8, url, null, 20000, 20000);
        stopWatch.stop();
        logger.info("返回值为:" + res);
        logger.info(platform + " " + requestType + " 本次请求耗时:(duration:" + stopWatch.getTime() + ")");
        return res;
    }


    /**
     * 发送HTTP POST请求，默认超时20s，默认Content-Type application/x-www-form-urlencoded; charset=UTF-8
     *
     * @param url    请求的地址
     * @param params 参数
     * @return 返回HTTP返回的信息
     * @throws IOException HTTP异常
     */
    public static String sendPost(String url, Map<String, Object> params) throws IOException {
        return sendPost(formUTF8, url, params, 20000, 20000);
    }

    /**
     * 发送HTTP POST请求，默认超时20s
     *
     * @param contentType 请求头的type
     * @param url         请求的地址
     * @param params      参数
     * @return 返回HTTP返回的信息
     * @throws IOException HTTP异常
     */
    public static String sendPost(ContentType contentType, String url, Map<String, Object> params) throws IOException {
        return sendPost(contentType, url, params, 20000, 20000);
    }

    public static String sendPost(ContentType contentType, String url, Map<String, Object> params, String platform, String requestType) throws IOException {
        logger.info("请求:" + platform + " 请求类型:" + requestType + " 地址:" + url + " 参数:" + JSON.toJSONString(params));
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String res = sendPost(contentType, url, params, 20000, 20000);
        stopWatch.stop();
        logger.info("返回值为:" + res);
        logger.info(platform + " " + requestType + " 本次请求耗时:(duration:" + stopWatch.getTime() + ")");
        return res;
    }

    public static String sendGet(String url, Map<String, Object> params, String platform, String requestType) throws IOException {
        logger.info("请求:" + platform + " 请求类型:" + requestType + " 地址:" + url + " 参数:" + JSON.toJSONString(params));
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String res = sendGet(url, params, 20000);
        stopWatch.stop();
        logger.info("返回值为:" + res);
        logger.info(platform + " " + requestType + " 本次请求耗时:(duration:" + stopWatch.getTime() + ")");
        return res;
    }

    public static String sendGet(String url, String platform, String requestType) throws IOException {
        logger.info("请求:" + platform + " 请求类型:" + requestType + " 地址:" + url);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String res = sendGet(url);
        stopWatch.stop();
        logger.info("返回值为:" + res);
        logger.info(platform + " " + requestType + " 本次请求耗时:(duration:" + stopWatch.getTime() + ")");
        return res;
    }

    public static String sendGet(String url, Map<String, Object> params) throws IOException {
        return sendGet(url, params, 20000);
    }

    public static String sendGet(String url, Integer timeout) throws IOException {
        return sendGet(url, null, timeout);
    }

    public static String sendGet(String url) throws IOException {
        return sendGet(url, null, 20000);
    }

    public static String sendGet(String url, Map<String, Object> params, Integer timeout) throws IOException {
        String realUrl = url + getUrl(url.contains("?"), params);
        HttpRequestRetryHandler myRetryHandler = (exception, executionCount, context) -> false;
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeout).setConnectionRequestTimeout(1000)
                .setSocketTimeout(timeout).setMaxRedirects(0).build();
        CloseableHttpClient httpclient = HttpClients.custom().setRetryHandler(myRetryHandler).setDefaultRequestConfig(requestConfig).build();
        HttpGet get = new HttpGet(realUrl);
        CloseableHttpResponse response = httpclient.execute(get);
        checkStatus(response.getStatusLine().getStatusCode());
        HttpEntity entity = response.getEntity();
        return EntityUtils.toString(entity);
    }

    public static String getUrl(Boolean notFirst, Map<String, Object> systemMap) throws UnsupportedEncodingException {
        if (systemMap == null || systemMap.size() == 0) {
            return StringUtils.EMPTY;
        }

        StringBuilder url = new StringBuilder();
        boolean isFirst = !notFirst;
        for (String key : systemMap.keySet()) {
            String split = "&";
            if (isFirst) {
                isFirst = false;
                split = "?";
            }
            if (systemMap.get(key) != null) {
                url.append(split).append(key).append("=").append(URLEncoder.encode(systemMap.get(key).toString(), "UTF-8"));
            }
        }
        return url.toString();
    }

    private static void checkStatus(Integer status) throws IOException {
        if (!Objects.equals(status, SUCCESS)) {
            throw new IOException(String.valueOf(status));
        }
    }

    public static String buildParamStr(HashMap<String, String> param) {
        String paramStr = "";
        Object[] keyArray = param.keySet().toArray();
        for (int i = 0; i < keyArray.length; i++) {
            String key = (String) keyArray[i];

            if (0 == i) {
                paramStr += (key + "=" + param.get(key));
            } else {
                paramStr += ("&" + key + "=" + param.get(key));
            }
        }

        return paramStr;
    }

    public static String buildParamStrWithEncode(HashMap<String, String> param) throws UnsupportedEncodingException {
        String paramStr = "";
        Object[] keyArray = param.keySet().toArray();
        for (int i = 0; i < keyArray.length; i++) {
            String key = (String) keyArray[i];

            if (0 == i) {
                paramStr += (URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(param.get(key), "UTF-8"));

            } else {
                paramStr += ("&" + URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(param.get(key), "UTF-8"));
            }
        }

        return paramStr;
    }
}
