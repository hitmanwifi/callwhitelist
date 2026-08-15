package org.alexrust.callwhitelist.system

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object CallScreeningAccess {
    fun isRoleAvailable(context: Context): Boolean {
        return roleManager(context)?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true
    }

    fun isRoleHeld(context: Context): Boolean {
        return roleManager(context)?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
    }

    fun createRoleRequestIntent(context: Context): Intent {
        val manager = roleManager(context)
        return if (manager != null && manager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            manager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
        } else {
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        }
    }

    fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun roleManager(context: Context): RoleManager? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return context.getSystemService(RoleManager::class.java)
    }
}
