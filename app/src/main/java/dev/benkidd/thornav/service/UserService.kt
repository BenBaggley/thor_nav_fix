package dev.benkidd.thornav.service

import android.content.Context
import androidx.annotation.Keep
import dev.benkidd.thornav.IUserService
import kotlin.system.exitProcess

@Keep
class UserService : IUserService.Stub {

    @Keep
    constructor() : super()

    @Keep
    constructor(@Suppress("UNUSED_PARAMETER") context: Context) : super()

    override fun destroy() {
        exitProcess(0)
    }

    override fun grantPermission(packageName: String, permission: String) {
        val process = Runtime.getRuntime().exec(arrayOf("pm", "grant", packageName, permission))
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw IllegalStateException("pm grant failed with exit code $exitCode")
        }
    }
}
