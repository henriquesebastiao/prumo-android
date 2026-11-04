package com.henriquesebastiao.prumo.core.data.session

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Encrypts the session token before it is written to disk. */
interface TokenCipher {
    fun encrypt(plain: String): String

    /** The token, or a [java.security.GeneralSecurityException] when the key can't open it. */
    fun decrypt(cipher: String): String
}

/**
 * AES-256-GCM (Tink) under a key wrapped by the Android Keystore: the token never touches disk in
 * the clear, and a copy of the app's files on another device can't be opened.
 */
@Singleton
class KeystoreTokenCipher @Inject constructor(@ApplicationContext private val context: Context) : TokenCipher {
    private val aead: Aead by lazy {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    override fun encrypt(plain: String): String =
        Base64.encodeToString(aead.encrypt(plain.toByteArray(), ASSOCIATED_DATA), Base64.NO_WRAP)

    override fun decrypt(cipher: String): String =
        String(aead.decrypt(Base64.decode(cipher, Base64.NO_WRAP), ASSOCIATED_DATA))

    private companion object {
        const val KEYSET_NAME = "prumo_token_keyset"
        const val KEYSET_PREFS = "prumo_keysets"
        const val MASTER_KEY_URI = "android-keystore://prumo_token_master_key"
        val ASSOCIATED_DATA = "prumo-session-token".toByteArray()
    }
}
