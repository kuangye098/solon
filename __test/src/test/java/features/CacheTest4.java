package features;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.noear.solon.test.HttpTester;
import org.noear.solon.test.SolonJUnit4ClassRunner;
import org.noear.solon.test.SolonTest;
import webapp.App;

@RunWith(SolonJUnit4ClassRunner.class)
@SolonTest(App.class)
public class CacheTest4 extends HttpTester {

    @Test
    public void test4() throws Exception {
        String rst = path("/cache4/cache").get();

        Thread.sleep(100);
        assert rst.equals(path("/cache4/cache").get());

        path("/cache4/remove").data("id", "12").post();


        assert rst.equals(path("/cache4/cache").get()) == false;
    }
}
