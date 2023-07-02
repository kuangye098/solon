package org.noear.solon.core.handle;

import org.noear.solon.core.NvMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.*;

/**
 * 通用上下文，空对象
 *
 * @author noear
 * @since 1.0
 * */
public class ContextEmpty extends Context {
    public static Context create(){
        return new ContextEmpty();
    }

    public ContextEmpty(){
        sessionState = new SessionStateEmpty();
    }

    private Object request = null;

    @Override
    public Object request() {
        return request;
    }

    public ContextEmpty request(Object request){
        this.request = request;
        return this;
    }


    @Override
    public String ip() {
        return null;
    }

    @Override
    public String method() {
        return null;
    }

    @Override
    public String protocol() {
        return null;
    }

    @Override
    public URI uri() {
        return null;
    }

    @Override
    public String url() {
        return null;
    }

    @Override
    public long contentLength() {
        return 0;
    }

    @Override
    public String contentType() {
        return null;
    }

    @Override
    public String contentCharset() {
        return null;
    }

    @Override
    public String queryString() {
        return null;
    }

    @Override
    public InputStream bodyAsStream() throws IOException {
        return null;
    }

    @Override
    public String[] paramValues(String key) {
        return new String[0];
    }

    @Override
    public String param(String key) {
        return paramMap().get(key);
    }

    private NvMap paramMap = null;
    @Override
    public NvMap paramMap() {
        if(paramMap == null){
            paramMap = new NvMap();
        }
        return paramMap;
    }

    @Override
    public Map<String, List<String>> paramsMap() {
        return null;
    }

    Map<String, List<UploadedFile>> filesMap = null;
    @Override
    public Map<String, List<UploadedFile>> filesMap() throws IOException {
        if (filesMap == null) {
            filesMap = new LinkedHashMap<>();
        }

        return filesMap;
    }


    @Override
    public String cookie(String key) {
        return cookieMap().get(key);
    }

    @Override
    public String cookie(String key, String def) {
        return cookieMap().getOrDefault(key,def);
    }

    NvMap cookieMap = null;
    @Override
    public NvMap cookieMap() {
        if(cookieMap == null){
            cookieMap = new NvMap();
        }
        return cookieMap;
    }

    private NvMap headerMap = null;
    @Override
    public NvMap headerMap() {
        if(headerMap == null){
            headerMap = new NvMap();
        }
        return headerMap;
    }

    private Map<String, List<String>> headersMap;
    @Override
    public Map<String, List<String>> headersMap() {
        if (headersMap == null) {
            headersMap = new LinkedHashMap<>();
        }
        return headersMap;
    }

    @Override
    public String sessionId() {
        return null;
    }

    @Override
    public Object session(String name) {
        return null;
    }

    @Override
    public <T> T sessionOrDefault(String name, T def) {
        return null;
    }

    @Override
    public int sessionAsInt(String name) {
        return 0;
    }

    @Override
    public int sessionAsInt(String name, int def) {
        return 0;
    }

    @Override
    public long sessionAsLong(String name) {
        return 0;
    }

    @Override
    public long sessionAsLong(String name, long def) {
        return 0;
    }

    @Override
    public double sessionAsDouble(String name) {
        return 0;
    }

    @Override
    public double sessionAsDouble(String name, double def) {
        return 0;
    }

    @Override
    public void sessionSet(String name, Object val) {

    }

    @Override
    public void sessionRemove(String name) {

    }

    @Override
    public void sessionClear() {

    }

    private Object response = null;

    @Override
    public Object response() {
        return response;
    }

    public ContextEmpty response(Object response) {
        this.response = response;
        return this;
    }

    @Override
    protected void contentTypeDoSet(String contentType) {

    }

    @Override
    public void output(byte[] bytes) {

    }

    @Override
    public void output(InputStream stream) {

    }

    @Override
    public OutputStream outputStream() {
        return null;
    }

    @Override
    public void headerSet(String key, String val) {
        headerMap().put(key,val);
    }

    @Override
    public void headerAdd(String key, String val) {
        headerMap().put(key,val);
    }

    @Override
    public void cookieSet(String key, String val, String domain, String path, int maxAge) {
        cookieMap().put(key,val);
    }

    @Override
    public void redirect(String url, int code) {

    }

    private int status = 200;
    @Override
    public int status() {
        return status;
    }

    @Override
    protected void statusDoSet(int status) {
        this.status = status;
    }

    @Override
    public void flush() throws IOException{

    }

    @Override
    public boolean asyncSupported() {
        return false;
    }

    @Override
    public void asyncStart(long timeout, ContextAsyncListener listener)  {
        throw new UnsupportedOperationException();
    }

    @Override
    public void asyncComplete() {
        throw new UnsupportedOperationException();
    }
}
