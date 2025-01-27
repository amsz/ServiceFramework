package net.csdn.modules.transport;

import com.google.inject.Inject;
import net.csdn.common.collect.Tuple;
import net.csdn.common.logging.CSLogger;
import net.csdn.common.logging.Loggers;
import net.csdn.common.path.Url;
import net.csdn.common.settings.Settings;
import net.csdn.modules.http.RestRequest;
import net.csdn.modules.http.support.HttpStatus;
import net.csdn.modules.threadpool.ThreadPoolService;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import static net.csdn.common.collections.WowCollections.map;

/**
 * BlogInfo: WilliamZhu
 * Date: 12-5-29
 * Time: 下午5:10
 */


public class DefaultHttpTransportService implements HttpTransportService {
    private final HttpClient httpClient;
    private CSLogger logger = Loggers.getLogger(getClass());
    public final static String charset = "utf-8";
    private final static Tuple<String, String> content_type = new Tuple<String, String>("Content-Type", "application/x-www-form-urlencoded");

    @Inject
    private ThreadPoolService threadPoolService;
    @Inject
    private Settings settings;
    private static final Map<String, String> EMPTY_MAP = map();

    /*
     timeout for how long to wait for a sequence  byte of response data
       `HttpConnectionParams.setConnectionTimeout()`
     timeout for how long to wait to establish a TCP connection
       `HttpConnectionParams.setSoTimeout()`

    */
    public DefaultHttpTransportService() {
        ThreadSafeClientConnManager threadSafeClientConnManager = new ThreadSafeClientConnManager();
        threadSafeClientConnManager.setDefaultMaxPerRoute(1000);
        threadSafeClientConnManager.setMaxTotal(10000);

        HttpParams httpParams = new BasicHttpParams();
//        HttpConnectionParams.setSoTimeout(httpParams, 1000);
//        HttpConnectionParams.setConnectionTimeout(httpParams, 1000);
        httpClient = new DefaultHttpClient(threadSafeClientConnManager, httpParams);
    }


    public SResponse post(Url url, Map data) {
        return post(url, data, EMPTY_MAP);
    }

    public SResponse post(Url url, Map data, Map<String, String> headers) {
        HttpPost post = null;

        try {
            post = new HttpPost(url.toURI());
            UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(mapToNameValuesPairs(data), charset);
            post.setHeader(content_type.v1(), content_type.v2());

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    post.setHeader(entry.getKey(), entry.getValue());
                }
            }

            post.setEntity(urlEncodedFormEntity);

