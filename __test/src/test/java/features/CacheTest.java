package features;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.noear.solon.test.HttpTester;
import org.noear.solon.test.SolonJUnit4ClassRunner;
import org.noear.solon.test.SolonTest;
import webapp.App;

@RunWith(SolonJUnit4ClassRunner.class)
@SolonTest(App.class)
public class CacheTest extends HttpTester {
    @Test
    public void test1() throws Exception {
        String rst = path("/cache/").get();

        Thread.sleep(100);

        assert rst.equals(path("/cache/").get());

        Thread.sleep(100);

        assert rst.equals(path("/cache/").get());

        Thread.sleep(100);

        assert rst.equals(path("/cache/").get());
    }

    @Test
    public void test2() throws Exception {
        String rst = path("/cache/").get();

        Thread.sleep(100);

        rst = path("/cache/update").get();
        assert rst.equals(path("/cache/").get());

        Thread.sleep(100);

        rst = path("/cache/update").get();
        assert rst.equals(path("/cache/").get());

        Thread.sleep(100);

        rst = path("/cache/update").get();
        assert rst.equals(path("/cache/").get());
    }

    @Test
    public void test3() throws Exception {
        String rst = path("/cache/").get();

        Thread.sleep(100);

        path("/cache/remove").get();
        assert rst.equals(path("/cache/").get()) == false;

        Thread.sleep(100);

        path("/cache/remove").get();
        assert rst.equals(path("/cache/").get()) == false;

        Thread.sleep(100);

        path("/cache/remove").get();
        assert rst.equals(path("/cache/").get()) == false;
    }

    @Test
    public void test_error() throws Exception {
        int code = path("/cache/error").head();
        System.out.println(code);
        assert code != 200;
        int code2 = path("/cache/error").execAsCode("GET");
        assert code2 == 500;
    }
}
