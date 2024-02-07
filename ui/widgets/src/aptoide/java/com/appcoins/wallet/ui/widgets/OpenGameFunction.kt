package com.appcoins.wallet.ui.widgets

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat.startActivity
import com.appcoins.wallet.core.analytics.analytics.legacy.GetAppAnalytics
import okhttp3.internal.applyConnectionSpec

fun openGame(
  gamePackage: String?,
  actionUrl: String?,
  context: Context,
  getAppAnalytics: GetAppAnalytics?
) {
  try {
    val launchIntent: Intent? = gamePackage?.let {
      context.packageManager.getLaunchIntentForPackage(
        it
      )
    }
    if (launchIntent != null) {
      getAppAnalytics?.sendGetAppAEvent(gamePackage, GetAppAnalytics.OPEN_ACTION)
      startActivity(context, launchIntent, null)
    } else
      getGame(gamePackage, actionUrl, context, getAppAnalytics)
  } catch (e: Throwable) {
    getGame(gamePackage, actionUrl, context, getAppAnalytics)
  }
}

fun isPackageInstalled(packageName: String?, packageManager: PackageManager): Boolean {
  if (packageName == null) {
    return false
  }
  return try {
    packageManager.getPackageInfo(packageName, 0)
    true
  } catch (e: PackageManager.NameNotFoundException) {
    false
  }
}

private fun getGame(
  gamePackage: String?,
  actionUrl: String?,
  context: Context,
  getAppAnalytics: GetAppAnalytics?
) {
  getAppAnalytics?.sendGetAppAEvent(gamePackage, GetAppAnalytics.GET_ACTION)
  if (!actionUrl.isNullOrEmpty()) {
    getGameFromUrl(actionUrl, context)
  } else {
    try {  // else tries to open with Aptoide store
      val intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=$gamePackage")
      )
      intent.setPackage("cm.aptoide.pt")
      startActivity(context, intent, null)
    } catch (_: ActivityNotFoundException) {
      // no store, go to aptoide webpage
      getGameFromUrl("https://en.aptoide.com/", context)
    }
  }
}

private fun getGameFromUrl(actionUrl: String?, context: Context) {
  val intent = Intent(
    Intent.ACTION_VIEW,
    Uri.parse(actionUrl)
  )
  startActivity(context, intent, null)
}
