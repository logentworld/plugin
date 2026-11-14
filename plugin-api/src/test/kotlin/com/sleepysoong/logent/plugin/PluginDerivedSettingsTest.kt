package com.sleepysoong.logent.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginDerivedSettingsTest {
    @Test
    fun `기존 기본 버전과 명시적 버전은 분리돼요`() {
        val legacy = object : LogentPlugin {
            override val id = "legacy"
            override val name = "Legacy"
            override val version = "1"
        }
        val derived = object : LogentPlugin, PluginDerivedSettings {
            override val id = "derived"
            override val name = "Derived"
            override val version = "1"
            override val apiVersion = PLUGIN_DERIVED_SETTINGS_API_VERSION
            override fun derivedSettings() = listOf(PluginDerivedSetting("accounts", "usage", "Usage", dependencyKeys = listOf("token")))
        }
        assertEquals(4, PLUGIN_API_VERSION)
        assertEquals(4, legacy.apiVersion)
        assertEquals(5, derived.apiVersion)
        assertEquals("usage", derived.derivedSettings().single().key)
        val defaults = Class.forName("com.sleepysoong.logent.plugin.LogentPlugin\$DefaultImpls")
        assertEquals(4, defaults.getMethod("getApiVersion", LogentPlugin::class.java).invoke(null, legacy))
    }

    @Test
    fun `파생 선언은 공통 키와 제목 및 의존성 규칙을 검증해요`() {
        val valid = PluginDerivedSetting("accounts", "usage", "Usage", dependencyKeys = listOf("token", "region"))
        assertEquals("", valid.description)
        assertEquals(listOf("token", "region"), valid.dependencyKeys)
        assertTrue(runCatching { PluginDerivedSetting("Accounts", "usage", "Usage", dependencyKeys = emptyList()) }.isFailure)
        assertTrue(runCatching { PluginDerivedSetting("accounts", "bad key", "Usage", dependencyKeys = emptyList()) }.isFailure)
        assertTrue(runCatching { PluginDerivedSetting("accounts", "usage", " ", dependencyKeys = emptyList()) }.isFailure)
        assertTrue(runCatching { PluginDerivedSetting("accounts", "usage", "Usage", dependencyKeys = listOf("Bad")) }.isFailure)
        assertTrue(runCatching { PluginDerivedSetting("accounts", "usage", "Usage", dependencyKeys = listOf("token", "token")) }.isFailure)
        assertTrue(runCatching { PluginDerivedSetting("accounts", "usage", "Usage", dependencyKeys = listOf("usage")) }.isFailure)
    }

    @Test
    fun `스냅샷은 원본을 보존하되 문자열에 아무 값도 노출하지 않아요`() {
        val entry = PluginSettingEntry("item-id", mapOf("secret-key" to "secret-value"))
        val values = mapOf("derived-key" to "derived-value")
        val revisions = mapOf("derived-key" to "revision-value")
        val snapshot = PluginDerivedSettingSnapshot(entry, values, revisions)
        assertSame(entry, snapshot.entry)
        assertSame(values, snapshot.values)
        assertSame(revisions, snapshot.revisions)
        listOf("item-id", "secret-key", "secret-value", "derived-key", "derived-value", "revision-value").forEach {
            assertFalse(snapshot.toString().contains(it))
        }
    }
}
