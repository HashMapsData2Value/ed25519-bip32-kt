/*
 * Copyright (c) Algorand Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.algorandfoundation.bip32ed25519

import cash.z.ecc.android.bip39.Mnemonics.MnemonicCode
import cash.z.ecc.android.bip39.toSeed
import com.algorand.algosdk.crypto.Address
import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.SodiumJava
import com.goterl.lazysodium.utils.Key
import com.goterl.lazysodium.utils.LibraryLoader
import java.util.Base64
import kotlin.collections.component1
import kotlin.test.Test
import kotlin.test.assertNotEquals
import net.pwall.json.schema.JSONSchema
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

/*
 * Helper function to convert a string of comma separated numbers to a byte array (
 * to save space as linter keeps placing each array value on a single row)
 */
fun helperStringToByteArray(input: String): ByteArray {
        return input.split(",")
                        .map { it.trim().toInt() }
                        .toIntArray()
                        .map { it.toByte() }
                        .toByteArray()
}
/*
 * Helper function that converts a hex string into a byte array
 */
fun helperHexStringToByteArray(s: String): ByteArray {
        val result = ByteArray(s.length / 2)
        for (i in 0 until s.length step 2) {
                val byte = s.substring(i, i + 2).toInt(16)
                result[i / 2] = byte.toByte()
        }
        return result
}

// Load the lazy sodium library, desktop version
val ls: LazySodiumJava = LazySodiumJava(SodiumJava(LibraryLoader.Mode.PREFER_BUNDLED))

