package org.noear.solon.view.velocity;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.runtime.directive.Directive;
import org.noear.solon.Solon;
import org.noear.solon.core.event.EventBus;
import org.noear.solon.core.handle.ModelAndView;
import org.noear.solon.core.handle.Render;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.util.ResourceUtil;
import org.noear.solon.core.util.SupplierEx;
import org.noear.solon.view.ViewConfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Velocity 视图渲染器
 *
 * @author noear
 * @since 1.0
 * */
public class VelocityRender implements Render {
    private static VelocityRender _global;

    public static VelocityRender global() {
        if (_global == null) {
            _global = new VelocityRender();
        }

        return _global;
    }

    private RuntimeInstance provider;
    private RuntimeInstance provider_debug;
    private Map<String, Object> _sharedVariable = new HashMap<>();

    //不要要入参，方便后面多视图混用
    //
    public VelocityRender() {

        forDebug();
        forRelease();

        engineInit(provider);
        engineInit(provider_debug);

        //通过事件扩展
        EventBus.push(provider_debug);
        //通过事件扩展
        EventBus.push(provider);

        Solon.app().shared().forEach((k, v) -> {
            putVariable(k, v);
        });

        Solon.app().onSharedAdd((k, v) -> {
            putVariable(k, v);
        });
    }

    private void engineInit(RuntimeInstance ve) {
        if (ve == null) {
            return;
        }

        ve.setProperty(Velocity.ENCODING_DEFAULT, Solon.encoding());
        ve.setProperty(Velocity.INPUT_ENCODING, Solon.encoding());

        Solon.cfg().forEach((k, v) -> {
            String key = k.toString();
            if (key.startsWith("velocity")) {
                ve.setProperty(key, v);
            }
        });

        ve.init();
    }

    private void forDebug() {
        if (Solon.cfg().isDebugMode() == false) {
            return;
        }

        if (Solon.cfg().isFilesMode() == false) {
            return;
        }

        if (provider_debug != null) {
            return;
        }

        //添加调试模式
        URL rooturi = ResourceUtil.getResource("/");
        if (rooturi == null) {
            return;
        }

        provider_debug = new RuntimeInstance();

        String rootdir = rooturi.toString().replace("target/classes/", "");
        File dir = null;

        if (rootdir.startsWith("file:")) {
            String dir_str = rootdir + "src/main/resources" + ViewConfig.getViewPrefix();
            dir = new File(URI.create(dir_str));
            if (!dir.exists()) {
                dir_str = rootdir + "src/main/webapp" + ViewConfig.getViewPrefix();
                dir = new File(URI.create(dir_str));
            }
        }

        try {
            if (dir != null && dir.exists()) {
                provider_debug.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH, dir.getAbsolutePath() + File.separatorChar);
            } else {
                //如果没有找到文件，则使用发行模式
                //
                forRelease();
            }
        } catch (Exception e) {
            EventBus.pushTry(e);
        }
    }

    private void forRelease() {
        if (provider != null) {
            return;
        }

        provider = new RuntimeInstance();

        URL resource = ResourceUtil.getResource(ViewConfig.getViewPrefix());
        if (resource == null) {
            return;
        }

        String root_path = resource.getPath();

        provider.setProperty(Velocity.FILE_RESOURCE_LOADER_CACHE, true);
        provider.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH, root_path);
    }

    /**
     * 添加共享指令（自定义标签）
     */
    public <T extends Directive> void putDirective(T obj) {
        provider.addDirective(obj);

        if (provider_debug != null) {
            provider_debug.addDirective(obj);
        }
    }

    /**
     * 添加共享变量
     */
    public void putVariable(String key, Object obj) {
        _sharedVariable.put(key, obj);
    }


    @Override
    public void render(Object obj, Context ctx) throws Throwable {
        if (obj == null) {
            return;
        }

        if (obj instanceof ModelAndView) {
            render_mav((ModelAndView) obj, ctx, () -> ctx.outputStream());
        } else {
            ctx.output(obj.toString());
        }
    }

    @Override
    public String renderAndReturn(Object obj, Context ctx) throws Throwable {
        if (obj == null) {
            return null;
        }

        if (obj instanceof ModelAndView) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            render_mav((ModelAndView) obj, ctx, () -> outputStream);

            return outputStream.toString();
        } else {
            return obj.toString();
        }
    }

    public void render_mav(ModelAndView mv, Context ctx, SupplierEx<OutputStream> outputStream) throws Throwable {
        if (ctx.contentTypeNew() == null) {
            ctx.contentType("text/html;charset=utf-8");
        }

        if (ViewConfig.isOutputMeta()) {
            ctx.headerSet(ViewConfig.HEADER_VIEW_META, "VelocityRender");
        }

        String view = mv.view();

        //取得velocity的模版
        Template template = null;

        if (provider_debug != null) {
            try {
                template = provider_debug.getTemplate(view, Solon.encoding());
            } catch (ResourceNotFoundException ex) {
                //忽略不计
            }
        }

        if (template == null) {
            template = provider.getTemplate(view, Solon.encoding());
        }

        // 取得velocity的上下文context
        VelocityContext vc = new VelocityContext(mv.model());
        _sharedVariable.forEach((k, v) -> vc.put(k, v));

        // 输出流
        PrintWriter writer = new PrintWriter(outputStream.get());
        // 转换输出
        template.merge(vc, writer);
        writer.flush();
    }
}
