package com.vectras.vm.core;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

public class VmJsonParserTest {

    @Test
    public void parseVmListJson_invalidJson_returnsEmptyList() {
        ArrayList<HashMap<String, Object>> parsed = VmJsonParser.parseVmListJson("{", "VmJsonParserTest");

        Assert.assertNotNull(parsed);
        Assert.assertTrue(parsed.isEmpty());
    }

    @Test
    public void isValidVmPosition_whenInsideBounds_returnsTrue() {
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        list.add(new HashMap<>());

        Assert.assertTrue(VmJsonParser.isValidVmPosition(list, 0));
    }
}
