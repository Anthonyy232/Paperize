package com.anthonyla.paperize.feature.wallpaper.tasker_shortcut

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager.WallpaperBootAndChangeReceiver
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutputOrInput
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoOutputOrInput
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigNoInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess

/**
 * Tasker Shortcut to change wallpaper
 */
class ShortcutHelper(config: TaskerPluginConfig<Unit>) : TaskerPluginConfigHelperNoOutputOrInput<ShortcutActionRunner>(config) {
    override val runnerClass: Class<ShortcutActionRunner> get() = ShortcutActionRunner::class.java
    override fun addToStringBlurb(input: TaskerInput<Unit>, blurbBuilder: StringBuilder) {
        blurbBuilder.append("Changes the wallpaper")
    }
}

class ActivityConfigBasicAction : Activity(), TaskerPluginConfigNoInput {
    override val context: Context get() = applicationContext
    private val taskerHelper by lazy { ShortcutHelper(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskerHelper.finishForTasker()
    }
}

class ShortcutActionRunner : TaskerPluginRunnerActionNoOutputOrInput() {
    companion object {
        private const val ACTION_CHANGE_WALLPAPER = "com.anthonyla.paperize.SHORTCUT"
    }

    override fun run(context: Context, input: TaskerInput<Unit>): TaskerPluginResult<Unit> {
        Handler(Looper.getMainLooper()).post {
            val intent = Intent(ACTION_CHANGE_WALLPAPER).apply {
                setClass(context, WallpaperBootAndChangeReceiver::class.java)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            context.sendBroadcast(intent)
        }
        return TaskerPluginResultSucess()
    }
}