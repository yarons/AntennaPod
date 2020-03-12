package de.danoeh.antennapod.fragment;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.AdapterView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;
import androidx.core.view.MenuItemCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import de.danoeh.antennapod.core.event.PlaybackHistoryEvent;
import de.danoeh.antennapod.core.event.PlayerStatusEvent;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.FeedItemlistAdapter;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.event.DownloaderUpdate;
import de.danoeh.antennapod.core.event.FeedItemEvent;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.view.EmptyViewHandler;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class PlaybackHistoryFragment extends Fragment implements Toolbar.OnMenuItemClickListener,
        AdapterView.OnItemClickListener {
    public static final String TAG = "PlaybackHistoryFragment";

    private List<FeedItem> playbackHistory;
    private FeedItemlistAdapter adapter;
    private Disposable disposable;
    private Toolbar toolbar;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.simple_list_fragment, container, false);

        toolbar = root.findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setTitle(R.string.playback_history_label);
        DrawerLayout drawerLayout = ((MainActivity) getActivity()).getDrawerLayout();
        actionBarDrawerToggle = new ActionBarDrawerToggle(getActivity(),
                drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        invalidateOptionsMenu();

        listView = root.findViewById(android.R.id.list);

        EmptyViewHandler emptyView = new EmptyViewHandler(getActivity());
        emptyView.setIcon(R.attr.ic_history);
        emptyView.setTitle(R.string.no_history_head_label);
        emptyView.setMessage(R.string.no_history_label);
        emptyView.attachToListView(listView);

        adapter = new FeedItemlistAdapter((MainActivity) getActivity(), itemAccess, true, false);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        actionBarDrawerToggle.syncState();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        loadItems();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(DownloadEvent event) {
        Log.d(TAG, "onEvent() called with: " + "event = [" + event + "]");
        DownloaderUpdate update = event.update;
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        position -= listView.getHeaderViewsCount();
        long[] ids = FeedItemUtil.getIds(playbackHistory);
        ((MainActivity) getActivity()).loadChildFragment(ItemPagerFragment.newInstance(ids, position));
    }

    private void invalidateOptionsMenu() {
        if (toolbar.getMenu().size() == 0) {
            MenuItem clearHistory = toolbar.getMenu().add(Menu.NONE, R.id.clear_history_item,
                    Menu.CATEGORY_CONTAINER, R.string.clear_history_label);
            MenuItemCompat.setShowAsAction(clearHistory, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
            TypedArray drawables = getActivity().obtainStyledAttributes(new int[]{R.attr.content_discard});
            clearHistory.setIcon(drawables.getDrawable(0));
            drawables.recycle();
        }

        MenuItem menuItem = toolbar.getMenu().findItem(R.id.clear_history_item);
        if (menuItem != null) {
            menuItem.setVisible(playbackHistory != null && !playbackHistory.isEmpty());
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_history_item:
                DBWriter.clearPlaybackHistory();
                return true;
            default:
                return false;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        if (playbackHistory == null) {
            return;
        }
        for (FeedItem item : event.items) {
            int pos = FeedItemUtil.indexOfItemWithId(playbackHistory, item.getId());
            if (pos >= 0) {
                loadItems();
                return;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onHistoryUpdated(PlaybackHistoryEvent event) {
        loadItems();
        invalidateOptionsMenu();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerStatusChanged(PlayerStatusEvent event) {
        loadItems();
        invalidateOptionsMenu();
    }

    private void onFragmentLoaded() {
        adapter.notifyDataSetChanged();
        invalidateOptionsMenu();
    }

    private final FeedItemlistAdapter.ItemAccess itemAccess = new FeedItemlistAdapter.ItemAccess() {

        @Override
        public int getCount() {
            return (playbackHistory != null) ? playbackHistory.size() : 0;
        }

        @Override
        public FeedItem getItem(int position) {
            if (playbackHistory != null && 0 <= position && position < playbackHistory.size()) {
                return playbackHistory.get(position);
            } else {
                return null;
            }
        }
    };

    private void loadItems() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(this::loadData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null) {
                        playbackHistory = result;
                        onFragmentLoaded();
                    }
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @NonNull
    private List<FeedItem> loadData() {
        List<FeedItem> history = DBReader.getPlaybackHistory();
        DBReader.loadAdditionalFeedItemListData(history);
        return history;
    }
}
