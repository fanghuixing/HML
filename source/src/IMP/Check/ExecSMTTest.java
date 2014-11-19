package IMP.Check;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExecSMTTest {
    private String path = "./source/src/HML_0_0.smt2" ;
    private String args = " --visualize --ode_grid=1024";
    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testExec() throws Exception {
        boolean ret = ExecSMT.exec("0.0001", path + args);



        assertEquals(ret, true);
    }
}