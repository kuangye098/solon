package features;

import okhttp3.Response;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.noear.solon.core.handle.Result;
import org.noear.solon.health.HealthChecker;
import org.noear.solon.test.HttpTester;
import org.noear.solon.test.SolonJUnit4ClassRunner;
import org.noear.solon.test.SolonTest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author noear 2021/10/5 created
 */
@RunWith(SolonJUnit4ClassRunner.class)
@SolonTest(TestApp.class)
public class TestDemo extends HttpTester {
    @Test
    public void test1() throws Exception {
        Response resp = path("/healthz").exec("GET");
        System.out.println(resp.body().string());
        assert resp.code() == 200;

        assert path("/healthz").head() == 200;
    }

    @Test
    public void test2() throws Exception {
        HealthChecker.addIndicator("preflight", Result::succeed);
        Response resp = path("/healthz").exec("GET");
        System.out.println(resp.body().string());
        assert resp.code() == 200;

        assert path("/healthz").head() == 200;
    }

    @Test
    public void test3() throws Exception {
        HealthChecker.addIndicator("preflight", Result::succeed);
        HealthChecker.addIndicator("test", Result::failure);


        Response resp = path("/healthz").exec("GET");
        System.out.println(resp.body().string());
        assert resp.code() == 503;

        assert path("/healthz").head() == 503;
    }

    @Test
    public void test4() throws Exception {
        Map<String, Object> preflightMap = new LinkedHashMap<>();
        preflightMap.put("total", 987656789);
        preflightMap.put("free", 6783);
        preflightMap.put("threshold", 7989031);

        HealthChecker.addIndicator("preflight", () -> Result.succeed(preflightMap));
        HealthChecker.addIndicator("test", () -> Result.failure());
        HealthChecker.addIndicator("boom", () -> {
            throw new IllegalStateException();
        });

        path("/healthz").get();
        int code = path("/healthz").head();
        assert code == 500;
    }
}
