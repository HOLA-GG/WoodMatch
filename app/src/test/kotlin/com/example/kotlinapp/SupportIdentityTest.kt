package com.example.kotlinapp
import com.example.kotlinapp.data.model.User
import com.example.kotlinapp.data.model.UserRole
import org.junit.Assert.*
import org.junit.Test
class SupportIdentityTest {
    @Test fun legacyAdministrativeRolesHaveNoCommunityPrivileges() {
        for (role in listOf("SOPORTE", "ADMIN", "admin")) {
            val user = User(uid = "legacy", role = role)
            assertFalse(user.isAdmin)
            assertFalse(user.canPublish)
            assertFalse(user.canRequest)
            assertEquals(UserRole.INVITADO, user.typedRole)
        }
    }
    @Test fun communityRolesKeepTheirCapabilities() {
        assertTrue(User(role = "MIXTO").canPublish)
        assertTrue(User(role = "MIXTO").canRequest)
        assertFalse(User(role = "RECEPTOR").canPublish)
    }
}
