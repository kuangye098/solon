package org.noear.solon.boot.smarthttp.http;

import org.noear.solon.boot.web.ContextBase;
import org.noear.solon.boot.web.Constants;
import org.noear.solon.boot.web.RedirectUtils;
import org.noear.solon.core.NvMap;
import org.noear.solon.Utils;
import org.noear.solon.core.event.EventBus;
import org.noear.solon.core.handle.ContextAsyncListener;
import org.noear.solon.core.handle.UploadedFile;
import org.noear.solon.core.util.IgnoreCaseMap;
import org.noear.solon.core.util.RunUtil;
import org.smartboot.http.common.Cookie;
import org.smartboot.http.common.enums.HttpStatus;
import org.smartboot.http.server.HttpRequest;
import org.smartboot.http.server.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SmHttpContext extends ContextBase {
    private HttpRequest _request;
    private HttpResponse _response;
    protected Map<String, List<UploadedFile>> _fileMap;

    private boolean _isAsync;
    private long _asyncTimeout = 30000L;//默认30秒
    private CompletableFuture<Object> _asyncFuture;
    private List<ContextAsyncListener> _asyncListeners = new ArrayList<>();

    protected HttpRequest innerGetRequest() {
        return _request;
    }

    protected HttpResponse innerGetResponse() {
        return _response;
    }

    protected boolean innerIsAsync() {
        return _isAsync;
    }

    protected List<ContextAsyncListener> innerAsyncListeners() {
        return _asyncListeners;
    }


    public SmHttpContext(HttpRequest request, HttpResponse response, CompletableFuture<Object> future) {
        _request = request;
        _response = response;
        _asyncFuture = future;

        _fileMap = new HashMap<>();
    }

    private boolean _loadMultipartFormData = false;

    private void loadMultipartFormData() throws IOException {
        if (_loadMultipartFormData) {
            return;
        } else {
            _loadMultipartFormData = true;
        }

        //文件上传需要
        if (isMultipartFormData()) {
            MultipartUtil.buildParamsAndFiles(this);
        }
    }

    @Override
    public Object request() {
        return _request;
    }

    private String _ip;

    @Override
    public String ip() {
        if (_ip == null) {
            _ip = header(Constants.HEADER_X_FORWARDED_FOR);

            if (_ip == null) {
                _ip = _request.getRemoteAddr();
            }
        }

        return _ip;
    }

    @Override
    public String method() {
        return _request.getMethod();
    }

    @Override
    public String protocol() {
        return _request.getProtocol();
    }

    private URI _uri;

    @Override
    public URI uri() {
        if (_uri == null) {
            _uri = URI.create(url());
        }
        return _uri;
    }

    private String _url;

    @Override
    public String url() {
        if (_url == null) {
            _url = _request.getRequestURL();
        }

        return _url;
    }

    @Override
    public long contentLength() {
        try {
            return _request.getContentLength();
        } catch (Exception e) {
            EventBus.pushTry(e);
            return 0;
        }
    }

    @Override
    public String queryString() {
        return _request.getQueryString();
    }

    @Override
    public InputStream bodyAsStream() throws IOException {
        return _request.getInputStream();
    }

    @Override
    public String[] paramValues(String key) {
        return _request.getParameterValues(key);
    }

    @Override
    public String param(String key) {
        //要充许为字符串
        //默认不能为null
        return paramMap().get(key);
    }

    private NvMap _paramMap;

    @Override
    public NvMap paramMap() {
        if (_paramMap == null) {
            _paramMap = new NvMap();

            try {
                if (autoMultipart()) {
                    loadMultipartFormData();
                }

                for (Map.Entry<String, String[]> entry : _request.getParameters().entrySet()) {
                    _paramMap.put(entry.getKey(), entry.getValue()[0]);
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        return _paramMap;
    }

    private Map<String, List<String>> _paramsMap;

    @Override
    public Map<String, List<String>> paramsMap() {
        if (_paramsMap == null) {
            _paramsMap = new LinkedHashMap<>();

            _request.getParameters().forEach((k, v) -> {
                _paramsMap.put(k, Arrays.asList(v));
            });
        }

        return _paramsMap;
    }

    @Override
    public Map<String, List<UploadedFile>> filesMap() throws IOException {
        if (isMultipartFormData()) {
            loadMultipartFormData();

            return _fileMap;
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public NvMap cookieMap() {
        if (_cookieMap == null) {
            _cookieMap = new NvMap();

            String _cookieMapStr = header(Constants.HEADER_COOKIE);
            if (_cookieMapStr != null) {
                String[] cookies = _cookieMapStr.split(";");

                for (String c1 : cookies) {
                    String[] ss = c1.trim().split("=");
                    if (ss.length == 2) {
                        _cookieMap.put(ss[0].trim(), ss[1].trim());
                    }

                }
            }
        }

        return _cookieMap;
    }

    private NvMap _cookieMap;


    @Override
    public NvMap headerMap() {
        if (_headerMap == null) {
            _headerMap = new NvMap();

            for (String k : _request.getHeaderNames()) {
                _headerMap.put(k, _request.getHeader(k));
            }
        }

        return _headerMap;
    }

    private NvMap _headerMap;

    @Override
    public Map<String, List<String>> headersMap() {
        if (_headersMap == null) {
            _headersMap = new IgnoreCaseMap<>();

            for (String k : _request.getHeaderNames()) {
                _headersMap.put(k, new ArrayList<>(_request.getHeaders(k)));
            }
        }

        return _headersMap;
    }
    private Map<String, List<String>> _headersMap;

    //=================================

    @Override
    public Object response() {
        return _response;
    }

    @Override
    protected void contentTypeDoSet(String contentType) {
        if (charset != null) {
            if (contentType.indexOf(";") < 0) {
                headerSet(Constants.HEADER_CONTENT_TYPE, contentType + ";charset=" + charset);
                return;
            }
        }

        headerSet(Constants.HEADER_CONTENT_TYPE, contentType);
    }


    private ByteArrayOutputStream _outputStreamTmp;

    @Override
    public OutputStream outputStream() throws IOException {
        sendHeaders(false);

        if (_allows_write) {
            return _response.getOutputStream();
        } else {
            if (_outputStreamTmp == null) {
                _outputStreamTmp = new ByteArrayOutputStream();
            } else {
                _outputStreamTmp.reset();
            }

            return _outputStreamTmp;
        }
    }

    @Override
    public void output(byte[] bytes) {
        try {
            OutputStream out = outputStream();

            if (!_allows_write) {
                return;
            }

            out.write(bytes);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void output(InputStream stream) {
        try {
            OutputStream out = outputStream();

            if (!_allows_write) {
                return;
            }

            Utils.transferTo(stream, out);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }


    @Override
    public void headerSet(String key, String val) {
        //用put才有效
        _response.setHeader(key, val);
    }

    @Override
    public void headerAdd(String key, String val) {
        _response.addHeader(key, val);
    }

    @Override
    public void cookieSet(String key, String val, String domain, String path, int maxAge) {
        Cookie cookie = new Cookie(key, val);

        if (Utils.isNotEmpty(path)) {
            cookie.setPath(path);
        }

        if (maxAge >= 0) {
            cookie.setMaxAge(maxAge);
        }

        if (Utils.isNotEmpty(domain)) {
            cookie.setDomain(domain);
        }

        _response.addCookie(cookie);
    }

    @Override
    public void redirect(String url, int code) {
        url = RedirectUtils.getRedirectPath(url);

        headerSet(Constants.HEADER_LOCATION, url);
        statusDoSet(code);
    }

    @Override
    public int status() {
        return _status;
    }

    private int _status = 200;

    @Override
    protected void statusDoSet(int status) {
        _status = status;
    }

    @Override
    public void contentLength(long size) {
        _response.setContentLength((int) size);
    }

    @Override
    public void flush() throws IOException {
        if (_allows_write) {
            outputStream().flush();
        }
    }

    @Override
    public boolean asyncSupported() {
        return true;
    }

    @Override
    public void asyncStart(long timeout, ContextAsyncListener listener) {
        if (_isAsync == false) {
            _isAsync = true;

            if(listener != null) {
                _asyncListeners.add(listener);
            }

            if (timeout != 0) {
                _asyncTimeout = timeout;
            }

            if (_asyncTimeout > 0) {
                RunUtil.delay(() -> {
                    for (ContextAsyncListener listener1 : _asyncListeners) {
                        try {
                            listener1.onTimeout(this);
                        } catch (IOException e) {
                            EventBus.pushTry(e);
                        }
                    }
                }, _asyncTimeout);
            }
        }
    }


    @Override
    public void asyncComplete() throws IOException {
        if (_isAsync) {
            try {
                innerCommit();
            } finally {
                _asyncFuture.complete(this);
            }
        }
    }


    @Override
    protected void innerCommit() throws IOException {
        if (getHandled() || status() >= 200) {
            sendHeaders(true);
        } else {
            status(404);
            sendHeaders(true);
        }
    }

    private boolean _headers_sent = false;
    private boolean _allows_write = true;

    private void sendHeaders(boolean isCommit) throws IOException {
        if (!_headers_sent) {
            _headers_sent = true;

            if ("HEAD".equals(method())) {
                _allows_write = false;
            }

            if (sessionState() != null) {
                sessionState().sessionPublish();
            }

            _response.setHttpStatus(HttpStatus.valueOf(status()));

            if (isCommit || _allows_write == false) {
                _response.setContentLength(0);
            }
        }
    }
}
