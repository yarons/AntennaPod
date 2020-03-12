package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.MenuAwareFragmentPagerAdapter;

/**
 * Shows the CompletedDownloadsFragment and the RunningDownloadsFragment
 */
public class DownloadsFragment extends Fragment implements Toolbar.OnMenuItemClickListener {

    public static final String TAG = "DownloadsFragment";

    public static final String ARG_SELECTED_TAB = "selected_tab";

    public static final int POS_RUNNING = 0;
    private static final int POS_COMPLETED = 1;
    public static final int POS_LOG = 2;

    private static final String PREF_LAST_TAB_POSITION = "tab_position";

    private ActionBarDrawerToggle actionBarDrawerToggle;
    private ViewPager viewPager;
    private TabLayout tabLayout;
    private DownloadsPagerAdapter pagerAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.pager_fragment, container, false);

        Toolbar toolbar = root.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.downloads_label);
        toolbar.setOnMenuItemClickListener(this);
        DrawerLayout drawerLayout = ((MainActivity) getActivity()).getDrawerLayout();
        actionBarDrawerToggle = new ActionBarDrawerToggle(getActivity(),
                drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);

        viewPager = root.findViewById(R.id.viewpager);
        pagerAdapter = new DownloadsPagerAdapter(getChildFragmentManager(), getContext(), toolbar.getMenu());
        viewPager.setAdapter(pagerAdapter);

        // Give the TabLayout the ViewPager
        tabLayout = root.findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);
        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        actionBarDrawerToggle.syncState();
        if (getArguments() != null) {
            int tab = getArguments().getInt(ARG_SELECTED_TAB);
            viewPager.setCurrentItem(tab, false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // save our tab selection
        SharedPreferences prefs = getActivity().getSharedPreferences(TAG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_LAST_TAB_POSITION, tabLayout.getSelectedTabPosition());
        editor.apply();
    }

    @Override
    public void onStart() {
        super.onStart();

        // restore our last position
        SharedPreferences prefs = getActivity().getSharedPreferences(TAG, Context.MODE_PRIVATE);
        int lastPosition = prefs.getInt(PREF_LAST_TAB_POSITION, 0);
        viewPager.setCurrentItem(lastPosition);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return pagerAdapter.onOptionsItemSelected(item);
    }

    public class DownloadsPagerAdapter extends MenuAwareFragmentPagerAdapter {

        public DownloadsPagerAdapter(FragmentManager fm, Context context, Menu menu) {
            super(fm, context, menu);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case POS_RUNNING:
                    return new RunningDownloadsFragment();
                case POS_COMPLETED:
                    return new CompletedDownloadsFragment();
                case POS_LOG:
                    return new DownloadLogFragment();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case POS_RUNNING:
                    return getString(R.string.downloads_running_label);
                case POS_COMPLETED:
                    return getString(R.string.downloads_completed_label);
                case POS_LOG:
                    return getString(R.string.downloads_log_label);
                default:
                    return super.getPageTitle(position);
            }
        }
    }
}
