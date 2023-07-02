package org.noear.solon.validation.annotation;

import org.noear.solon.Utils;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Result;
import org.noear.solon.validation.util.StringUtils;
import org.noear.solon.validation.Validator;

/**
 *
 * @author noear
 * @since 1.0
 * */
public class DecimalMaxValidator implements Validator<DecimalMax> {
    public static final DecimalMaxValidator instance = new DecimalMaxValidator();

    @Override
    public String message(DecimalMax anno) {
        return anno.message();
    }

    @Override
    public Class<?>[] groups(DecimalMax anno) {
        return anno.groups();
    }

    @Override
    public Result validateOfValue(DecimalMax anno, Object val0, StringBuilder tmp) {
        if (val0 != null && val0 instanceof Number == false) {
            return Result.failure();
        }

        Number val = (Number) val0;

        if (verify(anno, val) == false) {
            return Result.failure();
        } else {
            return Result.succeed();
        }
    }

    @Override
    public Result validateOfContext(Context ctx, DecimalMax anno, String name, StringBuilder tmp) {
        String val = ctx.param(name);

        //如果为空，算通过（交由 @NotNull 或 @NotEmpty 或 @NotBlank 进一步控制）
        if (Utils.isEmpty(val)) {
            return Result.succeed();
        }

        if (StringUtils.isNumber(val) == false) {
            return Result.failure(name);
        }

        if (verify(anno, Double.parseDouble(val)) == false) {
            return Result.failure(name);
        } else {
            return Result.succeed();
        }
    }

    private boolean verify(DecimalMax anno, Number val) {
        //如果为空，算通过（交由 @NotNull 进一步控制）
        if (val == null) {
            return true;
        }

        return val.doubleValue() <= anno.value();
    }
}
