package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.view.PieChartView;

/**
 * Adapter for the statistics list
 */
public class DownloadStatisticsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_FEED = 1;
    private final Context context;
    private DBReader.StatisticsData statisticsData;

    public DownloadStatisticsListAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getItemCount() {
        return statisticsData.feedTime.size() + 1;
    }

    public DBReader.StatisticsItem getItem(int position) {
        if (position == 0) {
            return null;
        }
        return statisticsData.feedTime.get(position - 1);
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_HEADER : TYPE_FEED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.download_statistics_listitem_total_size, parent, false));
        }
        return new StatisticsHolder(inflater.inflate(R.layout.download_statistics_listitem, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
        if (getItemViewType(position) == TYPE_HEADER) {
            HeaderHolder holder = (HeaderHolder) h;
            long totalDownloadSize = 0;

            for (DBReader.StatisticsItem item: statisticsData.feedTime) {
                totalDownloadSize = totalDownloadSize + item.totalDownloadSize;
            }
            holder.totalTime.setText(Converter.byteToString(totalDownloadSize));
            float[] dataValues = new float[statisticsData.feedTime.size()];
            for (int i = 0; i < statisticsData.feedTime.size(); i++) {
                DBReader.StatisticsItem item = statisticsData.feedTime.get(i);
                dataValues[i] = item.totalDownloadSize;
            }
            holder.pieChart.setData(dataValues);
        } else {
            StatisticsHolder holder = (StatisticsHolder) h;
            DBReader.StatisticsItem statsItem = statisticsData.feedTime.get(position - 1);
            Glide.with(context)
                    .load(statsItem.feed.getImageLocation())
                    .apply(new RequestOptions()
                            .placeholder(R.color.light_gray)
                            .error(R.color.light_gray)
                            .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                            .fitCenter()
                            .dontAnimate())
                    .into(holder.image);

            holder.title.setText(statsItem.feed.getTitle());
            holder.size.setText(Converter.byteToString(statsItem.totalDownloadSize));
        }
    }

    public void update(DBReader.StatisticsData statistics) {
        this.statisticsData = statistics;
        notifyDataSetChanged();
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        TextView totalTime;
        PieChartView pieChart;

        HeaderHolder(View itemView) {
            super(itemView);
            totalTime = itemView.findViewById(R.id.total_time);
            pieChart = itemView.findViewById(R.id.pie_chart);
        }
    }

    static class StatisticsHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;
        TextView size;

        StatisticsHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imgvCover);
            title = itemView.findViewById(R.id.txtvTitle);
            size = itemView.findViewById(R.id.txtvSize);
        }
    }

}
