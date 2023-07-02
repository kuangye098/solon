package features;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.noear.nami.Nami;
import org.noear.nami.annotation.NamiClient;
import org.noear.nami.coder.snack3.SnackEncoder;
import org.noear.solon.test.SolonJUnit4ClassRunner;
import org.noear.solon.test.SolonTest;
import webapp.App;
import webapp.nami.ComplexModel;
import webapp.nami.ComplexModelService1;
import webapp.nami.ComplexModelService2;
import webapp.nami.ComplexModelService3;

/**
 * @author noear 2022/12/6 created
 */
@RunWith(SolonJUnit4ClassRunner.class)
@SolonTest(App.class)
public class NamiTest {
    @NamiClient
    ComplexModelService1 complexModelService1;

    @NamiClient
    ComplexModelService2 complexModelService2;

    @NamiClient
    ComplexModelService3 complexModelService3;

    @Test
    public void test1() {
        ComplexModel model = new ComplexModel();
        model.setModelId(11);
        complexModelService1.save(model);

        assert complexModelService1.read(12).getModelId() == 12;
    }

    @Test
    public void test1_2() {
        ComplexModelService1 service1_2 = Nami.builder()
                .name("local")
                .path("/nami/ComplexModelService1/")
                .encoder(SnackEncoder.instance)
                .create(ComplexModelService1.class);

        assert service1_2.read(12).getModelId() == 12;
    }

    @Test
    public void test2() {
        ComplexModel model = new ComplexModel();
        model.setModelId(21);
        complexModelService2.save(model);

        assert complexModelService2.read(22).getModelId() == 22;
    }

    @Test
    public void test3() {
        ComplexModel model = new ComplexModel();
        model.setModelId(31);
        complexModelService3.save(model);

        assert complexModelService3.read(32).getModelId() == 32;
    }
}
