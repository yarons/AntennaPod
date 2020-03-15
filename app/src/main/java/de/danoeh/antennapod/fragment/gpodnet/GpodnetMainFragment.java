package de.danoeh.antennapod.fragment.gpodnet;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;

/**
 * Main navigation hub for gpodder.net podcast directory
 */
public class GpodnetMainFragment extends Fragment {
    private Toolbar toolbar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.pager_fragment, container, false);

        ViewPager viewPager = root.findViewById(R.id.viewpager);
        GpodnetPagerAdapter pagerAdapter = new GpodnetPagerAdapter(getChildFragmentManager(), getResources());
        viewPager.setAdapter(pagerAdapter);

        toolbar = root.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());
        toolbar.setTitle(R.string.gpodnet_main_label);
        setupMenu();

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = root.findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);

        return root;
    }

    private void setupMenu() {
        toolbar.inflateMenu(R.menu.gpodder_podcasts);
        MenuItem searchItem = toolbar.getMenu().findItem(R.id.action_search);
        final SearchView sv = (SearchView) MenuItemCompat.getActionView(searchItem);
        sv.setQueryHint(getString(R.string.gpodnet_search_hint));
        sv.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                sv.clearFocus();
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                    activity.loadChildFragment(SearchListFragment.newInstance(s));
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
    }

    public class GpodnetPagerAdapter extends FragmentPagerAdapter {


        private static final int NUM_PAGES = 2;
        private static final int POS_TOPLIST = 0;
        private static final int POS_TAGS = 1;
        private static final int POS_SUGGESTIONS = 2;

        final Resources resources;

        public GpodnetPagerAdapter(FragmentManager fm, Resources resources) {
            super(fm);
            this.resources = resources;
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case POS_TAGS:
                    return new TagListFragment();
                case POS_TOPLIST:
                    return new PodcastTopListFragment();
                case POS_SUGGESTIONS:
                    return new SuggestionListFragment();
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case POS_TAGS:
                    return getString(R.string.gpodnet_taglist_header);
                case POS_TOPLIST:
                    return getString(R.string.gpodnet_toplist_header);
                case POS_SUGGESTIONS:
                    return getString(R.string.gpodnet_suggestions_header);
                default:
                    return super.getPageTitle(position);
            }
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }
}
