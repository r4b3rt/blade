package com.hellokaton.blade.mvc.http;

import com.hellokaton.blade.mvc.ui.ModelAndView;
import com.hellokaton.blade.server.NettyHttpConst;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * HttpResponse
 *
 * @author biezhi
 * 2017/5/31
 */
@Slf4j
public class HttpResponse implements Response {

    private final Map<String, String> headers = new HashMap<>();
    private final Set<Cookie> cookies = new HashSet<>();

    private int statusCode = 200;
    private Body body;

    @Override
    public int statusCode() {
        return this.statusCode;
    }

    @Override
    public Response status(int status) {
        this.statusCode = status;
        return this;
    }

    @Override
    public Response contentType(@NonNull String contentType) {
        this.headers.put("Content-Type", contentType);
        return this;
    }

    @Override
    public String contentType() {
        return this.headers.get("Content-Type");
    }

    @Override
    public Map<String, String> headers() {
        return this.headers;
    }

    @Override
    public Response header(String name, String value) {
        this.headers.put(name, value);
        return this;
    }

    @Override
    public Response cookie(@NonNull com.hellokaton.blade.mvc.http.Cookie cookie) {
        Cookie nettyCookie = new io.netty.handler.codec.http.cookie.DefaultCookie(cookie.name(), cookie.value());
        if (cookie.domain() != null) {
            nettyCookie.setDomain(cookie.domain());
        }
        if (cookie.maxAge() > 0) {
            nettyCookie.setMaxAge(cookie.maxAge());
        }
        nettyCookie.setPath(cookie.path());
        nettyCookie.setHttpOnly(cookie.httpOnly());
        nettyCookie.setSecure(cookie.secure());
        this.cookies.add(nettyCookie);
        return this;
    }

    @Override
    public Response cookie(String name, String value) {
        this.cookies.add(new io.netty.handler.codec.http.cookie.DefaultCookie(name, value));
        return this;
    }

    @Override
    public Response cookie(@NonNull String name, @NonNull String value, int maxAge) {
        Cookie nettyCookie = new io.netty.handler.codec.http.cookie.DefaultCookie(name, value);
        nettyCookie.setPath("/");
        nettyCookie.setMaxAge(maxAge);
        this.cookies.add(nettyCookie);
        return this;
    }

    @Override
    public Response cookie(@NonNull String name, @NonNull String value, int maxAge, boolean secured) {
        Cookie nettyCookie = new io.netty.handler.codec.http.cookie.DefaultCookie(name, value);
        nettyCookie.setPath("/");
        nettyCookie.setMaxAge(maxAge);
        nettyCookie.setSecure(secured);
        this.cookies.add(nettyCookie);
        return this;
    }

    @Override
    public Response cookie(@NonNull String path, @NonNull String name, @NonNull String value, int maxAge, boolean secured) {
        Cookie nettyCookie = new io.netty.handler.codec.http.cookie.DefaultCookie(name, value);
        nettyCookie.setMaxAge(maxAge);
        nettyCookie.setSecure(secured);
        nettyCookie.setPath(path);
        this.cookies.add(nettyCookie);
        return this;
    }

    @Override
    public Response removeCookie(@NonNull String name) {
        Optional<Cookie> cookieOpt = this.cookies.stream().filter(cookie -> cookie.name().equals(name)).findFirst();
        cookieOpt.ifPresent(cookie -> {
            cookie.setValue("");
            cookie.setMaxAge(-1);
        });
        Cookie nettyCookie = new io.netty.handler.codec.http.cookie.DefaultCookie(name, "");
        nettyCookie.setMaxAge(-1);
        this.cookies.add(nettyCookie);
        return this;
    }

    @Override
    public Map<String, String> cookies() {
        Map<String, String> map = new HashMap<>(8);
        this.cookies.forEach(cookie -> map.put(cookie.name(), cookie.value()));
        return map;
    }

    @Override
    public Set<Cookie> cookiesRaw() {
        return this.cookies;
    }

    @Override
    public void render(@NonNull ModelAndView modelAndView) {
        this.body = new ViewBody(modelAndView);
    }

    @Override
    public void redirect(@NonNull String newUri) {
        headers.put(NettyHttpConst.LOCATION.toString(), newUri);
        this.status(302);
    }

    @Override
    public void write(File file) throws IOException {
        this.body = ChannelBody.of(file);
    }

    @Override
    public void write(String fileName, File file) throws IOException {
        FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        headers.put("Content-Disposition", "attachment; filename=" + new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
        this.body = new ChannelBody(null, fileChannel);
    }

    @Override
    public ModelAndView modelAndView() {
        if (this.body instanceof ViewBody) {
            return ((ViewBody) this.body).modelAndView();
        }
        return null;
    }

    public HttpResponse(Response response) {
        this.statusCode = response.statusCode();
        if (null != response.headers()) {
            response.headers().forEach(this.headers::put);
        }
        if (null != response.cookies()) {
            response.cookies().forEach((k, v) -> this.cookies.add(new DefaultCookie(k, v)));
        }
    }

    public HttpResponse() {

    }

    @Override
    public Body body() {
        if (null == this.body) {
            return EmptyBody.empty();
        }
        return this.body;
    }

    @Override
    public Response body(Body body) {
        this.body = body;
        return this;
    }

}