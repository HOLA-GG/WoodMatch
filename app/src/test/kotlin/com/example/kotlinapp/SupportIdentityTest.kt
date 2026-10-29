package com.example.kotlinapp

import com.example.kotlinapp.data.model.User
import com.example.kotlinapp.data.model.UserRole
import org.junit.Assert.*
import org.junit.Test

class SupportIdentityTest {
    @Test fun legacyProfileUsesExclusiveSupportNavigation() {
        val user = User(uid = "yDGu1WUesuc01uUbUzc3By4qyL02", email = "cookiestech@woodmatch.org", role = "GENERADOR")
        assertTrue(user.isSupport)
        assertEquals(UserRole.SOPORTE, user.typedRole)
        assertEquals("SOPORTE", user.navigationRole)
        assertFalse(user.canPublish)
        assertFalse(user.canRequest)
        assertFalse(user.isAdmin)
    }

    @Test fun matchingEmailAloneDoesNotIdentifySupport() {
        val user = User(uid = "other", email = "cookiestech@woodmatch.org", role = "RECEPTOR")
        assertFalse(user.isSupport)
        assertEquals(UserRole.RECEPTOR, user.typedRole)
        assertTrue(user.canRequest)
    }

    @Test fun matchingUidWithoutEmailDoesNotIdentifySupport() {
        val user = User(uid = "yDGu1WUesuc01uUbUzc3By4qyL02", email = "other@example.org", role = "GENERADOR")
        assertFalse(user.isSupport)
    }
}
