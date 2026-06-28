package com.hotel.common.security;

import com.hotel.common.core.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class IdCardEncryptService {

    private static final String ID_CARD_REGEX = "^\\d{17}[\\dXx]$";

    @org.springframework.beans.factory.annotation.Value("${security.encryption.master-key:hotel-master-key-change-in-production}")
    private String masterKey;

    private final Map<Integer, SecretKey> keyCache = new ConcurrentHashMap<>();
    private volatile Integer currentVersion = 1;

    public Integer getCurrentVersion() {
        return currentVersion;
    }

    public SecretKey getKey(Integer version) {
        return keyCache.computeIfAbsent(version, this::loadKey);
    }

    public IdCardData encrypt(String rawIdCard) {
        validate(rawIdCard);
        String salt = generateSalt();
        String hash = sha256(rawIdCard, salt);
        Integer kv = getCurrentVersion();
        String encrypted = aesEncrypt(rawIdCard, kv);
        return IdCardData.builder()
                .idCardHash(hash)
                .idCardEncrypted(encrypted)
                .keyVersion(kv)
                .salt(salt)
                .idCardMasked(mask(rawIdCard))
                .birthDate(parseBirthDate(rawIdCard))
                .gender(parseGender(rawIdCard))
                .build();
    }

    public boolean verify(String raw, String hash, String salt) {
        if (raw == null || hash == null || salt == null) return false;
        return secureEquals(sha256(raw, salt), hash);
    }

    public String decrypt(String encrypted, Integer keyVersion) {
        return aesDecrypt(encrypted, getKey(keyVersion));
    }

    public String mask(String id) {
        if (id == null || id.length() < 10) return "***";
        return id.substring(0, 3) + "***" + id.substring(id.length() - 4);
    }

    private void validate(String id) {
        if (id == null || !id.matches(ID_CARD_REGEX)) {
            throw new BusinessException("身份证号格式错误");
        }
    }

    private String generateSalt() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        return Base64.getEncoder().encodeToString(b);
    }

    private String sha256(String text, String salt) {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            byte[] h = d.digest((text + salt).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(h);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String aesEncrypt(String text, Integer kv) {
        try {
            SecretKey key = getKey(kv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] out = c.doFinal(text.getBytes(StandardCharsets.UTF_8));
            byte[] combined = ByteBuffer.allocate(12 + out.length).put(iv).put(out).array();
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("AES 加密失败", e);
        }
    }

    private String aesDecrypt(String data, SecretKey key) {
        try {
            byte[] raw = Base64.getDecoder().decode(data);
            ByteBuffer bb = ByteBuffer.wrap(raw);
            byte[] iv = new byte[12];
            bb.get(iv);
            byte[] ct = new byte[bb.remaining()];
            bb.get(ct);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            return new String(c.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES 解密失败", e);
        }
    }

    private LocalDate parseBirthDate(String id) {
        try {
            return LocalDate.parse(id.substring(6, 14), DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception e) {
            return null;
        }
    }

    private int parseGender(String id) {
        return Character.getNumericValue(id.charAt(16)) % 2 == 1 ? 1 : 2;
    }

    private boolean secureEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }

    private SecretKey loadKey(Integer version) {
        try {
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] kb = f.generateSecret(new PBEKeySpec(
                    masterKey.toCharArray(),
                    ("idcard-key-v" + version).getBytes(),
                    10000, 256)).getEncoded();
            return new SecretKeySpec(kb, "AES");
        } catch (Exception e) {
            throw new RuntimeException("密钥加载失败: v" + version, e);
        }
    }
}
