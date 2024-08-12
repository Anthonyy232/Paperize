package com.anthonyla.paperize.feature.wallpaper.tasker_shortcut

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
    override fun run(context: Context, input: TaskerInput<Unit>): TaskerPluginResult<Unit> {
        val intent = Intent(context, WallpaperBootAndChangeReceiver::class.java)
        context.sendBroadcast(intent)
        return TaskerPluginResultSucess()
    }
}
