package com.nao20010128nao.Cryptorage.internal.file

import com.google.common.base.Optional
import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.internal.unsupported
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

internal class ZipFileSource(file: File) : FileSource {
    private val zf = ZipFile(file)
    override val isReadOnly: Boolean = true

    override fun close() {
        zf.close()
    }

    override fun commit() {
    }

    override fun list(): Array<String> = zf.entries().asSequence().map { it.name }.toList().toTypedArray()

    override fun open(name: String, offset: Int): ByteSource = object : ByteSource() {
        val entry = zf.getEntry(name)!!
        override fun openStream(): InputStream = zf.getInputStream(entry)!!
        override fun sizeIfKnown(): Optional<Long> = if (entry.size < 0L) {
            Optional.absent()
        } else {
            Optional.of(entry.size)
        }
    }

    override fun put(name: String): ByteSink = unsupported("FileSource", "put")

    override fun delete(name: String) = unsupported("FileSource", "delete")
}