class Bip32Ed25519Test {

        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        internal class KeyGenTests {

                private lateinit var c: Bip32Ed25519JVM

                @BeforeAll
                fun setup() {
                        val seed =
                                        MnemonicCode(
                                                        "salon zoo engage submit smile frost later decide wing sight chaos renew lizard rely canal coral scene hobby scare step bus leaf tobacco slice".toCharArray()
                                        )
                        c = Bip32Ed25519JVM(seed.toSeed())
                }

                @Test
                fun hardenTest() {
                        assert(Bip32Ed25519Base.harden(0u) == 2147483648u) {
                                "harden(0) and 2147483648 are not equal"
                        }
                        assert(Bip32Ed25519Base.harden(1u) == 2147483649u) {
                                "harden(1) and 2147483648 are not equal"
                        }
                        assert(Bip32Ed25519Base.harden(44u) == 2147483692u) {
                                "harden(44) and 2147483648 are not equal"
                        }
                        assert(Bip32Ed25519Base.harden(283u) == 2147483931u) {
                                "harden(283) and 2147483648 are not equal"
                        }
                }

                @Test
                fun deriveNonHardenedTest() {
                        val kl =
                                        helperStringToByteArray(
                                                        "168,186,128,2,137,34,217,252,250,5,92,120,174,222,85,181,197,117,188,216,213,165,49,104,237,244,95,54,217,236,143,70"
                                        )
                        val cc =
                                        helperStringToByteArray(
                                                        "121,107,146,6,236,48,225,66,233,75,121,10,152,128,91,249,153,4,43,85,4,105,99,23,78,230,206,226,208,55,89,70"
                                        )
                        val index = 0u

                        val deriveNonHardenedExpectedOutcomeZZ =
                                        helperStringToByteArray(
                                                        "79,57,235,234,215,9,72,57,157,32,34,226,81,95,29,115,250,66,232,187,16,193,209,254,140,127,122,242,224,69,122,166,31,223,82,170,49,164,3,115,96,128,159,63,116,37,118,15,167,94,148,38,50,10,126,70,3,86,36,78,199,91,146,54"
                                        )
                        val deriveNonHardenedExpectedOutcomeChildChainCode =
                                        helperStringToByteArray(
                                                        "98,42,235,140,228,232,27,136,136,143,220,220,32,187,77,47,254,209,231,13,224,226,108,113,167,234,93,101,160,32,37,152,216,141,148,178,77,222,78,201,150,148,186,65,223,76,237,113,104,229,170,167,224,222,193,99,251,94,222,14,82,185,232,206"
                                        )

                        val (zProduced, cccProduced) = c.deriveNonHardened(kl, cc, index)

                        assert(zProduced.contentEquals(deriveNonHardenedExpectedOutcomeZZ)) {
                                "zProduced and deriveNonHardenedExpectedOutcomeZZ are not equal"
                        }

                        assert(
                                        cccProduced.contentEquals(
                                                        deriveNonHardenedExpectedOutcomeChildChainCode
                                        )
                        ) {
                                "ccProduced and deriveNonHardenedExpectedOutcomeChainCode are not equal"
                        }
                }

                @Test
                fun derivedHardenedTest() {
                        val kl =
                                        helperStringToByteArray(
                                                        "168,186,128,2,137,34,217,252,250,5,92,120,174,222,85,181,197,117,188,216,213,165,49,104,237,244,95,54,217,236,143,70"
                                        )

                        val kr =
                                        helperStringToByteArray(
                                                        "148,89,43,75,200,146,144,117,131,226,38,105,236,223,27,4,9,169,243,189,85,73,242,221,117,27,81,54,9,9,205,5"
                                        )

                        val cc =
                                        helperStringToByteArray(
                                                        "121,107,146,6,236,48,225,66,233,75,121,10,152,128,91,249,153,4,43,85,4,105,99,23,78,230,206,226,208,55,89,70"
                                        )

                        val index = Bip32Ed25519Base.harden(44u)

                        val deriveHardenedExpectedOutcomeZZ =
                                        helperStringToByteArray(
                                                        "241,155,222,63,177,102,52,174,88,241,56,59,144,16,74,143,9,66,66,43,208,144,253,154,211,54,107,135,59,57,54,101,184,111,121,207,178,74,118,177,0,10,69,137,96,97,246,116,206,37,118,201,90,48,254,232,249,234,191,143,116,13,40,109"
                                        )
                        val deriveHardenedExpectedOutcomeChildChainCode =
                                        helperStringToByteArray(
                                                        "113,159,183,57,127,174,86,11,68,82,114,215,136,191,242,88,45,11,66,160,140,77,60,25,130,238,210,239,247,55,117,240,141,123,149,66,11,250,54,180,175,41,166,195,76,15,154,235,246,49,203,70,79,22,94,165,138,89,21,152,23,108,180,148"
                                        )

                        val (zProduced, cccProduced) = c.deriveHardened(kl, kr, cc, index)

                        assert(zProduced.contentEquals(deriveHardenedExpectedOutcomeZZ)) {
                                "zProduced and deriveHardenedExpectedOutcomeZZ are not equal"
                        }

                        assert(
                                        cccProduced.contentEquals(
                                                        deriveHardenedExpectedOutcomeChildChainCode
                                        )
                        ) {
                                "ccProduced and deriveHardenedExpectedOutcomeChildChainCode are not equal"
                        }
                }

                @Test
                fun keyGenAcc00Test() {
                        val expectedKeyOutput =
                                        helperStringToByteArray(
                                                        "98,254,131,43,122,209,5,68,190,131,55,166,112,67,94,80,100,174,74,102,231,123,215,137,9,118,91,70,181,118,166,243"
                                        )
                        // derive key m'/44'/283'/0'/0/0"
                        val derivedPublicKey = c.keyGen(KeyContext.Address, 0u, 0u, 0u)
                        assert(derivedPublicKey.contentEquals(expectedKeyOutput)) {
                                "derivedPublicKey and expectedKeyOutput are not equal"
                        }
                }

                @Test
                fun keyGenAcc01Test() {
                        val expectedKeyOutput =
                                        helperStringToByteArray(
                                                        "83,4,97,0,46,172,206,192,199,181,121,89,37,170,16,74,127,180,95,133,239,10,169,91,187,91,233,59,111,133,55,173"
                                        )
                        // derive key m'/44'/283'/0'/0/1"
                        val derivedPublicKey = c.keyGen(KeyContext.Address, 0u, 0u, 1u)
                        assert(derivedPublicKey.contentEquals(expectedKeyOutput)) {
                                "derivedPublicKey and expectedKeyOutput are not equal"
                        }
                }

                @Test
                fun keyGenAcc02Test() {
                        val expectedKeyOutput =
                                        helperStringToByteArray(
                                                        "34,129,200,27,238,4,238,3,159,164,130,194,131,84,28,106,176,108,131,36,219,111,28,197,156,104,37,46,29,88,188,179"
                                        )
                        // derive key m'/44'/283'/0'/0/2
                        val derivedPublicKey = c.keyGen(KeyContext.Address, 0u, 0u, 2u)
                        assert(derivedPublicKey.contentEquals(expectedKeyOutput)) {
                                "derivedPublicKey and expectedKeyOutput are not equal"
                        }
                }

                @Test
                fun keyGenAcc10Test() {
                        val expectedKeyOutput =
                                        helperStringToByteArray(
                                                        "158,18,100,63,108,0,104,220,245,59,4,218,206,214,248,193,169,10,210,28,149,74,102,223,65,64,215,147,3,22,106,103"
                                        )
                        // derive key m'/44'/283'/1'/0/1"
                        val derivedPublicKey = c.keyGen(KeyContext.Address, 1u, 0u, 0u)
                        assert(derivedPublicKey.contentEquals(expectedKeyOutput)) {
                                "derivedPublicKey and expectedKeyOutput are not equal"
                        }
                }

                @Test
                fun keyGenAcc11Test() {
                        val expectedKeyOutput =
                                        helperStringToByteArray(
                                                        "25,254,250,164,39,200,166,251,76,248,11,184,72,233,192,195,122,162,191,76,177,156,245,172,149,21,186,30,109,152,140,186"
                                        )
                        // derive key m'/44'/283'/1'/0/1"
                        val derivedPublicKey = c.keyGen(KeyContext.Address, 1u, 0u, 1u)
                        assert(derivedPublicKey.contentEquals(expectedKeyOutput)) {
                                "derivedPublicKey and expectedKeyOutput are not equal"
                        }
                }

                @Test
                fun keyGenAcc21Test() {
                        val expectedKeyOutput =
                                        helperStringToByteArray(
                                                        "138,93,223,98,213,26,44,80,229,29,186,212,99,67,86,204,114,49,74,129,237,217,23,172,145,218,150,71,122,159,181,176"
                                        )
                        // derive key m'/44'/283'/2'/0/1
                        val derivedPublicKey = c.keyGen(KeyContext.Address, 2u, 0u, 1u)
                        assert(derivedPublicKey.contentEquals(expectedKeyOutput)) {
                                "derivedPublicKey and expectedKeyOutput are not equal"
                        }
                }

                @Test
                fun keyGenAcc30Test() {
                        val expectedKeyOutput =
                                        helperStringToByteArray(
                                                        "35,88,224,242,180,101,171,62,143,85,19,157,131,22,101,77,75,227,158,187,34,54,125,54,64,159,208,42,32,176,224,23"
                                        )
                        // derive key m'/44'/283'/3'/0/0"
                        val derivedPublicKey = c.keyGen(KeyContext.Address, 3u, 0u, 0u)
                        assert(derivedPublicKey.contentEquals(expectedKeyOutput)) {
                                "derivedPublicKey and expectedKeyOutput are not equal"
                        }
                }

                @Test
                fun keyGenId00Test() {
                        val expectedKeyOutput =
                                        helperStringToByteArray(
                                                        "182,215,238,165,175,10,216,62,223,67,64,101,158,114,240,234,43,69,102,222,31,195,182,58,64,164,37,170,190,190,94,73"
                                        )
                        // derive key m'/44'/0'/0'/0/0
                        val derivedPublicKey = c.keyGen(KeyContext.Identity, 0u, 0u, 0u)
                        assert(derivedPublicKey.contentEquals(expectedKeyOutput)) {
                                "derivedPublicKey and expectedKeyOutput are not equal"
                        }
                }

                @Test
                fun keyGenId01Test() {
                        val expectedKeyOutput =
                                        helperStringToByteArray(
                                                        "181,206,198,118,197,162,18,158,209,190,66,35,162,112,36,57,187,178,70,47,215,123,67,242,126,47,121,253,25,74,48,162"
                                        )
                        // derive key m'/44'/0'/0'/0/1
                        val derivedPublicKey = c.keyGen(KeyContext.Identity, 0u, 0u, 1u)
                        assert(derivedPublicKey.contentEquals(expectedKeyOutput)) {
                                "derivedPublicKey and expectedKeyOutput are not equal"
                        }
                }

                @Test
                fun keyGenId02Test() {
                        val expectedKeyOutput =
                                        helperStringToByteArray(
                                                        "67,94,94,52,70,67,29,70,37,114,171,238,27,139,173,184,134,8,144,106,106,242,123,132,151,188,207,213,3,237,182,254"
                                        )

                        // derive key m'/44'/0'/0'/0/2
                        val derivedPublicKey = c.keyGen(KeyContext.Identity, 0u, 0u, 2u)
                        assert(derivedPublicKey.contentEquals(expectedKeyOutput)) {
                                "derivedPublicKey and expectedKeyOutput are not equal"
                        }
                }

                @Test
                fun keyGenId10Test() {
                        val expectedKeyOutput =
                                        helperStringToByteArray(
                                                        "191,99,190,131,255,249,188,157,10,235,194,49,213,3,66,17,14,82,32,36,126,80,222,55,107,71,225,84,181,211,42,62"
                                        )
                        // derive key m'/44'/0'/1'/0/0
                        val derivedPublicKey = c.keyGen(KeyContext.Identity, 1u, 0u, 0u)
                        assert(derivedPublicKey.contentEquals(expectedKeyOutput)) {
                                "derivedPublicKey and expectedKeyOutput are not equal"
                        }
                }

                @Test
                fun keyGenId12Test() {
                        val expectedKeyOutput =
                                        helperStringToByteArray(
                                                        "70,149,142,118,219,21,21,127,64,18,39,248,172,189,183,9,36,93,202,5,85,200,232,95,86,176,210,5,46,131,77,6"
                                        )
                        // derive key m'/44'/0'/1'/0/2"
                        val derivedPublicKey = c.keyGen(KeyContext.Identity, 1u, 0u, 2u)
                        assert(derivedPublicKey.contentEquals(expectedKeyOutput)) {
                                "derivedPublicKey and expectedKeyOutput are not equal"
                        }
                }

                @Test
                fun keyGenId21Test() {
                        val expectedKeyOutput =
                                        helperStringToByteArray(
                                                        "237,177,15,255,36,164,116,93,245,47,26,10,177,174,113,179,117,45,1,156,140,36,55,212,106,184,200,230,52,167,76,212"
                                        )
                        // derive key m'/44'/0'/2'/0/1
                        val derivedPublicKey = c.keyGen(KeyContext.Identity, 2u, 0u, 1u)
                        assert(derivedPublicKey.contentEquals(expectedKeyOutput)) {
                                "derivedPublicKey and expectedKeyOutput are not equal"
                        }
                }
        }

