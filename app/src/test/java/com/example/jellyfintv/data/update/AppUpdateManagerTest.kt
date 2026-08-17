package com.example.jellyfintv.data.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun `higher patch version is newer`() {
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0", "1.0.1"))
    }

    @Test
    fun `double-digit minor version beats single-digit minor version numerically, not lexically`() {
        // A plain string compare would put "1.9.0" ahead of "1.10.0" - must not regress to that.
        assertTrue(AppUpdateManager.isNewerVersion("1.9.0", "1.10.0"))
        assertFalse(AppUpdateManager.isNewerVersion("1.10.0", "1.9.0"))
    }

    @Test
    fun `leading v prefix on the tag name is ignored`() {
        assertTrue(AppUpdateManager.isNewerVersion("1.0", "v1.1"))
    }

    @Test
    fun `equal versions are not newer`() {
        assertFalse(AppUpdateManager.isNewerVersion("1.0.0", "1.0.0"))
        assertFalse(AppUpdateManager.isNewerVersion("v1.0.0", "1.0.0"))
    }

    @Test
    fun `older version is not newer`() {
        assertFalse(AppUpdateManager.isNewerVersion("2.0.0", "1.9.9"))
    }

    @Test
    fun `missing trailing segments are treated as zero`() {
        assertTrue(AppUpdateManager.isNewerVersion("1.0", "1.0.1"))
        assertFalse(AppUpdateManager.isNewerVersion("1.0.0", "1.0"))
    }

    @Test
    fun `pre-release and build-metadata suffixes are stripped before comparing`() {
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0", "1.1.0-beta"))
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0", "1.1.0+build.5"))
        assertFalse(AppUpdateManager.isNewerVersion("1.1.0", "1.1.0-beta"))
    }

    @Test
    fun `non-numeric segments do not crash and are treated as zero`() {
        assertFalse(AppUpdateManager.isNewerVersion("1.0.0", "1.0.x"))
    }
}
