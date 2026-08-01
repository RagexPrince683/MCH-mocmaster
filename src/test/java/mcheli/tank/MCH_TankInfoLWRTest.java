package mcheli.tank;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MCH_TankInfoLWRTest {

    @Test
    public void lwrDefaultsToFalseAndParsesTrueCaseInsensitively() {
        MCH_TankInfo info = new MCH_TankInfo("lwr_test");

        assertFalse(info.LWR);
        info.loadItemData("lWr", "TrUe");
        assertTrue(info.LWR);
    }

    @Test
    public void falseAndInvalidValuesDisableLwr() {
        MCH_TankInfo info = new MCH_TankInfo("lwr_test");

        info.loadItemData("LWR", "true");
        info.loadItemData("LWR", "false");
        assertFalse(info.LWR);

        info.loadItemData("LWR", "invalid");
        assertFalse(info.LWR);
    }
}
