package com.hify.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DemoItem extends BaseEntity {

    private String name;

    private Integer status;
}
