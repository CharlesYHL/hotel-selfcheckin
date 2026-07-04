package com.hotel.common.security;

import com.hotel.common.core.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IdCardEncryptService 单元测试")
class IdCardEncryptServiceTest {

    private IdCardEncryptService service;

    private static final String VALID_ID_CARD = "330102199001011234";
    private static final String VALID_ID_CARD_2 = "110101198512155678";

    @BeforeEach
    void setUp() {
        service = new IdCardEncryptService();
        ReflectionTestUtils.setField(service, "masterKey", "test-master-key-for-encryption-testing!!");
    }

    @Nested
    @DisplayName("加密与解密")
    class EncryptionAndDecryption {

        @Test
        @DisplayName("encrypt 应返回完整的 IdCardData")
        void shouldReturnCompleteIdCardData() {
            IdCardData data = service.encrypt(VALID_ID_CARD);

            assertNotNull(data);
            assertNotNull(data.getIdCardHash());
            assertNotNull(data.getIdCardEncrypted());
            assertNotNull(data.getSalt());
            assertNotNull(data.getIdCardMasked());
            assertNotNull(data.getKeyVersion());
        }

        @Test
        @DisplayName("加密 → 解密往返应得到原文")
        void shouldRoundTripEncryptDecrypt() {
            IdCardData data = service.encrypt(VALID_ID_CARD);
            String decrypted = service.decrypt(data.getIdCardEncrypted(), data.getKeyVersion());

            assertEquals(VALID_ID_CARD, decrypted);
        }

        @Test
        @DisplayName("相同证件号不同盐值应产生不同哈希")
        void shouldProduceDifferentHashWithDifferentSalt() {
            IdCardData data1 = service.encrypt(VALID_ID_CARD);
            IdCardData data2 = service.encrypt(VALID_ID_CARD);

            // 每次加密产生新盐值，哈希应不同
            assertNotEquals(data1.getIdCardHash(), data2.getIdCardHash());
            assertNotEquals(data1.getSalt(), data2.getSalt());
        }

        @Test
        @DisplayName("不同密钥版本加密的数据不能相互解密")
        void shouldFailDecryptWithWrongKeyVersion() {
            IdCardData data = service.encrypt(VALID_ID_CARD);
            // 手动获取不同版本的 key 应导致解密失败
            SecretKey wrongKey = service.getKey(data.getKeyVersion() + 99);

            assertThrows(RuntimeException.class, () ->
                    service.decrypt(data.getIdCardEncrypted(), data.getKeyVersion() + 99));
        }
    }

    @Nested
    @DisplayName("哈希校验")
    class HashVerification {

        @Test
        @DisplayName("verify: 正确明文 + 正确哈希 + 正确盐值 → true")
        void shouldVerifyCorrectCredentials() {
            IdCardData data = service.encrypt(VALID_ID_CARD);

            assertTrue(service.verify(VALID_ID_CARD, data.getIdCardHash(), data.getSalt()));
        }

        @Test
        @DisplayName("verify: 正确明文 + 错误哈希 → false")
        void shouldRejectWrongHash() {
            IdCardData data = service.encrypt(VALID_ID_CARD);

            assertFalse(service.verify(VALID_ID_CARD, "wrong-hash-value", data.getSalt()));
        }

        @Test
        @DisplayName("verify: 错误明文 + 正确哈希 → false")
        void shouldRejectWrongPlaintext() {
            IdCardData data = service.encrypt(VALID_ID_CARD);

            assertFalse(service.verify(VALID_ID_CARD_2, data.getIdCardHash(), data.getSalt()));
        }

        @Test
        @DisplayName("verify: null 参数 → false")
        void shouldHandleNullParameters() {
            IdCardData data = service.encrypt(VALID_ID_CARD);

            assertFalse(service.verify(null, data.getIdCardHash(), data.getSalt()));
            assertFalse(service.verify(VALID_ID_CARD, null, data.getSalt()));
            assertFalse(service.verify(VALID_ID_CARD, data.getIdCardHash(), null));
        }
    }

    @Nested
    @DisplayName("脱敏显示")
    class Masking {

        @Test
        @DisplayName("mask: 标准 18 位身份证 → 330***1234 格式")
        void shouldMaskStandardIdCard() {
            String masked = service.mask(VALID_ID_CARD);
            assertEquals("330***1234", masked);
        }

        @Test
        @DisplayName("mask: 短字符串 → ***")
        void shouldMaskShortString() {
            assertEquals("***", service.mask("123456789"));
        }

        @Test
        @DisplayName("mask: null → ***")
        void shouldMaskNull() {
            assertEquals("***", service.mask(null));
        }
    }

    @Nested
    @DisplayName("身份证号解析")
    class IdCardParsing {

        @Test
        @DisplayName("应正确解析出生日期")
        void shouldParseBirthDate() {
            IdCardData data = service.encrypt(VALID_ID_CARD);

            assertEquals(LocalDate.of(1990, 1, 1), data.getBirthDate());
        }

        @Test
        @DisplayName("应正确解析性别（奇数男，偶数女）")
        void shouldParseGender() {
            // 330102199001011234 → 第17位是3（奇数 → 男）
            IdCardData data1 = service.encrypt(VALID_ID_CARD);
            assertEquals(1, data1.getGender());

            // 110101198512155678 → 第17位是7（奇数 → 男）
            IdCardData data2 = service.encrypt(VALID_ID_CARD_2);
            assertEquals(1, data2.getGender());
        }
    }

    @Nested
    @DisplayName("输入校验")
    class InputValidation {

        @Test
        @DisplayName("非法格式 → BusinessException")
        void shouldRejectInvalidFormat() {
            assertThrows(BusinessException.class, () -> service.encrypt("123456"));
            assertThrows(BusinessException.class, () -> service.encrypt("abcdefghijklmnopqr"));
            assertThrows(BusinessException.class, () -> service.encrypt(""));
        }

        @Test
        @DisplayName("null → BusinessException")
        void shouldRejectNull() {
            assertThrows(BusinessException.class, () -> service.encrypt(null));
        }

        @Test
        @DisplayName("17位 → BusinessException")
        void shouldReject17Digits() {
            assertThrows(BusinessException.class, () -> service.encrypt("33010219900101123"));
        }

        @Test
        @DisplayName("末位 X 应被接受")
        void shouldAcceptXAsLastDigit() {
            IdCardData data = service.encrypt("33010219900101123X");
            assertNotNull(data);
            assertNotNull(data.getIdCardHash());
        }
    }
}
