package de.danoeh.antennapod.fragment;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.OnlineFeedViewActivity;
import de.danoeh.antennapod.adapter.itunes.ItunesAdapter;
import de.danoeh.antennapod.discovery.CombinedSearcher;
import de.danoeh.antennapod.discovery.PodcastSearchResult;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;
import io.reactivex.disposables.Disposable;

import java.util.ArrayList;
import java.util.List;

public class CombinedSearchFragment extends Fragment {

    private static final String TAG = "CombinedSearchFragment";
    public static final String ARGUMENT_QUERY = "query";

    /**
     * Adapter responsible with the search results
     */
    private ItunesAdapter adapter;
    private GridView gridView;
    private ProgressBar progressBar;
    private TextView txtvError;
    private Button butRetry;
    private TextView txtvEmpty;

    /**
     * List of podcasts retreived from the search
     */
    private List<PodcastSearchResult> searchResults = new ArrayList<>();
    private Disposable disposable;
    private Toolbar toolbar;

    /**
     * Constructor
     */
    public CombinedSearchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_itunes_search, container, false);
        gridView = root.findViewById(R.id.gridView);
        adapter = new ItunesAdapter(getActivity(), new ArrayList<>());
        gridView.setAdapter(adapter);

        //Show information about the podcast when the list item is clicked
        gridView.setOnItemClickListener((parent, view1, position, id) -> {
            PodcastSearchResult podcast = searchResults.get(position);
            Intent intent = new Intent(getActivity(), OnlineFeedViewActivity.class);
            intent.putExtra(OnlineFeedViewActivity.ARG_FEEDURL, podcast.feedUrl);
            intent.putExtra(OnlineFeedViewActivity.ARG_TITLE, podcast.title);
            startActivity(intent);
        });
        progressBar = root.findViewById(R.id.progressBar);
        txtvError = root.findViewById(R.id.txtvError);
        butRetry = root.findViewById(R.id.butRetry);
        txtvEmpty = root.findViewById(android.R.id.empty);

        toolbar = root.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());
        toolbar.setTitle(R.string.search_label);
        setupMenu();

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            disposable.dispose();
        }
        adapter = null;
    }

    public void setupMenu() {
        toolbar.inflateMenu(R.menu.itunes_search);
        MenuItem searchItem = toolbar.getMenu().findItem(R.id.action_search);
        final SearchView sv = (SearchView) MenuItemCompat.getActionView(searchItem);
        sv.setQueryHint(getString(R.string.search_label));
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                sv.clearFocus();
                search(s);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                getActivity().getSupportFragmentManager().popBackStack();
                return true;
            }
        });
        MenuItemCompat.expandActionView(searchItem);

        if (getArguments() != null && getArguments().getString(ARGUMENT_QUERY, null) != null) {
            sv.setQuery(getArguments().getString(ARGUMENT_QUERY, null), true);
        }
    }

    private void search(String query) {
        if (disposable != null) {
            disposable.dispose();
        }

        showOnlyProgressBar();

        CombinedSearcher searcher = new CombinedSearcher(getContext());
        disposable = searcher.search(query).subscribe(result -> {
                searchResults = result;
                progressBar.setVisibility(View.GONE);

                adapter.clear();
                adapter.addAll(searchResults);
                adapter.notifyDataSetInvalidated();
                gridView.setVisibility(!searchResults.isEmpty() ? View.VISIBLE : View.GONE);
                txtvEmpty.setVisibility(searchResults.isEmpty() ? View.VISIBLE : View.GONE);

            }, error -> {
                Log.e(TAG, Log.getStackTraceString(error));
                progressBar.setVisibility(View.GONE);
                txtvError.setText(error.toString());
                txtvError.setVisibility(View.VISIBLE);
                butRetry.setOnClickListener(v -> search(query));
                butRetry.setVisibility(View.VISIBLE);
            });
    }

    private void showOnlyProgressBar() {
        gridView.setVisibility(View.GONE);
        txtvError.setVisibility(View.GONE);
        butRetry.setVisibility(View.GONE);
        txtvEmpty.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }
}