            HttpResponse response = httpClient.execute(post);
            return new SResponse(response.getStatusLine().getStatusCode(), EntityUtils.toString(response.getEntity(), charset), url);
        } catch (IOException e) {
            logger.error("Error when remote search url:[{}] ", url.toString());
            return null;
        } finally {
            if (post != null)
                post.abort();
        }
    }


    public SResponse put(Url url, Map data) {
        return put(url, data, EMPTY_MAP);
    }

    public SResponse put(Url url, Map data, Map<String, String> headers) {
        HttpPut put = null;

        String result = "";
        try {
            put = new HttpPut(url.toURI());
            UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(mapToNameValuesPairs(data), charset);
            put.setHeader(content_type.v1(), content_type.v2());
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    put.setHeader(entry.getKey(), entry.getValue());
                }
            }
            put.setEntity(urlEncodedFormEntity);
            HttpResponse response = httpClient.execute(put);
            return new SResponse(response.getStatusLine().getStatusCode(), EntityUtils.toString(response.getEntity(), charset), url);
        } catch (IOException e) {
            logger.error(getClass().getName() + " error when visit url:[{}] ", url.toString());
            return null;
        } finally {
            if (put != null)
                put.abort();
        }
    }

    public SResponse http(Url url, String jsonData, RestRequest.Method method) {
        return http(url, jsonData, EMPTY_MAP, method);
    }

    public SResponse http(Url url, String jsonData, Map<String, String> headers, RestRequest.Method method) {
        HttpRequestBase httpRequestBase = null;
        try {
            httpRequestBase = createMethod(url.toURI(), jsonData, method);
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    httpRequestBase.setHeader(entry.getKey(), entry.getValue());
                }
            }
            HttpResponse response = httpClient.execute(httpRequestBase);
            return new SResponse(response.getStatusLine().getStatusCode(), EntityUtils.toString(response.getEntity(), charset), url);
        } catch (IOException e) {
//            if (e instanceof HttpHostConnectException) {
//
//            }

            return new SResponse(HttpStatus.HttpStatusServerDown, "", url);
        } finally {
            if (httpRequestBase != null)
                httpRequestBase.abort();
        }
    }

    @Override
    public SResponse http(final Url url, final String jsonData, final RestRequest.Method method, int timeout) {

        SResponse response = (SResponse) threadPoolService.runWithTimeout(timeout, new ThreadPoolService.Run<Object>() {
            @Override
            public Object run() {
                return DefaultHttpTransportService.this.http(url, jsonData, method);
            }
        });
        return response;
    }


    public SResponse http(final Url url, final String jsonData,final Map<String,String> headers,final RestRequest.Method method, int timeout) {

        SResponse response = (SResponse) threadPoolService.runWithTimeout(timeout, new ThreadPoolService.Run<Object>() {
            @Override
            public Object run() {
                return DefaultHttpTransportService.this.http(url, jsonData,headers, method);
            }
        });
        return response;
    }


    public SResponse post(final Url url, final Map data, final Map<String,String> headers,final int timeout) {
        SResponse response = (SResponse) threadPoolService.runWithTimeout(timeout, new ThreadPoolService.Run<Object>() {
            @Override
            public Object run() {
                return DefaultHttpTransportService.this.post(url, data,headers);
            }
        });
        return response;
    }

    public SResponse post(final Url url, final Map data, final int timeout) {
        SResponse response = (SResponse) threadPoolService.runWithTimeout(timeout, new ThreadPoolService.Run<Object>() {
            @Override
            public Object run() {
                return DefaultHttpTransportService.this.post(url, data);
            }
        });
        return response;
    }


    public FutureTask<SResponse> asyncHttp(final Url url, final String jsonData, final RestRequest.Method method) {
        FutureTask<SResponse> getRemoteDataTask = new FutureTask(new Callable<SResponse>() {
            @Override
            public SResponse call() throws Exception {
                return http(url, jsonData, method);
            }
        });
        threadPoolService.executor(ThreadPoolService.Names.SEARCH).execute(getRemoteDataTask);
        return getRemoteDataTask;
    }


    public List<SResponse> asyncHttps(final List<Url> urls, final String jsonData, RestRequest.Method method) {

        List<SResponse> responses = new ArrayList<SResponse>(urls.size());
        List<FutureTask<SResponse>> futureTasks = new ArrayList<FutureTask<SResponse>>(urls.size());
        for (int i = 0; i < urls.size(); i++) {
            final Url url = urls.get(i);
            FutureTask<SResponse> getRemoteDataTask = asyncHttp(url, jsonData, method);
            threadPoolService.executor(ThreadPoolService.Names.SEARCH).execute(getRemoteDataTask);
            futureTasks.add(getRemoteDataTask);
        }
        for (FutureTask<SResponse> futureTask : futureTasks) {
            try {
                SResponse sResponse = futureTask.get();
                responses.add(sResponse);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }

        return responses;
    }


    private HttpRequestBase createMethod(URI uri, String jsonData, RestRequest.Method method) {
        HttpRequestBase httpRequestBase;
        if (method == RestRequest.Method.GET) {
            httpRequestBase = new HttpGet(uri);
        } else if (method == RestRequest.Method.PUT) {
            httpRequestBase = new HttpPut(uri);
            if (jsonData != null && !jsonData.isEmpty())
                ((HttpPut) httpRequestBase).setEntity(stringEntity(jsonData));
        } else if (method == RestRequest.Method.DELETE) {
            httpRequestBase = new HttpDelete(uri);
        } else if (method == RestRequest.Method.HEAD) {
            httpRequestBase = new HttpHead(uri);
        } else if (method == RestRequest.Method.OPTIONS) {
            httpRequestBase = new HttpOptions(uri);
        } else {
            httpRequestBase = new HttpPost(uri);
            if (jsonData != null && !jsonData.isEmpty())
                ((HttpPost) httpRequestBase).setEntity(stringEntity(jsonData));
        }
        return httpRequestBase;
    }

    private StringEntity stringEntity(String jsonData) {
        try {
            return new StringEntity(jsonData, charset);
        } catch (UnsupportedEncodingException e) {
            logger.error(getClass().getName() + "UnsupportedEncodingException e=>" + e.getMessage());
            return null;
        }
    }


    private List<NameValuePair> mapToNameValuesPairs(Map data) {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        for (Object key : data.keySet()) {
            params.add(new BasicNameValuePair((String) key, (String) data.get(key)));
        }
        return params;
    }


    public void shutdownTransport() {
        httpClient.getConnectionManager().shutdown();
    }


}