        @Test
        fun fromSeedBip39Test() {

                //////////
                val seed =
                                MnemonicCode(
                                                "salon zoo engage submit smile frost later decide wing sight chaos renew lizard rely canal coral scene hobby scare step bus leaf tobacco slice".toCharArray()
                                )

                assert(seed.toSeed().size == 64) { "seed size is not 64" }
                assert(
                                seed.toSeed()
                                                .contentEquals(
                                                                helperStringToByteArray(
                                                                                "58,255,45,180,22,184,149,236,60,249,164,248,209,233,112,188,152,25,146,14,123,244,74,94,53,4,119,175,14,245,87,177,81,27,9,134,222,191,120,221,56,199,197,32,205,68,255,124,114,49,97,143,149,142,33,239,2,80,115,58,140,25,21,234"
                                                                )
                                                )
                ) { "seed mnemonic did not give expected bip39 seed" }

                val rootKey = Bip32Ed25519Base.fromSeed(seed.toSeed())
                val fromSeedExpectedOutput =
                                helperStringToByteArray(
                                                "168,186,128,2,137,34,217,252,250,5,92,120,174,222,85,181,197,117,188,216,213,165,49,104,237,244,95,54,217,236,143,70,148,89,43,75,200,146,144,117,131,226,38,105,236,223,27,4,9,169,243,189,85,73,242,221,117,27,81,54,9,9,205,5,121,107,146,6,236,48,225,66,233,75,121,10,152,128,91,249,153,4,43,85,4,105,99,23,78,230,206,226,208,55,89,70"
                                )

                assert(rootKey.contentEquals(fromSeedExpectedOutput)) {
                        "rootKey and fromSeedExpectedOutput are not equal"
                }

                assert(rootKey.size == 96) { "rootKey size is not 96" }
        }

        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        internal class ValidateDataTests {

                // Inspired by
                // https://github.com/algorandfoundation/ARCs/blob/d44a8e9ecb62152d419f1b4ea50d72baba6b5ba3/assets/arc-0052/contextual.api.crypto.spec.ts#L218
                // But how can that test be valid when it's a random byte? Not a JSON object?
                // TODO: Get this to work

                @Test
                fun validateNonceDataTest() {
                        val challenge =
                                        """
                        {
                                "0": 28, "1": 103, "2": 26, "3": 222, "4": 7, "5": 86, "6": 55, "7": 95, 
                                "8": 197, "9": 179, "10": 249, "11": 252, "12": 232, "13": 252, "14": 176,
                                "15": 39, "16": 112, "17": 131, "18": 52, "19": 63, "20": 212, "21": 58,
                                "22": 226, "23": 89, "24": 64, "25": 94, "26": 23, "27": 91, "28": 128,
                                "29": 143, "30": 123, "31": 27
                        }""".trimIndent()
                        val authSchema =
                                        JSONSchema.parseFile("src/test/resources/auth.request.json")

                        val metadata = SignMetadata(Encoding.NONE, authSchema)
                        val valid = Bip32Ed25519Base.validateData(challenge.toByteArray(), metadata)
                        assert(valid) { "validation failed, message not in line with schema" }
                }

                @Test
                fun validateNonceDataBase64Test() {
                        val challenge =
                                        """
                        {
                                "0": 28, "1": 103, "2": 26, "3": 222, "4": 7, "5": 86, "6": 55, "7": 95, 
                                "8": 197, "9": 179, "10": 249, "11": 252, "12": 232, "13": 252, "14": 176,
                                "15": 39, "16": 112, "17": 131, "18": 52, "19": 63, "20": 212, "21": 58,
                                "22": 226, "23": 89, "24": 64, "25": 94, "26": 23, "27": 91, "28": 128,
                                "29": 143, "30": 123, "31": 27
                        }""".trimIndent()

                        val authSchema =
                                        JSONSchema.parseFile("src/test/resources/auth.request.json")

                        val metadata = SignMetadata(Encoding.BASE64, authSchema)

                        val valid =
                                        Bip32Ed25519Base.validateData(
                                                        Base64.getEncoder()
                                                                        .encode(
                                                                                        challenge.toByteArray()
                                                                        ),
                                                        metadata
                                        )
                        assert(valid) { "validation failed, message not in line with schema" }
                }

                @Test
                fun validateMsgTest() {
                        val message = """{"text":"Hello World"}"""

                        val msgSchema = JSONSchema.parseFile("src/test/resources/msg.schema.json")
                        val metadata = SignMetadata(Encoding.NONE, msgSchema)

                        val valid = Bip32Ed25519Base.validateData(message.toByteArray(), metadata)
                        assert(valid) { "validation failed, message not in line with schema" }
                }

                @Test
                fun validateMsgBase64Test() {
                        val message = """{"text":"Hello World"}"""

                        val msgSchema = JSONSchema.parseFile("src/test/resources/msg.schema.json")
                        val metadata = SignMetadata(Encoding.BASE64, msgSchema)

                        val valid =
                                        Bip32Ed25519Base.validateData(
                                                        Base64.getEncoder()
                                                                        .encode(
                                                                                        message.toByteArray()
                                                                        ),
                                                        metadata
                                        )
                        assert(valid) { "validation failed, message not in line with schema" }
                }

                @Test
                fun validateInvalidNonceDataTest() {
                        // make one value larger than 255, the max according to the schema
                        val challenge =
                                        """
                        {
                                "0": 256, "1": 103, "2": 26, "3": 222, "4": 7, "5": 86, "6": 55, "7": 95, 
                                "8": 197, "9": 179, "10": 249, "11": 252, "12": 232, "13": 252, "14": 176,
                                "15": 39, "16": 112, "17": 131, "18": 52, "19": 63, "20": 212, "21": 58,
                                "22": 226, "23": 89, "24": 64, "25": 94, "26": 23, "27": 91, "28": 128,
                                "29": 143, "30": 123, "31": 27
                        }""".trimIndent()

                        val authSchema =
                                        JSONSchema.parseFile("src/test/resources/auth.request.json")

                        val metadata = SignMetadata(Encoding.NONE, authSchema)

                        val valid = Bip32Ed25519Base.validateData(challenge.toByteArray(), metadata)
                        assert(!valid) {
                                "validation succeeded, despite message not in line with schema"
                        }
                }
                @Test
                fun validateMsgBase64WrongEncodingFailedTest() {
                        // Message is encoded as Base64, but Encoding is set to NONE

                        val message = """{"text":"Hello, World!"}"""
                        val jsonSchema =
                                        """
                        {
                                "type": "object",
                                "properties": {
                                        "text": {
                                                "type": "string"
                                        }
                                },
                                "required": ["text"]
                        }
                        """.trimIndent()

                        val msgSchema = JSONSchema.parse(jsonSchema)

                        val metadata = SignMetadata(Encoding.NONE, msgSchema)

                        val valid =
                                        Bip32Ed25519Base.validateData(
                                                        Base64.getEncoder()
                                                                        .encode(
                                                                                        message.toByteArray()
                                                                        ),
                                                        metadata
                                        )
                        assert(!valid) {
                                "validation succeeded, despite message not in line with schema"
                        }
                }

                @Test
                fun validateMsgWrongMessageFailedTest() {
                        // Schema expects "text" but message has "sentence" field name

                        val message = """{"sentence":"Hello, World!"}"""
                        val jsonSchema =
                                        """
                        {
                                "type": "object",
                                "properties": {
                                        "text": {
                                                "type": "string"
                                        }
                                },
                                "required": ["text"]
                        }
                        """.trimIndent()

                        val msgSchema = JSONSchema.parse(jsonSchema)

                        val metadata = SignMetadata(Encoding.NONE, msgSchema)

                        val valid = Bip32Ed25519Base.validateData(message.toByteArray(), metadata)
                        assert(!valid) {
                                "validation succeeded, despite message not in line with schema"
                        }
                }

                @Test
                fun validateMsgMissingFieldFailedTest() {
                        // Schema requires

                        val message = """{"text":"Hello, World!"}"""
                        val jsonSchema =
                                        """
                        {
                                "type": "object",
                                "properties": {
                                        "text": {
                                                "type": "string"
                                        },
                                        "i": {
                                                "type": "integer"
                                        }
                                },
                                "required": ["i"]
                        }
                        """.trimIndent()

                        val msgSchema = JSONSchema.parse(jsonSchema)

                        val metadata = SignMetadata(Encoding.NONE, msgSchema)

                        val valid = Bip32Ed25519Base.validateData(message.toByteArray(), metadata)
                        assert(!valid) {
                                "validation succeeded, despite message not in line with schema"
                        }
                }

                @Test
                fun validateMsgExtraFieldFailedTest() {
                        // Extra fields in message

                        val message =
                                        """{"text":"Hello World", "i": 10, "extra0": "test", "extra1": "test", "extra2": "test"}"""
                        val jsonSchema =
                                        """
                        {
                                "type": "object",
                                "properties": {
                                        "text": {
                                                "type": "string"
                                        },
                                        "i": {
                                                "type": "integer"
                                        }
                                },
                                "required": ["text", "i"],
                                "additionalProperties": false
                        }
                        """.trimIndent()

                        val msgSchema = JSONSchema.parse(jsonSchema)

                        val metadata = SignMetadata(Encoding.NONE, msgSchema)

                        val valid = Bip32Ed25519Base.validateData(message.toByteArray(), metadata)
                        assert(!valid) {
                                "validation succeeded, despite message not in line with schema"
                        }
                }

                @Test
                fun validateMsgPackTest() {
                        // {"text":"Hello, World!"} --> 81a474657874ad48656c6c6f2c20576f726c6421
                        val msgPackData = "81a474657874ad48656c6c6f2c20576f726c6421"
                        val message = helperHexStringToByteArray(msgPackData)
                        val jsonSchema =
                                        """
                                        {
                                                "type": "object",
                                                "properties": {
                                                        "text": {
                                                                "type": "string"
                                                        }
                                                },
                                                "required": ["text"]
                                        }
                                        """.trimIndent()
                        val msgSchema = JSONSchema.parse(jsonSchema)
                        val metadata = SignMetadata(Encoding.MSGPACK, msgSchema)
                        val valid = Bip32Ed25519Base.validateData(message, metadata)
                        assert(valid) { "validation failed, message not in line with schema" }
                }

                @Test
                fun validateMsg2PackTest() {
                        // {"num": 1} --> 81a36e756d01
                        val msgPackData = "81a36e756d01"
                        val message = helperHexStringToByteArray(msgPackData)
                        val jsonSchema =
                                        """
                                        {
                                                "type": "object",
                                                "properties": {
                                                        "num": {
                                                                "type": "integer"
                                                        }
                                                },
                                                "required": ["num"]
                                        }
                                        """.trimIndent()
                        val msgSchema = JSONSchema.parse(jsonSchema)
                        val metadata = SignMetadata(Encoding.MSGPACK, msgSchema)
                        val valid = Bip32Ed25519Base.validateData(message, metadata)
                        assert(valid) { "validation failed, message not in line with schema" }
                }

                @Test
                fun validateMsgPackFailedTest() {
                        // Incompatible JSON Schema
                        // {"text":"Hello, World!"} --> 81a474657874ad48656c6c6f2c20576f726c6421
                        val msgPackData = "81a474657874ad48656c6c6f2c20576f726c6421"
                        val message = helperHexStringToByteArray(msgPackData)
                        val jsonSchema =
                                        """
                                        {
                                                "type": "object",
                                                "properties": {
                                                        "text": {
                                                                "type": "integer"
                                                        }
                                                },
                                                "required": ["text"]
                                        }
                                        """.trimIndent()
                        val msgSchema = JSONSchema.parse(jsonSchema)
                        val metadata = SignMetadata(Encoding.MSGPACK, msgSchema)
                        val valid = Bip32Ed25519Base.validateData(message, metadata)
                        assert(!valid) {
                                "validation succeeded, despite message not in line with schema"
                        }
                }

                // CBOR is not yet supported across all language implementations
                // @Test
                // fun validateCBORTest() {
                //         // {"text":"Hello, World!"} --> A164746578746D48656C6C6F2C20576F726C6421
                //         val cborData = "A164746578746D48656C6C6F2C20576F726C6421"
                //         val message = helperHexStringToByteArray(cborData)
                //         val jsonSchema =
                //                         """
                //                         {
                //                                 "type": "object",
                //                                 "properties": {
                //                                         "text": {
                //                                                 "type": "string"
                //                                         }
                //                                 },
                //                                 "required": ["text"]
                //                         }
                //                         """.trimIndent()
                //         val msgSchema = JSONSchema.parse(jsonSchema)
                //         val metadata = SignMetadata(Encoding.CBOR, msgSchema)
                //         val valid = Bip32Ed25519Base.validateData(message, metadata)
                //         assert(valid) { "validation failed, message not in line with schema" }
                // }

                // @Test
                // fun validateCBORFailedTest() {
                //         // {"text":"Hello, World!"} --> A164746578746D48656C6C6F2C20576F726C6421
                //         val cborData = "A164746578746D48656C6C6F2C20576F726C6421"
                //         val message = helperHexStringToByteArray(cborData)
                //         val jsonSchema =
                //                         """
                //                         {
                //                                 "type": "object",
                //                                 "properties": {
                //                                         "text": {
                //                                                 "type": "integer"
                //                                         }
                //                                 },
                //                                 "required": ["text"]
                //                         }
                //                         """.trimIndent()
                //         val msgSchema = JSONSchema.parse(jsonSchema)
                //         val metadata = SignMetadata(Encoding.CBOR, msgSchema)
                //         val valid = Bip32Ed25519Base.validateData(message, metadata)
                //         assert(!valid) { "validation failed, message not in line with schema" }
                // }

                @Test
                fun passThroughIllegalPrependFailedTest() {
                        val message = """{"text":"Hello, World!"}""".toByteArray()
                        val jsonSchema =
                                        """
                                        {
                                                "type": "object",
                                                "properties": {
                                                        "text": {
                                                                "type": "string"
                                                        }
                                                },
                                                "required": ["text"]
                                        }
                                        """.trimIndent()
                        val metadata = SignMetadata(Encoding.NONE, JSONSchema.parse(jsonSchema))
                        val valid = Bip32Ed25519Base.validateData(message, metadata)
                        assert(valid) { "validation failed, message not in line with schema" }

                        try {
                                for (prefix in Bip32Ed25519Base.prefixes) {
                                        Bip32Ed25519Base.validateData(
                                                        prefix.toByteArray() + message,
                                                        metadata
                                        )
                                        assert(false) {
                                                "Illegal prepend unexpectedly did not throw error!"
                                        }
                                }
                        } catch (e: DataValidationException) {
                                assert(true) { "Wrong exception was thrown" }
                        }
                }
        }

        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        internal class SignTypedDataTests {
                private lateinit var c: Bip32Ed25519JVM

                @BeforeAll
                fun setup() {
                        val seed =
                                        MnemonicCode(
                                                        "salon zoo engage submit smile frost later decide wing sight chaos renew lizard rely canal coral scene hobby scare step bus leaf tobacco slice".toCharArray()
                                        )
                        c = Bip32Ed25519JVM(seed.toSeed())
                }

                @Test
                fun simpleSignDataTest() {
                        // Message to sign

                        val data = """{"text":"Hello, World!"}""".trimIndent().toByteArray()

                        val pk = c.keyGen(KeyContext.Address, 0u, 0u, 0u)

                        val msgSchema = JSONSchema.parseFile("src/test/resources/msg.schema.json")
                        val metadata = SignMetadata(Encoding.NONE, msgSchema)

                        val signature = c.signData(KeyContext.Address, 0u, 0u, 0u, data, metadata)

                        assert(
                                        signature.contentEquals(
                                                        helperStringToByteArray(
                                                                        "137,13,247,162,115,48,233,188,188,81,7,167,158,250,252,66,138,30,3,65,88,209,92,250,43,13,60,193,44,175,87,93,60,73,243,145,170,38,214,152,29,54,61,109,241,24,238,186,159,45,149,15,141,69,118,162,31,148,162,221,29,156,226,1"
                                                        )
                                        )
                        ) { "Signature different from expected" }

                        assert(signature.size == 64) { "Signature size is not 64" }

                        val isValid = c.verifyWithPublicKey(signature, data, pk)
                        assert(isValid) { "signature is not valid" }

                        val pk2 = c.keyGen(KeyContext.Address, 0u, 0u, 1u)
                        assert(!c.verifyWithPublicKey(signature, data, pk2)) {
                                "signature is unexpectedly valid"
                        }
                }

                @Test
                fun signAuthChallengeTest() {
                        // Randomly generated 32 length byteArray

                        val data =
                                        """
                        {
                                "0": 255, "1": 103, "2": 26, "3": 222, "4": 7, "5": 86, "6": 55, "7": 95, 
                                "8": 197, "9": 179, "10": 249, "11": 252, "12": 232, "13": 252, "14": 176,
                                "15": 39, "16": 112, "17": 131, "18": 52, "19": 63, "20": 212, "21": 58,
                                "22": 226, "23": 89, "24": 64, "25": 94, "26": 23, "27": 91, "28": 128,
                                "29": 143, "30": 123, "31": 27
                        }"""
                                                        .trimIndent()
                                                        .toByteArray()

                        val pk = c.keyGen(KeyContext.Address, 0u, 0u, 0u)

                        val authSchema =
                                        JSONSchema.parseFile("src/test/resources/auth.request.json")
                        val metadata = SignMetadata(Encoding.NONE, authSchema)

                        val signature = c.signData(KeyContext.Address, 0u, 0u, 0u, data, metadata)

                        val isValid = c.verifyWithPublicKey(signature, data, pk)
                        assert(isValid) { "signature is not valid" }

                        val pk2 = c.keyGen(KeyContext.Address, 0u, 0u, 1u)
                        assert(!c.verifyWithPublicKey(signature, data, pk2)) {
                                "signature is unexpectedly valid"
                        }
                }

                @Test
                fun signAuthChallenge64Test() {
                        // Randomly generated 32 length byteArray

                        val dataRaw =
                                        """
                        {
                                "0": 255, "1": 103, "2": 26, "3": 222, "4": 7, "5": 86, "6": 55, "7": 95, 
                                "8": 197, "9": 179, "10": 249, "11": 252, "12": 232, "13": 252, "14": 176,
                                "15": 39, "16": 112, "17": 131, "18": 52, "19": 63, "20": 212, "21": 58,
                                "22": 226, "23": 89, "24": 64, "25": 94, "26": 23, "27": 91, "28": 128,
                                "29": 143, "30": 123, "31": 27
                        }"""
                                                        .trimIndent()
                                                        .toByteArray()

                        val data = Base64.getEncoder().encode(dataRaw)
                        val pk = c.keyGen(KeyContext.Address, 0u, 0u, 0u)

                        val authSchema =
                                        JSONSchema.parseFile("src/test/resources/auth.request.json")
                        val metadata = SignMetadata(Encoding.BASE64, authSchema)

                        val signature = c.signData(KeyContext.Address, 0u, 0u, 0u, data, metadata)

                        val isValid = c.verifyWithPublicKey(signature, data, pk)
                        assert(isValid) { "signature is not valid" }

                        val pk2 = c.keyGen(KeyContext.Address, 0u, 0u, 1u)
                        assert(!c.verifyWithPublicKey(signature, data, pk2)) {
                                "signature is unexpectedly valid"
                        }
                }

                @Test
                fun signAuthChallenge64FailedTest() {
                        // Encoding set to none despite message being Base64 encoded

                        val dataRaw =
                                        """
                        {
                                "0": 255, "1": 103, "2": 26, "3": 222, "4": 7, "5": 86, "6": 55, "7": 95, 
                                "8": 197, "9": 179, "10": 249, "11": 252, "12": 232, "13": 252, "14": 176,
                                "15": 39, "16": 112, "17": 131, "18": 52, "19": 63, "20": 212, "21": 58,
                                "22": 226, "23": 89, "24": 64, "25": 94, "26": 23, "27": 91, "28": 128,
                                "29": 143, "30": 123, "31": 27
                        }"""
                                                        .trimIndent()
                                                        .toByteArray()

                        val data = Base64.getEncoder().encode(dataRaw)

                        val authSchema =
                                        JSONSchema.parseFile("src/test/resources/auth.request.json")

                        val metadata = SignMetadata(Encoding.NONE, authSchema)

                        try {
                                c.signData(KeyContext.Address, 0u, 0u, 0u, data, metadata)
                                // If we get past this line, the test failed
                                throw (IllegalArgumentException(
                                                "signData func did not throw DataValidationExcept despite wrong encoding"
                                ))
                        } catch (e: Exception) {
                                assert(e is DataValidationException) {
                                        "signData did not throw an DataValidationException"
                                }
                        }
                }

                @Test
                fun signAuthChallengeMsgPackTest() {
                        // Corresponds to {"0": 255, "1": 1032, ..., "31": 27}
                        val msgPackData =
                                        "de0020a130ccffa13167a1321aa133ccdea13407a13556a13637a1375fa138ccc5a139ccb3a23130ccf9a23131ccfca23132cce8a23133ccfca23134ccb0a2313527a2313670a23137cc83a2313834a231393fa23230ccd4a232313aa23232cce2a2323359a2323440a232355ea2323617a232375ba23238cc80a23239cc8fa233307ba233311b"

                        val data = helperHexStringToByteArray(msgPackData)
                        val pk = c.keyGen(KeyContext.Address, 0u, 0u, 0u)

                        val authSchema =
                                        JSONSchema.parseFile("src/test/resources/auth.request.json")
                        val metadata = SignMetadata(Encoding.MSGPACK, authSchema)

                        val signature = c.signData(KeyContext.Address, 0u, 0u, 0u, data, metadata)

                        val isValid = c.verifyWithPublicKey(signature, data, pk)
                        assert(isValid) { "signature is not valid" }

                        val pk2 = c.keyGen(KeyContext.Address, 0u, 0u, 1u)
                        assert(!c.verifyWithPublicKey(signature, data, pk2)) {
                                "signature is unexpectedly valid"
                        }
                }

                @Test
                fun signAuthChallengeMsgPackFailedTest() {
                        // Corresponds to {"0": 256, "1": 1032, ..., "31": 27} - 256 is too large

                        val msgPackData =
                                        "de0020a130cd0100a13167a1321aa133ccdea13407a13556a13637a1375fa138ccc5a139ccb3a23130ccf9a23131ccfca23132cce8a23133ccfca23134ccb0a2313527a2313670a23137cc83a2313834a231393fa23230ccd4a232313aa23232cce2a2323359a2323440a232355ea2323617a232375ba23238cc80a23239cc8fa233307ba233311b"

                        val data = helperHexStringToByteArray(msgPackData)

                        val authSchema =
                                        JSONSchema.parseFile("src/test/resources/auth.request.json")
                        val metadata = SignMetadata(Encoding.MSGPACK, authSchema)

                        try {
                                c.signData(KeyContext.Address, 0u, 0u, 0u, data, metadata)
                                // If we get past this line, the test failed
                                throw (IllegalArgumentException(
                                                "signData func did not throw DataValidationExcept despite bad message"
                                ))
                        } catch (e: Exception) {
                                assert(e is DataValidationException) {
                                        "signData did not throw an DataValidationException"
                                }
                        }
                }

                // CBOR is not yet supported across all language implementations.
                // @Test
                // fun signAuthChallengeCBORTest() {
                //         // Corresponds to {"0": 255, "1": 1032, ..., "31": 27}
                //         val CBORData =
                //
                // "B820613018FF613118676132181A613318DE61340761351856613618376137185F613818C5613918B362313018F962313118FC62313218E862313318FC62313418B06231351827623136187062313718836231381834623139183F62323018D4623231183A62323218E262323318596232341840623235185E62323617623237185B6232381880623239188F623330187B623331181B"

                //         val data = helperHexStringToByteArray(CBORData)
                //         val pk = c.keyGen(KeyContext.Address, 0u, 0u, 0u)

                //         val authSchema =
                //
                // JSONSchema.parseFile("src/test/resources/auth.request.json")
                //         val metadata = SignMetadata(Encoding.CBOR, authSchema)

                //         val signature = c.signData(KeyContext.Address, 0u, 0u, 0u, data,
                // metadata)

                //         val isValid = c.verifyWithPublicKey(signature, data, pk)
                //         assert(isValid) { "signature is not valid" }

                //         val pk2 = c.keyGen(KeyContext.Address, 0u, 0u, 1u)
                //         assert(!c.verifyWithPublicKey(signature, data, pk2)) {
                //                 "signature is unexpectedly valid"
                //         }
                // }

                // @Test
                // fun signAuthChallengeCBORFailedTest() {
                //         // Corresponds to {"0": 256, "1": 1032, ..., "31": 27} - 256 is too large

                //         val CBORData =
                //
                // "B8206130190100613118676132181A613318DE61340761351856613618376137185F613818C5613918B362313018F962313118FC62313218E862313318FC62313418B06231351827623136187062313718836231381834623139183F62323018D4623231183A62323218E262323318596232341840623235185E62323617623237185B6232381880623239188F623330187B623331181B"

                //         val data = helperHexStringToByteArray(CBORData)

                //         val authSchema =
                //
                // JSONSchema.parseFile("src/test/resources/auth.request.json")
                //         val metadata = SignMetadata(Encoding.CBOR, authSchema)

                //         try {
                //                 c.signData(KeyContext.Address, 0u, 0u, 0u, data, metadata)
                //                 // If we get past this line, the test failed
                //                 throw (IllegalArgumentException(
                //                                 "signData func did not throw DataValidationExcept
                // despite bad message"
                //                 ))
                //         } catch (e: Exception) {
                //                 assert(e is DataValidationException) {
                //                         "signData did not throw an DataValidationException"
                //                 }
                //         }
                // }

                @Test
                fun signIllegalPrependMsgFailedTest() {
                        val message = """{"text":"Hello, World!"}""".toByteArray()
                        val jsonSchema =
                                        """
                                        {
                                                "type": "object",
                                                "properties": {
                                                        "text": {
                                                                "type": "string"
                                                        }
                                                },
                                                "required": ["text"]
                                        }
                                        """.trimIndent()
                        val metadata = SignMetadata(Encoding.NONE, JSONSchema.parse(jsonSchema))

                        val pk = c.keyGen(KeyContext.Address, 0u, 0u, 0u)

                        val signature =
                                        c.signData(
                                                        KeyContext.Address,
                                                        0u,
                                                        0u,
                                                        0u,
                                                        message,
                                                        metadata
                                        )

                        val isValid = c.verifyWithPublicKey(signature, message, pk)
                        assert(isValid) { "signature is not valid" }

                        val pk2 = c.keyGen(KeyContext.Address, 0u, 0u, 1u)
                        assert(!c.verifyWithPublicKey(signature, message, pk2)) {
                                "signature is unexpectedly valid"
                        }

                        try {
                                for (prefix in Bip32Ed25519Base.prefixes) {
                                        c.signData(
                                                        KeyContext.Address,
                                                        0u,
                                                        0u,
                                                        0u,
                                                        prefix.toByteArray() + message,
                                                        metadata
                                        )
                                        assert(false) {
                                                "Illegal prepend unexpectedly did not throw error!"
                                        }
                                }
                        } catch (e: DataValidationException) {
                                assert(true) { "Wrong exception was thrown" }
                        }
                }

                @Test
                fun verifyAlgorandTx() {
                        val pk = c.keyGen(KeyContext.Address, 0u, 0u, 0u)
                        // this transaction wes successfully submitted to the network
                        // https://testnet.explorer.perawallet.app/tx/UJG3NVCSCW5A63KPV35BPAABLXMXTTEM2CVUKNS4EML3H3EYGMCQ/
                        // in accordance with the Typescript implementation
                        val prefixEncodedTx =
                                        Base64.getDecoder()
                                                        .decode(
                                                                        "VFiJo2FtdM0D6KNmZWXNA+iiZnbOAkeSd6NnZW6sdGVzdG5ldC12MS4womdoxCBIY7UYpLPITsgQ8i1PEIHLD3HwWaesIN7GL39w5Qk6IqJsds4CR5Zfo3JjdsQgYv6DK3rRBUS+gzemcENeUGSuSmbne9eJCXZbRrV2pvOjc25kxCBi/oMretEFRL6DN6ZwQ15QZK5KZud714kJdltGtXam86R0eXBlo3BheQ=="
                                                        )
                        val sig =
                                        c.signAlgoTransaction(
                                                        KeyContext.Address,
                                                        0u,
                                                        0u,
                                                        0u,
                                                        prefixEncodedTx
                                        )

                        assert(
                                        encodeAddress(pk)
                                                        .equals(
                                                                        "ML7IGK322ECUJPUDG6THAQ26KBSK4STG4555PCIJOZNUNNLWU3Z3ZFXITA"
                                                        )
                        )
                        assert(c.verifyWithPublicKey(sig, prefixEncodedTx, pk))
                }
        }

        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        internal class ECDHTests {

                private lateinit var alice: Bip32Ed25519JVM
                private lateinit var bob: Bip32Ed25519JVM

                @BeforeAll
                fun setup() {
                        val aliceSeed =
                                        MnemonicCode(
                                                                        "exact remain north lesson program series excess lava material second riot error boss planet brick rotate scrap army riot banner adult fashion casino bamboo".toCharArray()
                                                        )
                                                        .toSeed()

                        val bobSeed =
                                        MnemonicCode(
                                                                        "identify length ranch make silver fog much puzzle borrow relax occur drum blue oval book pledge reunion coral grace lamp recall fever route carbon".toCharArray()
                                                        )
                                                        .toSeed()
                        alice = Bip32Ed25519JVM(aliceSeed)
                        bob = Bip32Ed25519JVM(bobSeed)
                }

                @Test
                fun basicECDHTest() {
                        val aliceKey = alice.keyGen(KeyContext.Identity, 0u, 0u, 0u)
                        val bobKey = bob.keyGen(KeyContext.Identity, 0u, 0u, 0u)

                        val aliceSharedSecret =
                                        alice.ECDH(KeyContext.Identity, 0u, 0u, 0u, bobKey, true)
                        val bobSharedSecret =
                                        bob.ECDH(KeyContext.Identity, 0u, 0u, 0u, aliceKey, false)

                        assertNotEquals(
                                        aliceKey,
                                        bobKey,
                                        "alice's key and bob's key are unexpectedly equal"
                        )

                        assert(aliceSharedSecret.contentEquals(bobSharedSecret)) {
                                "aliceSharedSecret and bobSharedSecret are not equal"
                        }

                        assert(
                                        aliceSharedSecret.contentEquals(
                                                        helperStringToByteArray(
                                                                        "202,114,20,173,185,153,18,48,253,145,160,157,145,158,198,130,178,172,151,129,183,110,32,107,75,135,244,221,110,246,66,127"
                                                        )
                                        )
                        ) { "produced shared secret does not correspond to hardcoded secret" }

                        // Now we reverse pubkey order in concatenation
                        val aliceSharedSecret2 =
                                        alice.ECDH(KeyContext.Identity, 0u, 0u, 0u, bobKey, false)
                        alice.ECDH(KeyContext.Identity, 0u, 0u, 0u, bobKey, false)
                        val bobSharedSecret2 =
                                        bob.ECDH(KeyContext.Identity, 0u, 0u, 0u, aliceKey, true)
                        bob.ECDH(KeyContext.Identity, 0u, 0u, 0u, aliceKey, true)

                        assertNotEquals(
                                        aliceSharedSecret,
                                        aliceSharedSecret2,
                                        "despite different concat orders shared secrets are equal"
                        )
                        assertNotEquals(
                                        bobSharedSecret,
                                        bobSharedSecret2,
                                        "despite different concat orders shared secrets are equal"
                        )

                        assert(aliceSharedSecret2.contentEquals(bobSharedSecret2)) {
                                "aliceSharedSecret and bobSharedSecret are not equal"
                        }

                        assert(
                                        aliceSharedSecret2.contentEquals(
                                                        helperStringToByteArray(
                                                                        "90,215,114,148,204,139,215,147,233,41,219,196,163,237,229,68,134,255,92,129,181,253,137,142,191,244,101,46,252,253,250,26"
                                                        )
                                        )
                        ) {
                                "produced second shared secret does not correspond to hardcoded secret"
                        }
                }

                @Test
                fun encryptDecryptECDHTest() {
                        val aliceKey = alice.keyGen(KeyContext.Identity, 0u, 0u, 0u)
                        val bobKey = bob.keyGen(KeyContext.Identity, 0u, 0u, 0u)

                        val aliceSharedSecret =
                                        Key.fromBytes(
                                                        alice.ECDH(
                                                                        KeyContext.Identity,
                                                                        0u,
                                                                        0u,
                                                                        0u,
                                                                        bobKey,
                                                                        true
                                                        )
                                        )
                        val bobSharedSecret =
                                        Key.fromBytes(
                                                        bob.ECDH(
                                                                        KeyContext.Identity,
                                                                        0u,
                                                                        0u,
                                                                        0u,
                                                                        aliceKey,
                                                                        false
                                                        )
                                        )

                        assert(aliceSharedSecret.asBytes.contentEquals(bobSharedSecret.asBytes)) {
                                "aliceSharedSecret and bobSharedSecret are equal"
                        }

                        val message = "Hello, World!"
                        val nonce =
                                        helperStringToByteArray(
                                                        "16,197,142,8,174,91,118,244,202,136,43,200,97,242,104,99,42,154,191,32,67,30,6,123"
                                        )

                        // Encrypt
                        val ciphertext = ls.cryptoSecretBoxEasy(message, nonce, aliceSharedSecret)

                        assert(
                                        ciphertext.equals(
                                                        "FB07303A391687989674F28A1A9B88FCA3D107227D87DADE662DFA3722"
                                        ),
                        ) {
                                "produced ciphertext is not what was expected given hardcoded keys, nonce and 'Hello, World!' message"
                        }

                        // The hex string above is the same as the bytearray below.
                        // This conversion is done for readability/visibility's sake,
                        // when comparing with other language implementations

                        assert(
                                        helperHexStringToByteArray(ciphertext)
                                                        .contentEquals(
                                                                        helperStringToByteArray(
                                                                                        "251,7,48,58,57,22,135,152,150,116,242,138,26,155,136,252,163,209,7,34,125,135,218,222,102,45,250,55,34"
                                                                        )
                                                        )
                        ) {
                                "produced ciphertext bytes is that what was expected given hardcoded ciphertext bytes"
                        }

                        assert(
                                        ciphertext.equals(
                                                        "FB07303A391687989674F28A1A9B88FCA3D107227D87DADE662DFA3722"
                                        ),
                        ) {
                                "produced ciphertext is not what was expected given hardcoded keys, nonce and 'Hello, World!' message"
                        }

                        // The hex string above is the same as the bytearray below.
                        // This conversion is done for readability/visibility's sake,
                        // when comparing with other language implementations

                        assert(
                                        helperHexStringToByteArray(ciphertext)
                                                        .contentEquals(
                                                                        helperStringToByteArray(
                                                                                        "251,7,48,58,57,22,135,152,150,116,242,138,26,155,136,252,163,209,7,34,125,135,218,222,102,45,250,55,34"
                                                                        )
                                                        )
                        ) {
                                "produced ciphertext bytes is that what was expected given hardcoded ciphertext bytes"
                        }

                        // Decrypt
                        val plaintext =
                                        ls.cryptoSecretBoxOpenEasy(
                                                        ciphertext,
                                                        nonce,
                                                        aliceSharedSecret
                                        )

                        assert(message.contentEquals(plaintext)) {
                                "message and decrypted plaintext are not equal"
                        }
                }
        }
}
