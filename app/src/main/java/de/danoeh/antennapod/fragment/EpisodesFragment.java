package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.MenuAwareFragmentPagerAdapter;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class EpisodesFragment extends Fragment implements Toolbar.OnMenuItemClickListener {

    public static final String TAG = "EpisodesFragment";
    private static final String PREF_LAST_TAB_POSITION = "tab_position";

    private static final int POS_NEW_EPISODES = 0;
    private static final int POS_ALL_EPISODES = 1;
    private static final int POS_FAV_EPISODES = 2;
    private static final int TOTAL_COUNT = 3;

    private Toolbar toolbar;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private EpisodesPagerAdapter pagerAdapter;
    private boolean isUpdatingFeeds = false;

    //Mandatory Constructor
    public EpisodesFragment() {
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.pager_fragment, container, false);

        toolbar = rootView.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.episodes_label);
        toolbar.setOnMenuItemClickListener(this);
        DrawerLayout drawerLayout = ((MainActivity) getActivity()).getDrawerLayout();
        actionBarDrawerToggle = new ActionBarDrawerToggle(getActivity(),
                drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);

        viewPager = rootView.findViewById(R.id.viewpager);
        pagerAdapter = new EpisodesPagerAdapter();
        viewPager.setAdapter(pagerAdapter);

        // Give the TabLayout the ViewPager
        tabLayout = rootView.findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        actionBarDrawerToggle.syncState();
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
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return pagerAdapter.onOptionsItemSelected(item);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DownloadEvent event) {
        if (event.hasChangedFeedUpdateStatus(isUpdatingFeeds)) {
            isUpdatingFeeds = DownloadRequester.getInstance().isDownloadingFeeds();
            pagerAdapter.invalidateMenu();
        }
    }

    public class EpisodesPagerAdapter extends MenuAwareFragmentPagerAdapter {

        public EpisodesPagerAdapter() {
            super(getChildFragmentManager(), getContext(), toolbar.getMenu());
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new NewEpisodesFragment();
                case 1:
                    return new AllEpisodesFragment();
                default:
                    return new FavoriteEpisodesFragment();
            }
        }

        @Override
        public int getCount() {
            return TOTAL_COUNT;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case POS_ALL_EPISODES:
                    return getString(R.string.all_episodes_short_label);
                case POS_NEW_EPISODES:
                    return getString(R.string.new_episodes_label);
                case POS_FAV_EPISODES:
                    return getString(R.string.favorite_episodes_label);
                default:
                    return super.getPageTitle(position);
            }
        }
    }
}
