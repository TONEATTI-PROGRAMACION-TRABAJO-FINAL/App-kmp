package com.toneatti.app.data.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.toneatti.app.platform.AndroidContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * [TokenStorage] de Android: cifra la sesión con una clave AES-256 del
 * **Android Keystore** y guarda solo el texto cifrado en `SharedPreferences` privadas.
 *
 * Por qué así y no `EncryptedSharedPreferences`: la librería `androidx.security:security-crypto`
 * tiene todas sus APIs deprecadas desde la 1.1.0; Google recomienda usar el Keystore directamente.
 *
 * - La clave se genera dentro del Keystore y no puede exportarse: aunque alguien copie
 *   el archivo de preferencias, no puede descifrarlo fuera de este dispositivo.
 * - AES/GCM cifra y autentica: un archivo alterado falla al descifrar en lugar de
 *   devolver datos corruptos.
 * - El archivo está excluido del backup (`res/xml/backup_rules.xml` y
 *   `data_extraction_rules.xml` en `androidApp`), porque en otro dispositivo la clave no existe.
 *
 * @param context `Context` de la aplicación.
 */
class AndroidTokenStorage(context: Context) : TokenStorage {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override suspend fun save(session: UserSession) = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(Json.encodeToString(session).encodeToByteArray())
        // `commit` (síncrono) en lugar de `apply`: ya estamos fuera del hilo principal
        // y así el token queda en disco antes de navegar a la pantalla principal.
        preferences.edit()
            .putString(KEY_IV, encode(cipher.iv))
            .putString(KEY_SESSION, encode(encrypted))
            .commit()
        Unit
    }

    override suspend fun load(): UserSession? = withContext(Dispatchers.IO) {
        val iv = preferences.getString(KEY_IV, null)
        val encrypted = preferences.getString(KEY_SESSION, null)
        if (iv == null || encrypted == null) return@withContext null
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, decode(iv)))
            Json.decodeFromString<UserSession>(cipher.doFinal(decode(encrypted)).decodeToString())
        } catch (e: GeneralSecurityException) {
            // La clave fue invalidada o el archivo no corresponde a ella: no hay sesión utilizable.
            clearPreferences()
            null
        } catch (e: IllegalArgumentException) {
            // Base64 o JSON inválidos (SerializationException es una IllegalArgumentException).
            clearPreferences()
            null
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        clearPreferences()
    }

    private fun clearPreferences() {
        preferences.edit().remove(KEY_IV).remove(KEY_SESSION).commit()
    }

    /** Sincronizado para que dos llamadas concurrentes no generen dos claves distintas. */
    @Synchronized
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build(),
        )
        return keyGenerator.generateKey()
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(text: String): ByteArray = Base64.decode(text, Base64.NO_WRAP)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "user_session_key"
        const val KEY_SIZE_BITS = 256
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128

        // Debe coincidir con el archivo excluido en backup_rules.xml y data_extraction_rules.xml.
        const val PREFERENCES_NAME = "user_session"
        const val KEY_IV = "iv"
        const val KEY_SESSION = "session"
    }
}

/** `actual` de [getTokenStorage] para Android. Requiere `AndroidContext.init` previo. */
actual fun getTokenStorage(): TokenStorage = AndroidTokenStorage(AndroidContext.get())
