package cl.monsoon.s1next.view.adapter.delegate;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hannesdorfmann.adapterdelegates.AbsAdapterDelegate;

import java.util.List;

import cl.monsoon.s1next.R;
import cl.monsoon.s1next.view.adapter.item.FooterProgressItem;

public final class PostFooterProgressAdapterDelegate extends AbsAdapterDelegate<List<Object>> {

    private final LayoutInflater mLayoutInflater;

    public PostFooterProgressAdapterDelegate(Activity activity, int viewType) {
        super(viewType);

        mLayoutInflater = activity.getLayoutInflater();
    }

    @Override
    public boolean isForViewType(@NonNull List<Object> items, int position) {
        return items.get(position) instanceof FooterProgressItem;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new FooterProgressViewHolder(mLayoutInflater.inflate(R.layout.item_footer_progress,
                parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull List<Object> items, int position, @NonNull RecyclerView.ViewHolder holder) {}

    private static final class FooterProgressViewHolder extends RecyclerView.ViewHolder {

        public FooterProgressViewHolder(View itemView) {
            super(itemView);
        }
    }
}
