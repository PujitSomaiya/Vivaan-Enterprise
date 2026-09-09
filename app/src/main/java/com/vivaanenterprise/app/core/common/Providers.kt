package com.vivaanenterprise.app.core.common

import java.util.UUID

interface IdGenerator {
    fun newId(): String
}

class DefaultIdGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}

interface TimeProvider {
    fun currentTimeMillis(): Long
}

class SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
