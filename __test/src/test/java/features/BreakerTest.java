package features;

import lombok.extern.slf4j.Slf4j;
import org.noear.solon.Solon;
import org.noear.solon.test.HttpTester;
import webapp.demox_log_breaker.BreakerServiceDemo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

/**
 * @author noear 2021/3/14 created
 */
@Slf4j
//@RunWith(SolonJUnit4ClassRunner.class)
//@SolonTest(webapp.TestApp.class)
public class BreakerTest extends HttpTester {

//    @Test
    public void test() throws Exception {
        CountDownLatch downLatch = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    path("/demox/test").get();

                } catch (Exception ex) {

                } finally {
                    downLatch.countDown();
                }
            });
        }

        downLatch.await();
    }

//    @Test
    public void test2() throws Exception{
        BreakerServiceDemo serviceDemo = Solon.context().getBeanOrNew(BreakerServiceDemo.class);
        CountDownLatch downLatch = new CountDownLatch(105);

        for (int i = 0; i < 105; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    serviceDemo.test();
                    log.debug("ok");
                } catch (Exception ex) {
                    log.error("{}", ex);
                }finally {
                    downLatch.countDown();
                }
            });
        }

        downLatch.await();
    }
}
