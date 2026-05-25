package com.lachlan.stitchstash.data.drive

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

/**
 * Thin helper around the legacy GoogleSignIn API. Modern Credential Manager doesn't
 * cover Drive scope OAuth directly, so we keep the classic flow here. Returns the
 * signed-in account email, which Drive REST API uses for token acquisition.
 */
object GoogleSignInHelper {

    fun client(context: Context): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    fun currentAccountName(context: Context): String? =
        GoogleSignIn.getLastSignedInAccount(context)?.email

    fun signInIntent(context: Context): Intent = client(context).signInIntent

    fun signOut(context: Context, onDone: () -> Unit) {
        client(context).signOut().addOnCompleteListener { onDone() }
    }
}
