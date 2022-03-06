package com.dorcaapps.android.ktor.handler

import com.dorcaapps.android.ktor.Constants
import com.dorcaapps.android.ktor.dto.SessionCookie
import com.dorcaapps.android.ktor.extensions.toMD5ByteArray
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.sessions.*
import io.ktor.util.*
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticationHandler @Inject constructor(fileHandler: FileHandler) {
    private val sessionCookieMap = mutableMapOf<String, OffsetDateTime>()

    val authenticationConfig: Authentication.Configuration.() -> Unit = {
        session<SessionCookie>(Constants.Authentication.SESSION) {
            validate {
                handleCookie(sessions, it)
            }
            challenge {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
        digest(name = Constants.Authentication.LOGIN) {
            realm = "login"

            @OptIn(KtorExperimentalAPI::class)
            digestProvider { userName, realm ->
                when (userName) {
                    "missing" -> null
                    else -> {
                        val savedLoginData =
                            fileHandler.getLoginData(userName) ?: return@digestProvider null

                        "$userName:$realm:${savedLoginData.password}".toMD5ByteArray()
                    }
                }
            }
        }
    }

    fun getNewSessionCookie(): SessionCookie = SessionCookie.create().apply {
        sessionCookieMap[id] = OffsetDateTime.now().plusMinutes(10)
    }

    private fun handleCookie(session: CurrentSession, sessionCookie: SessionCookie): Principal? =
        if (isValidCookie(sessionCookie)) {
            sessionCookie
        } else {
            session.clear<SessionCookie>()
            sessionCookieMap.remove(sessionCookie.id)
            null
        }

    private fun isValidCookie(sessionCookie: SessionCookie): Boolean =
        sessionCookieMap[sessionCookie.id]?.isAfter(
            OffsetDateTime.now()
        ) ?: false
}