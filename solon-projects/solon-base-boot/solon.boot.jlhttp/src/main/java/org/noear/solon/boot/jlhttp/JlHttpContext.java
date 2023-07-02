package org.noear.solon.boot.jlhttp;

import org.noear.solon.Utils;
import org.noear.solon.boot.web.ContextBase;
import org.noear.solon.boot.web.Constants;
import org.noear.solon.boot.web.RedirectUtils;
import org.noear.solon.core.NvMap;
import org.noear.solon.core.event.EventBus;
import org.noear.solon.core.handle.ContextAsyncListener;
import org.noear.solon.core.handle.UploadedFile;
import org.noear.solon.core.util.IgnoreCaseMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class JlHttpContext extends ContextBase {
    private HTTPServer.Request _request;
    private HTTPServer.Response _response;
    protected Map<String, List<UploadedFile>> _fileMap;

    private boolean _isAsync;
    private long _asyncTimeout = 30000L;//默认30秒
    private CompletableFuture<Object> _asyncFuture;
    private List<ContextAsyncListener> _asyncListeners = new ArrayList<>();

    protected boolean innerIsAsync() {
        return _isAsync;
    }

    public JlHttpContext(HTTPServer.Request request, HTTPServer.Response response) {
        _request = request;
        _response = response;
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
                _ip = _request.getSocket().getInetAddress().getHostAddress();
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
        return _request.getVersion();
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
            _url = _request.getURI().toString();

            if (_url != null) {
                if (_url.startsWith("/")) {
                    String host = header(Constants.HEADER_HOST);

                    if (host == null) {
                        host = header(":authority");
                        String scheme = header(":scheme");

                        if (host == null) {
                            host = "localhost";
                        }

                        if (scheme != null) {
                            _url = "https://" + host + _url;
                        } else {
                            _url = scheme + "://" + host + _url;
                        }

                    } else {
                        _url = "http://" + host + _url;
                    }
                }

                int idx = _url.indexOf("?");
                if (idx > 0) {
                    _url = _url.substring(0, idx);
                }
            }
        }

        return _url;
    }

    @Override
    public long contentLength() {
        try {
            return _request.getBody().available();
        } catch (Exception e) {
            EventBus.pushTry(e);
            return 0;
        }
    }
    @Override
    public String queryString() {
        return _request.getURI().getQuery();
    }

    @Override
    public InputStream bodyAsStream() throws IOException {
        return _request.getBody();
    }

    @Override
    public String[] paramValues(String key) {
        List<String> list = paramsMap().get(key);
        if (list == null) {
            return null;
        }

        return list.toArray(new String[list.size()]);
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

                _paramMap.putAll(_request.getParams());
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

            try {
                for (String[] kv : _request.getParamsList()) {
                    List<String> list = _paramsMap.get(kv[0]);
                    if (list == null) {
                        list = new ArrayList<>();
                        _paramsMap.put(kv[0], list);
                    }

                    list.add(kv[1]);
                }
            } catch (Exception e) {
                EventBus.pushTry(e);
                return null;
            }
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
            _cookieMap = new NvMap(_request.getHeaders().getParams("Cookie"));
        }

        return _cookieMap;
    }

    private NvMap _cookieMap;


    @Override
    public NvMap headerMap() {
        if (_headerMap == null) {
            _headerMap = new NvMap();

            HTTPServer.Headers headers = _request.getHeaders();

            if (headers != null) {
                for (HTTPServer.Header h : headers) {
                    _headerMap.put(h.getName(), h.getValue());
                }
            }
        }

        return _headerMap;
    }

    private NvMap _headerMap;



    @Override
    public Map<String, List<String>> headersMap() {
        if (_headersMap == null) {
            _headersMap = new IgnoreCaseMap<>();

            HTTPServer.Headers headers = _request.getHeaders();

            if (headers != null) {
                for (HTTPServer.Header h : headers) {
                    List<String> values = _headersMap.get(h.getName());
                    if (values == null) {
                        values = new ArrayList<>();
                        _headersMap.put(h.getName(), values);
                    }

                    values.add(h.getValue());
                }
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
            return _response.getBody();
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
        _response.getHeaders().replace(key, val);
    }

    @Override
    public void headerAdd(String key, String val) {
        _response.getHeaders().add(key, val);
    }

    @Override
    public void cookieSet(String key, String val, String domain, String path, int maxAge) {
        StringBuilder sb = new StringBuilder();
        sb.append(key).append("=").append(val).append(";");

        if (Utils.isNotEmpty(path)) {
            sb.append("path=").append(path).append(";");
        }

        if (maxAge >= 0) {
            sb.append("max-age=").append(maxAge).append(";");
        }

        if (Utils.isNotEmpty(domain)) {
            sb.append("domain=").append(domain.toLowerCase()).append(";");
        }

        headerAdd(Constants.HEADER_SET_COOKIE, sb.toString());
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
        _status = status; //jlhttp 的 状态，由 上下文代理 负责
    }

    @Override
    public void flush() throws IOException {
        if (_allows_write) {
            outputStream();
            _response.flush();
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

            _asyncFuture = new CompletableFuture<>();

            if (listener != null) {
                _asyncListeners.add(listener);
            }

            if (timeout != 0) {
                _asyncTimeout = timeout;
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

    protected void asyncAwait() throws InterruptedException, ExecutionException, IOException {
        if(_isAsync){
            if (_asyncTimeout > 0) {
                try {
                    _asyncFuture.get(_asyncTimeout, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    for (ContextAsyncListener listener1 : _asyncListeners) {
                        listener1.onTimeout(this);
                    }
                }
            } else {
                _asyncFuture.get();
            }
        }
    }



    //jlhttp 需要先输出 header ，但是 header 后面可能会有变化；所以不直接使用  response.getOutputStream()
    @Override
    protected void innerCommit() throws IOException {
        if (getHandled() || status() >= 200) {
            sendHeaders(true);
        } else {
            if (!_response.headersSent()) {
                _response.sendError(404);
            }
        }
    }

    private boolean _allows_write = true;

    private void sendHeaders(boolean isCommit) throws IOException {
        if (!_response.headersSent()) {
            if ("HEAD".equals(method())) {
                _allows_write = false;
            }

            if (sessionState() != null) {
                sessionState().sessionPublish();
            }

            if (isCommit || _allows_write == false) {
                _response.sendHeaders(status(), 0L, -1, null, null, null);
            } else {
                String tmp = _response.getHeaders().get(Constants.HEADER_CONTENT_LENGTH);

                if (tmp != null) {
                    _response.sendHeaders(status(), Long.parseLong(tmp), -1, null, null, null);
                } else {
                    _response.sendHeaders(status(), -1L, -1, null, null, null);
                }
            }
        }
    }
}
