/**
 * The package declaration MUST match the file's directory structure. This file is at:
 * src/main/java/com/example/plugins/FirebaseAuth.kt Therefore, the package MUST be
 * com.example.plugins
 */
package com.example.plugins

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import io.ktor.server.application.*
import io.ktor.server.auth.*
import java.io.FileInputStream

data class FirebaseUser(val uid: String, val email: String?, val name: String?) : Principal

class FirebaseAuthenticationProvider internal constructor(config: Config) :
        AuthenticationProvider(config) {

    val authenticationFunction = config.authenticationFunction

    class Config internal constructor(name: String?) : AuthenticationProvider.Config(name) {
        internal var authenticationFunction: suspend ApplicationCall.(String) -> Principal? = {
            throw NotImplementedError("Firebase auth validate function is not specified.")
        }

        fun validate(body: suspend ApplicationCall.(String) -> Principal?) {
            authenticationFunction = body
        }
    }

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")

        val principal = token?.let { authenticationFunction(call, it) }

        if (principal != null) {
            context.principal(principal)
        }
    }
}

fun AuthenticationConfig.firebase(
        name: String? = null,
        configure: FirebaseAuthenticationProvider.Config.() -> Unit
) {
    val provider =
            FirebaseAuthenticationProvider(
                    FirebaseAuthenticationProvider.Config(name).apply(configure)
            )
    register(provider)
}

fun Application.configureFirebase() {
    try {
        val serviceAccount = FileInputStream("src/main/resources/firebase-service-account.json")

        val options =
                FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build()

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
            println("✅ Firebase initialized successfully!")
        } else {
            println("✅ Firebase already initialized")
        }
    } catch (e: Exception) {
        println("❌ Failed to initialize Firebase: ${e.message}")
        throw e
    }
}

suspend fun ApplicationCall.verifyFirebaseToken(token: String): Principal? {
    return try {
        val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
        FirebaseUser(uid = decodedToken.uid, email = decodedToken.email, name = decodedToken.name)
    } catch (e: Exception) {
        application.log.error("Failed to verify Firebase token: ${e.message}")
        null
    }
}
