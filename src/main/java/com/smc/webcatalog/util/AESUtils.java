package com.smc.webcatalog.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author wmayer This class introduces a new security approach and extends {@code de.cadenas.partcommunity.utils.crypt.AESUtil}
 * <p>
 * AES with Galois/Counter Mode (GCM) block mode <b>AES/GCM/NoPadding</b>
 * <ul>
 * <li>Use a 12 byte initialization vector that is never reused with the same key (use a strong pseudorandom number generator like
 * <code>SecureRandom</code>)</li>
 * <li>Use a 128 bit authentication tag length</li>
 * <li>Pack everything together into a single message</li>
 * <li>Integrates authentication tag (at least 128 bit). HMAC not required.</li>
 * </ul>
 * </p>
 */
public class AESUtils {

	public static final byte[] encrypt(final String plainText, final String key) throws GeneralSecurityException {

		final SecretKey secretKey = new SecretKeySpec(key.getBytes(), "AES");

		// Initialization Vector (IV). For GCM a 12 byte (not 16) random (or counter) byte-array is recommended by NIST because it’s faster and more secure.
		SecureRandom secureRandom = new SecureRandom();
		byte[] iv = new byte[12];
		secureRandom.nextBytes(iv);

		final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv); // 128 bit authentication tag length
		cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

		byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

		ByteBuffer byteBuffer = ByteBuffer.allocate(4 + iv.length + cipherText.length);
		byteBuffer.putInt(iv.length);
		byteBuffer.put(iv);
		byteBuffer.put(cipherText);
		byte[] cipherMessage = byteBuffer.array();

		return cipherMessage;
	}

	public static final byte[] decrypt(final byte[] cipherMessage, final String key) throws GeneralSecurityException, IllegalArgumentException {

		final SecretKey secretKey = new SecretKeySpec(key.getBytes(), "AES");

		ByteBuffer byteBuffer = ByteBuffer.wrap(cipherMessage);
		int ivLength = byteBuffer.getInt();
		if (ivLength < 12 || ivLength >= 16) { // check input parameter
			throw new IllegalArgumentException("Invalid length of 'Initialization Vector'");
		}
		byte[] iv = new byte[ivLength];
		byteBuffer.get(iv);
		byte[] cipherText = new byte[byteBuffer.remaining()];
		byteBuffer.get(cipherText);

		final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secretKey.getEncoded(), "AES"), new GCMParameterSpec(128, iv));
		byte[] plainText = cipher.doFinal(cipherText);

		return plainText;
	}
}
