package org.noear.nami.coder.hessian;

import com.alibaba.com.caucho.hessian.io.Hessian2Input;
import org.noear.nami.Decoder;
import org.noear.nami.Context;
import org.noear.nami.Result;
import org.noear.nami.common.Constants;
import org.noear.nami.common.ContentTypes;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Type;

/**
 * Hessian 解码器
 *
 * @author noear
 * @since 1.2
 * */
public class HessianDecoder implements Decoder {
    public static final HessianDecoder instance = new HessianDecoder();

    @Override
    public String enctype() {
        return ContentTypes.HESSIAN_VALUE;
    }


    @Override
    public <T> T decode(Result rst, Type type) {
        Hessian2Input hi = new Hessian2Input(new ByteArrayInputStream(rst.body()));

        Object returnVal = null;
        try {
            if (rst.body().length == 0) {
                return null;
            }

            returnVal = hi.readObject();
        } catch (Throwable ex) {
            returnVal = ex;
        }

        if (returnVal != null && returnVal instanceof Throwable) {
            if (returnVal instanceof RuntimeException) {
                throw (RuntimeException) returnVal;
            } else {
                throw new RuntimeException((Throwable) returnVal);
            }
        } else {
            return (T) returnVal;
        }
    }

    @Override
    public void pretreatment(Context ctx) {
        ctx.headers.put(Constants.HEADER_SERIALIZATION, Constants.AT_HESSIAN);
        ctx.headers.put(Constants.HEADER_ACCEPT, ContentTypes.HESSIAN_VALUE);
    }
}
