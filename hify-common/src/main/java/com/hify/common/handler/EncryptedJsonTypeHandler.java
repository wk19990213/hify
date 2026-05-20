package com.hify.common.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.crypto.AesEncryptor;
import com.hify.common.enums.ErrorCode;
import com.hify.common.exception.BizException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 加密 JSON 类型处理器 —— 在 JacksonTypeHandler 基础上叠加 AES-GCM 加密。
 * DB 写入: Object → JSON 字符串 → AES 加密 → 存入 MySQL
 * DB 读取: MySQL → AES 解密 → JSON 字符串 → Object
 */
public class EncryptedJsonTypeHandler extends BaseTypeHandler<Object> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(parameter);
            ps.setString(i, AesEncryptor.encrypt(json));
        } catch (Exception e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Encrypted JSON serialize error", e);
        }
    }

    @Override
    public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private Object parse(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) {
            return null;
        }
        try {
            String json = AesEncryptor.decrypt(encrypted);
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Encrypted JSON parse error", e);
        }
    }
}
