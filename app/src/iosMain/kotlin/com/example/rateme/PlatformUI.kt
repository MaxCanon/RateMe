package com.example.rateme

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIActivityViewController
import platform.Foundation.NSUserDefaults

actual fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url)
    if (nsUrl != null) {
        UIApplication.sharedApplication.openURL(nsUrl)
    }
}

actual fun showToast(message: String) {
    println("TOAST: $message")
}

actual fun shareText(text: String) {
    val window = UIApplication.sharedApplication.keyWindow
    val rootViewController = window?.rootViewController
    
    val activityViewController = UIActivityViewController(
        activityItems = listOf(text),
        applicationActivities = null
    )
    
    rootViewController?.presentViewController(
        viewControllerToPresent = activityViewController,
        animated = true,
        completion = null
    )
}

actual fun changeLanguage(langCode: String) {
    // On iOS, language is usually changed in system settings, 
    // but we can store preference for future app-specific logic
    NSUserDefaults.standardUserDefaults.setObject(listOf(langCode), forKey = "AppleLanguages")
}
