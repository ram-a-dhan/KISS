package fr.neamar.kiss.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.utils.UserHandle;

/**
 * This class gets called when an application is created or removed on the
 * system
 * <p/>
 * We then recreate our data set.
 *
 * @author dorvaryn
 */
public class PackageAddedRemovedHandler extends BroadcastReceiver {

    public static void handleEvent(Context ctx, String action, String packageName, UserHandle user, boolean replacing) {
        if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            if (!replacing) {
                Intent launchIntent = ctx.getPackageManager().getLaunchIntentForPackage(packageName);
                // launchIntent can be null for some plugin app
                if (launchIntent != null) {
                    // Add new package to history
                    if (PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean("enable-app-history", true)) {
                        String className = launchIntent.getComponent().getClassName();
                        String pojoID = user.addUserSuffixToString("app://" + packageName + "/" + className, '/');
                        KissApplication.getApplication(ctx).getDataHandler().addToHistory(pojoID);
                    }
                }
            }
        }

        if (Intent.ACTION_PACKAGE_REMOVED.equals(action) && !replacing) {
            // Remove all installed shortcuts
            KissApplication.getApplication(ctx).getDataHandler().removeShortcuts(packageName);
            KissApplication.getApplication(ctx).getDataHandler().removeFromExcluded(packageName);
        }

        KissApplication.getApplication(ctx).resetIconsHandler();

        // Reload application list
        KissApplication.getApplication(ctx).getDataHandler().reloadApps();
        // reload shortcuts
        KissApplication.getApplication(ctx).getDataHandler().reloadShortcuts();
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {

        String packageName = intent.getData().getSchemeSpecificPart();

        if (packageName.equalsIgnoreCase(ctx.getPackageName())) {
            // When running KISS locally, sending a new version of the APK immediately triggers a "package removed" for fr.neamar.kiss,
            // There is no need to handle this event.
            // Discarding it makes startup time much faster locally as apps don't have to be loaded twice.
            return;
        }

        handleEvent(ctx,
                intent.getAction(),
                packageName, new UserHandle(),
                intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
        );

    }

}
