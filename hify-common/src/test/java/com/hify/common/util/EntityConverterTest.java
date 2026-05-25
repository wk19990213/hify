package com.hify.common.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 实体转换工具测试 - P2-4
 */
class EntityConverterTest {

    @Test
    void testConvert_SingleObject() {
        SourceObj src = new SourceObj();
        src.setId(1L);
        src.setName("test-name");

        TargetObj target = EntityConverter.convert(src, TargetObj.class);

        assertNotNull(target);
        assertEquals(1L, target.getId().longValue());
        assertEquals("test-name", target.getName());
    }

    @Test
    void testConvert_NullSource_ReturnsNull() {
        TargetObj target = EntityConverter.convert(null, TargetObj.class);
        assertNull(target);
    }

    @Test
    void testConvertList() {
        SourceObj src1 = new SourceObj();
        src1.setId(1L);
        src1.setName("first");
        SourceObj src2 = new SourceObj();
        src2.setId(2L);
        src2.setName("second");

        List<TargetObj> targets = EntityConverter.convertList(
                Arrays.asList(src1, src2), TargetObj.class);

        assertNotNull(targets);
        assertEquals(2, targets.size());
        assertEquals("first", targets.get(0).getName());
        assertEquals("second", targets.get(1).getName());
    }

    @Test
    void testConvertList_EmptyList() {
        List<TargetObj> targets = EntityConverter.convertList(
                Collections.emptyList(), TargetObj.class);

        assertNotNull(targets);
        assertTrue(targets.isEmpty());
    }

    @Test
    void testConvertList_NullList_ReturnsEmptyList() {
        List<TargetObj> targets = EntityConverter.convertList(null, TargetObj.class);

        assertNotNull(targets);
        assertTrue(targets.isEmpty());
    }

    @Test
    void testConvert_DifferentPropertyTypes_ConvertsCompatibleTypes() {
        SourceObj src = new SourceObj();
        src.setId(100L);
        src.setName("test");
        src.setCount(42);

        TargetObj target = EntityConverter.convert(src, TargetObj.class);
        assertEquals(42, target.getCount());
    }

    // Test classes
    public static class SourceObj {
        private Long id;
        private String name;
        private int count;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }

    public static class TargetObj {
        private Long id;
        private String name;
        private int count;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }
}
