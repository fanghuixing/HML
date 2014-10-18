package IMP.Check;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ExecSMTTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testExec() throws Exception {
        boolean ret = ExecSMT.exec("0.0001", "./source/src/HML_20_0.smt2 --visualize");
        assertEquals(ret, true);
    }
}