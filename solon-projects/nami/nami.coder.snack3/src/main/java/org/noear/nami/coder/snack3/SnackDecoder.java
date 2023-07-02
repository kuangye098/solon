package org.noear.nami.coder.snack3;

import org.noear.nami.Context;
import org.noear.nami.Decoder;
import org.noear.nami.EncoderTyped;
import org.noear.nami.Result;
import org.noear.nami.common.Constants;
import org.noear.nami.common.ContentTypes;
import org.noear.snack.ONode;

import java.lang.reflect.Type;

public class SnackDecoder implements Decoder {
    public static final SnackDecoder instance = new SnackDecoder();

    @Override
    public String enctype() {
        return ContentTypes.JSON_VALUE;
    }

    @Override
    public <T> T decode(Result rst, Type type) {
        String str = rst.bodyAsString();

        Object returnVal = null;
        try {
            if (str == null) {
                return (T) str;
            }
            returnVal = ONode.deserialize(str, type);

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
        if (ctx.config.getEncoder() instanceof EncoderTyped) {
            ctx.headers.put(Constants.HEADER_SERIALIZATION, Constants.AT_TYPE_JSON);
        }

        ctx.headers.put(Constants.HEADER_ACCEPT, ContentTypes.JSON_VALUE);
    }
}
