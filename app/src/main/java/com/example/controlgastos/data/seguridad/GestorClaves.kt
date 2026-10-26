package com.example.controlgastos.data.seguridad

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda secretos de forma segura con el Android Keystore.
 *
 * El Keystore genera y guarda una "clave maestra" AES dentro del hardware
 * seguro del teléfono (TEE o StrongBox): la app puede USARLA para cifrar y
 * descifrar, pero nadie (ni la propia app, ni un atacante con root) puede
 * EXTRAERLA. Con esa clave maestra se cifran otros secretos (aquí, la clave
 * de la base de datos) que sí se guardan en un archivo, ya cifrados.
 *
 * Nota: la librería EncryptedSharedPreferences, que hacía esto mismo, está
 * deprecada desde 2025; usar el Keystore directamente es la alternativa
 * recomendada.
 */
@Singleton
class GestorClaves @Inject constructor(@ApplicationContext context: Context) {

    private val preferencias = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    /**
     * Clave de 256 bits para SQLCipher. Se genera al azar la primera vez; luego
     * se recupera descifrándola con la clave maestra del Keystore.
     */
    @Synchronized
    fun claveBaseDeDatos(): ByteArray {
        preferencias.getString(CLAVE_BD, null)?.let { return descifrar(it) }

        val nueva = ByteArray(32).also { SecureRandom().nextBytes(it) }
        // commit() (síncrono): si se perdiera, la BD quedaría ilegible para siempre.
        preferencias.edit(commit = true) { putString(CLAVE_BD, cifrar(nueva)) }
        return nueva
    }

    fun cifrar(datos: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(Cipher.ENCRYPT_MODE, claveMaestra())
        // AES-GCM: el IV (aleatorio, lo genera el Keystore) se guarda junto al
        // texto cifrado; no es secreto, pero es necesario para descifrar.
        val resultado = cipher.iv + cipher.doFinal(datos)
        return Base64.encodeToString(resultado, Base64.NO_WRAP)
    }

    fun descifrar(textoCifrado: String): ByteArray {
        val bytes = Base64.decode(textoCifrado, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, TAMANIO_IV)
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(Cipher.DECRYPT_MODE, claveMaestra(), GCMParameterSpec(BITS_TAG, iv))
        return cipher.doFinal(bytes, TAMANIO_IV, bytes.size - TAMANIO_IV)
    }

    private fun claveMaestra(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }

        val especificacion = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
            .apply { init(especificacion) }
            .generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "clave_maestra_gastos"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val TAMANIO_IV = 12
        const val BITS_TAG = 128
        const val ARCHIVO = "claves_cifradas"
        const val CLAVE_BD = "clave_bd"
    }
}
