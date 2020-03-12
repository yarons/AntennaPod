package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

/**
 * Automatically delegates menu creation to the currently active Fragment.
 */
public abstract class MenuAwareFragmentPagerAdapter extends FragmentPagerAdapter {
    private final Context context;
    private final Menu menu;
    private Fragment currentFragment;

    public MenuAwareFragmentPagerAdapter(FragmentManager fragmentManager, Context context, Menu menu) {
        super(fragmentManager);
        this.context = context;
        this.menu = menu;
    }

    @Override
    public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        if (currentFragment != object) {
            currentFragment = ((Fragment) object);
            new Handler().post(this::invalidateMenu);
        }
        super.setPrimaryItem(container, position, object);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return currentFragment.onOptionsItemSelected(item);
    }

    public void invalidateMenu() {
        menu.clear();
        currentFragment.onCreateOptionsMenu(menu, new MenuInflater(context));
        currentFragment.onPrepareOptionsMenu(menu);
    }
}
