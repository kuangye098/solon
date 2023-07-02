package demo1;

import org.noear.solon.annotation.Component;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Filter;
import org.noear.solon.core.handle.FilterChain;
import org.noear.solon.core.handle.Result;
import org.noear.solon.validation.ValidatorException;
import org.noear.solon.validation.annotation.Logined;


/**
 * @author noear 2022/9/28 created
 */
@Component
public class DemoFilter implements Filter {
    @Override
    public void doFilter(Context ctx, FilterChain chain) throws Throwable {
        try {
            chain.doFilter(ctx);
        } catch (ValidatorException e) {
            if(e.getAnnotation() instanceof Logined){
                ctx.status(401);
            }else {
                ctx.render(Result.failure(e.getCode(), e.getMessage()));
            }
        }
    }
}
