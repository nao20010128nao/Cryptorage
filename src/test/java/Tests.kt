import com.google.common.io.*
import com.nao20010128nao.Cryptorage.*
import com.nao20010128nao.Cryptorage.cryptorage.*
import com.nao20010128nao.Cryptorage.file.*
import com.nao20010128nao.Cryptorage.internal.*
import junit.framework.*

import java.io.*
import java.security.*
import java.util.*

class Tests : TestCase() {
    fun testSimpleWriting() {
        val cryptorage = newMemoryFileSource().withV1Encryption("test")
        val sr = SecureRandom()
        val test = ByteArray(1024 * 1024)
        sr.nextBytes(test)
        val dest = cryptorage.put("test").openBufferedStream()
        dest.write(test)
        dest.close()
        val md = MessageDigest.getInstance("sha-256")
        val hashed = md.digest(test)

        val `is` = cryptorage.open("test", 0).openBufferedStream()
        val read = ByteStreams.toByteArray(`is`)
        `is`.close()

        assertEquals(test, read)
        assertEquals(hashed, md.digest(read))
    }

    fun testSimpleWriting2() {
        val cryptorage = newMemoryFileSource().withV1Encryption("test")
        val payload = "It's a small world"

        val test = payload.toByteArray()
        val dest = cryptorage.put("test").openBufferedStream()
        for (i in 0..99999)
            dest.write(test)
        dest.close()

        val `is` = cryptorage.open("test", 0).openBufferedStream()
        for (i in 0..99999) {
            var error: Throwable? = null
            try {
                Arrays.fill(test, 0.toByte())
                ByteStreams.readFully(`is`, test)
            } catch (e: Throwable) {
                error = e
            }

            println(i.toString() + ": " + String(test))
            if (error != null) {
                throw error
            }
            assertEquals(test, payload.toByteArray())
        }
        `is`.close()
    }

    fun testWriteSize() {
        val cryptorage = newMemoryFileSource().withV1Encryption("test")
        val payload = "It's a small world"

        val test = payload.toByteArray()
        val dest = cryptorage.put("test").openBufferedStream()
        for (i in 0..9999)
            dest.write(test)
        dest.close()

        TestCase.assertEquals((payload.length * 10000).toLong(), cryptorage.size("test"))
    }

    fun testOverflow() {
        val payload = "It's a small world"
        val first10 = "It's a sma"
        val remain8 = "ll world"
        val stream = SizeLimitedOutputStream(10, { a, b ->
            TestCase.assertTrue(Arrays.equals(a.buffer, first10.toByteArray()))
            TestCase.assertTrue(Arrays.equals(b.buffer, remain8.toByteArray()))
        },null)
        stream.write(payload.toByteArray())
    }

    fun testOverWrite() {
        val cryptorage = newMemoryFileSource().withV1Encryption("test")
        val sr = SecureRandom()
        val test = ByteArray(1024 * 1024)
        sr.nextBytes(test)
        var dest = cryptorage.put("test").openBufferedStream()
        dest.write(test)
        dest.close()
        sr.nextBytes(test)
        dest = cryptorage.put("test").openBufferedStream()
        dest.write(test)
        dest.close()
        val md = MessageDigest.getInstance("sha-256")
        val hashed = md.digest(test)

        val `is` = cryptorage.open("test", 0).openBufferedStream()
        val read = ByteStreams.toByteArray(`is`)
        `is`.close()

        assertEquals(test, read)
        assertEquals(hashed, md.digest(read))
    }

    fun testSkip() {
        val cryptorage = newMemoryFileSource().withV1Encryption("test")
        val sr = SecureRandom()
        val test = ByteArray(1024 * 1024)
        sr.nextBytes(test)
        val dest = cryptorage.put("test").openBufferedStream()
        dest.write(test)
        dest.close()
        val md = MessageDigest.getInstance("sha-256")
        md.update(test, 1000000, test.size - 1000000)
        val hashed = md.digest()

        val `is` = cryptorage.open("test", 1000000).openBufferedStream()
        val read = ByteStreams.toByteArray(`is`)
        `is`.close()

        assertEquals(hashed, md.digest(read))
    }

    fun testReopen() {
        val memory = newMemoryFileSource()
        val cryptorage = memory.withV1Encryption("test")
        val payload = "It's a small world"

        val test = payload.toByteArray()
        var dest = cryptorage.put("file1").openBufferedStream()
        dest.write(test)
        dest.close()
        dest = cryptorage.put("file2").openBufferedStream()
        dest.write(test)
        dest.write(test)
        dest.close()
        TestCase.assertTrue(cryptorage.list().contains("file1"))
        TestCase.assertTrue(cryptorage.list().contains("file2"))

        val cryptorageReopen = memory.withV1Encryption("test")
        TestCase.assertTrue(cryptorageReopen.list().contains("file1"))
        TestCase.assertTrue(cryptorageReopen.list().contains("file2"))
    }

    fun assertEquals(a:ByteArray,b:ByteArray):Nothing?{
        val min= minOf(a.size,b.size)
        (0 until min)
                .filter { a[it]!=b[it] }
                .forEach { throw AssertionError(it.toString()) }
        return null
    }
}