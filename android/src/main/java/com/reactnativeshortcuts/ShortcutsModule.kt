package com.reactnativeshortcuts

import android.annotation.TargetApi
import android.app.Activity
import android.app.Person
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import android.os.PersistableBundle
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@ReactModule(name = ShortcutsModule.MODULE_NAME)
class ShortcutsModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext),
    ActivityEventListener {

    private val TAG = "shortcutsModule"

    companion object {
        const val MODULE_NAME = "RNShortcuts"
        const val INTENT_ACTION_SHORTCUT = "com.reactnativeshortcuts.shortcutsModule"
        const val EVENT_ON_SHORTCUT_ITEM_PRESSED = "onShortcutItemPressed"
    }

    init {
        reactContext.addActivityEventListener(this)
        ContextHolder.setApplicationContext(reactContext);
    }

    override fun onCatalystInstanceDestroy() {
        reactApplicationContext.removeActivityEventListener(this)
        super.onCatalystInstanceDestroy()
    }

    override fun onActivityResult(activity: Activity?, requestCode: Int, resultCode: Int, data: Intent?) {
        // No implementation needed
    }

    override fun onNewIntent(intent: Intent?) {
        emitEvent(intent)
    }

    override fun getName(): String {
        return MODULE_NAME
    }

    @ReactMethod
    @TargetApi(30)
    fun addShortcut(shortcutItem: ReadableMap, promise: Promise) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            promise.resolve("not supported");
            return;
        }

        val context = reactApplicationContext
        val activity = currentActivity

        if (context == null) {
          promise.reject("no context for shortcutItem");
          return;
        }

        val shortcutItem = ShortcutItem.fromReadableMap(shortcutItem)

        if (shortcutItem == null) {
            promise.reject("wrong shortcutItem");
            return;
        }

        val intent: Intent?

        if (activity == null) {
          intent = createLaunchActivityIntent(context)
        } else {
          intent = Intent(context, activity::class.java)
        }

        if (intent == null) {
          promise.reject("no intent for shortcutItem");
          return;
        }

        intent.action = INTENT_ACTION_SHORTCUT
        intent.putExtra("shortcutItem", shortcutItem.toBundle())

        val (id, title, shortTitle, iconName, data, personName, personIcon, longLived) = shortcutItem;

        val builder = ShortcutInfo
            .Builder(reactApplicationContext, id)
            .setLongLabel(title)
            .setShortLabel(shortTitle)
            .setIntent(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (personName != null) {
                val personBuilder = Person.Builder();
                personBuilder.setBot(false);
                personBuilder.setName(personName);
                builder.setLongLabel(personName);
                builder.setPerson(personBuilder.build());

                if (personIcon != null) {
                    var largeIconBitmap: Bitmap? = null
                    try {
                        largeIconBitmap = Tasks.await<Bitmap>(
                            ResourceUtils.getImageBitmapFromUrl(
                                personIcon
                            ), 10, TimeUnit.SECONDS
                        )
                    } catch (e: TimeoutException) {
                        Log.e(TAG, "Timeout occurred whilst trying to retrieve a largeIcon image: $personIcon", e);
                    } catch (e: Exception) {
                        Log.e(TAG, "An error occurred whilst trying to retrieve a largeIcon image: $personIcon", e);
                    }
                    if (largeIconBitmap != null) {
                        Log.d(TAG, "Successfully created bitmap")

                        // square icons break notifications, but working in shortcut 🤷‍♂️
                        largeIconBitmap = ResourceUtils.getCircularBitmap(largeIconBitmap);

                        var icon = Icon.createWithBitmap(largeIconBitmap)
                        personBuilder.setIcon(icon);
                        builder.setIcon(icon);
                    }
                }
            }

            if (longLived) {
                builder.setLongLived(longLived);
            }
        }

        if (iconName != null) {
            val resourceId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
            builder.setIcon(Icon.createWithResource(context, resourceId))
        }

        var shortcutInfo = builder.build();

        val shortcutManager = context.getSystemService<ShortcutManager>(ShortcutManager::class.java)
        shortcutManager?.pushDynamicShortcut(shortcutInfo);

        promise.resolve("success")
    }

    @ReactMethod
    @TargetApi(25)
    fun setShortcuts(items: ReadableArray, promise: Promise) {
        if (!isSupported()) {
            promise.reject(NotSupportedException)
        }

        val context = reactApplicationContext ?: return
        val activity = currentActivity ?: return

        val shortcutItems = items.toArrayList().mapIndexed { index, _ ->
            val map = items.getMap(index) ?: return
            ShortcutItem.fromReadableMap(map)
        }.filterNotNull()

        val shortcuts = shortcutItems.map {
            val intent = Intent(reactApplicationContext, activity::class.java)
            intent.action = INTENT_ACTION_SHORTCUT
            intent.putExtra("shortcutItem", it.toBundle())

            val (type, title, shortTitle, iconName) = it

            val builder = ShortcutInfo
                .Builder(reactApplicationContext, type)
                .setLongLabel(title)
                .setShortLabel(shortTitle)
                .setIntent(intent)

            if (iconName != null) {
                val resourceId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
                builder.setIcon(Icon.createWithResource(context, resourceId))
            }

            builder.build()
        }

        val shortcutManager = activity.getSystemService<ShortcutManager>(ShortcutManager::class.java)
        shortcutManager?.dynamicShortcuts = shortcuts

        promise.resolve(ShortcutItem.toWritableArray(shortcutItems))
    }

    @ReactMethod
    @TargetApi(25)
    fun getShortcuts(promise: Promise) {
        if (!isSupported()) {
            promise.reject(NotSupportedException)
        }

        val shortcutManager = currentActivity?.getSystemService<ShortcutManager>(ShortcutManager::class.java)
        val shortcutItems = shortcutManager?.dynamicShortcuts?.map {
            ShortcutItem(it.id, it.longLabel.toString(), it.shortLabel.toString(), null, null, null, null, false)
        }

        promise.resolve(ShortcutItem.toWritableArray(shortcutItems ?: arrayListOf()))
    }

    @ReactMethod
    @TargetApi(25)
    fun getInitialShortcut(promise: Promise) {
        if (!isSupported()) {
            promise.reject(NotSupportedException)
        }

        val shortcutItem = getShortcutItemFromIntent(currentActivity?.intent)

        promise.resolve(shortcutItem?.toMap())
    }

    @ReactMethod
    @TargetApi(25)
    fun clearShortcuts() {
        if (!isSupported()) {
            return
        }

        val shortcutManager = currentActivity?.getSystemService<ShortcutManager>(ShortcutManager::class.java)
        shortcutManager?.removeAllDynamicShortcuts()
    }

    private fun getShortcutItemFromIntent(intent: Intent?): ShortcutItem? {
        if (intent?.action !== INTENT_ACTION_SHORTCUT) {
            return null
        }

        val bundle = intent.getParcelableExtra<PersistableBundle>("shortcutItem") ?: return null
        return ShortcutItem.fromPersistentBundle(bundle)
    }

    private fun emitEvent(intent: Intent?) {
        val shortcutItem = getShortcutItemFromIntent(intent) ?: return

        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(EVENT_ON_SHORTCUT_ITEM_PRESSED, shortcutItem.toMap())
    }

    fun isSupported(): Boolean {
        return Build.VERSION.SDK_INT >= 25
    }

  fun createLaunchActivityIntent(
    context: Context
  ): Intent? {
    try {
      var launchActivityIntent =
        context.packageManager.getLaunchIntentForPackage(context.packageName)
      var launchActivity: String? = null

      if (launchActivityIntent == null && launchActivity != null) {
        val launchActivityClass: Class<*> =
          IntentUtils.getLaunchActivity(launchActivity)
        launchActivityIntent = Intent(context, launchActivityClass)
        launchActivityIntent.setPackage(null)
        launchActivityIntent.flags =
          Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
      }
      return launchActivityIntent
    } catch (e: java.lang.Exception) {
      Log.e(
        TAG,
        "Failed to create LaunchActivityIntent",
        e
      )
    }
    return null
  }
}

object NotSupportedException: Throwable("Feature not supported, requires version 25 or above")
