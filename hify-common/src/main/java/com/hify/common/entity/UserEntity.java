package com.hify.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hify_user")
public class UserEntity extends BaseEntity {
    private String username;
    private String passwordHash;
    private String displayName;
    private Integer status;
}
