package org.noear.solon.view.beetl;

import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.ClasspathResourceLoader;
import org.beetl.core.resource.FileResourceLoader;
import org.beetl.core.statement.ErrorGrammarProgram;
import org.beetl.core.tag.Tag;
import org.noear.solon.Solon;
import org.noear.solon.core.JarClassLoader;
import org.noear.solon.core.event.EventBus;
import org.noear.solon.core.handle.Render;
import org.noear.solon.core.handle.ModelAndView;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.util.ResourceUtil;
import org.noear.solon.core.util.SupplierEx;
import org.noear.solon.view.ViewConfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;

/**
 * Beetl 视图渲染器
 *
 * @author noear
 * @since 1.0
 * */
public class BeetlRender implements Render {

    private static BeetlRender _global;

    public static BeetlRender global() {
        if (_global == null) {
            _global = new BeetlRender();
        }

        return _global;
    }


    Configuration cfg = null;
    GroupTemplate provider = null;
    GroupTemplate provider_debug = null;

    private ClassLoader classLoader;

    //不要要入参，方便后面多视图混用
    //
    public BeetlRender() {
        this(JarClassLoader.global());
    }

    public BeetlRender(ClassLoader classLoader) {
        this.classLoader = classLoader;

        try {
            cfg = Configuration.defaultConfiguration();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        forDebug();
        forRelease();

        Solon.app().shared().forEach((k, v) -> {
            putVariable(k, v);
        });

        Solon.app().onSharedAdd((k, v) -> {
            putVariable(k, v);
        });
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
                FileResourceLoader loader = new FileResourceLoader(dir.getAbsolutePath(), Solon.encoding());
                loader.setAutoCheck(true);
                provider_debug = new GroupTemplate(loader, cfg);

                //通过事件扩展
                EventBus.push(provider_debug);
            }
        } catch (Exception e) {
            EventBus.pushTry(e);
        }
    }

    private void forRelease() {
        if (provider != null) {
            return;
        }

        try {
            ClasspathResourceLoader loader = new ClasspathResourceLoader(classLoader, ViewConfig.getViewPrefix());
            provider = new GroupTemplate(loader, cfg);

            //通过事件扩展
            EventBus.push(provider);
        } catch (Exception e) {
            EventBus.pushTry(e);
        }

    }

    /**
     * 添加共享指令（自定义标签）
     */
    public void putDirective(String name, Class<? extends Tag> clz) {
        try {
            provider.registerTag(name, clz);

            if (provider_debug != null) {
                provider_debug.registerTag(name, clz);
            }
        } catch (Exception e) {
            EventBus.pushTry(e);
        }
    }

    /**
     * 添加共享变量
     */
    public void putVariable(String name, Object value) {
        try {
            provider.getSharedVars().put(name, value);

            if (provider_debug != null) {
                provider_debug.getSharedVars().put(name, value);
            }
        } catch (Exception e) {
            EventBus.pushTry(e);
        }
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

    private void render_mav(ModelAndView mv, Context ctx, SupplierEx<OutputStream> outputStream) throws Throwable {
        if (ctx.contentTypeNew() == null) {
            ctx.contentType("text/html;charset=utf-8");
        }

        if (ViewConfig.isOutputMeta()) {
            ctx.headerSet(ViewConfig.HEADER_VIEW_META, "BeetlRender");
        }

        Template template = null;

        if (provider_debug != null) {
            try {
                template = provider_debug.getTemplate(mv.view());
                if (template != null && template.program instanceof ErrorGrammarProgram) {
                    template = null;
                }
            }catch (Exception e){
                //忽略不计
            }
        }

        if (template == null) {
            template = provider.getTemplate(mv.view());
        }


        template.binding(mv.model());
        template.renderTo(outputStream.get());
    }
}
