/**
 * Copyright (c) 2022, katon (hellokaton@gmail.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hellokaton.blade.mvc.wrapper;

import com.hellokaton.blade.kit.DateKit;
import com.hellokaton.blade.mvc.BladeConst;
import com.hellokaton.blade.mvc.WebContext;
import com.hellokaton.blade.server.NettyHttpConst;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.*;

import java.io.*;
import java.nio.channels.FileChannel;

/**
 * OutputStream Wrapper
 *
 * @author biezhi
 * @date 2017/8/2
 */
public class OutputStreamWrapper implements Closeable, Flushable {

    private OutputStream          outputStream;
    private File                  file;
    private ChannelHandlerContext ctx;

    public OutputStreamWrapper(OutputStream outputStream, File file) {
        this.outputStream = outputStream;
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public OutputStream getRaw() {
        return outputStream;
    }

    public void write(byte[] b) throws IOException {
        outputStream.write(b);
    }

    public void write(int b) throws IOException {
        outputStream.write(b);
    }

    public void write(byte[] bytes, int off, int len) throws IOException {
        outputStream.write(bytes, off, len);
    }

    @Override
    public void flush() throws IOException {
        outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        try {
            this.flush();
            FileChannel file       = new FileInputStream(this.file).getChannel();
            long        fileLength = file.size();

            HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            httpResponse.headers().set(NettyHttpConst.CONTENT_LENGTH, fileLength);
            httpResponse.headers().set(NettyHttpConst.DATE, DateKit.gmtDate());
            httpResponse.headers().set(NettyHttpConst.SERVER, "blade/" + BladeConst.VERSION);

            boolean keepAlive = WebContext.request().keepAlive();
            if (keepAlive) {
                httpResponse.headers().set(NettyHttpConst.CONNECTION, NettyHttpConst.KEEP_ALIVE);
            }

            // Write the initial line and the header.
            ctx.write(httpResponse);
            ctx.write(new DefaultFileRegion(file, 0, fileLength), ctx.newProgressivePromise());
            // Write the end marker.
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        } finally {
            if(null != outputStream){
                outputStream.close();
            }
        }
    }

}