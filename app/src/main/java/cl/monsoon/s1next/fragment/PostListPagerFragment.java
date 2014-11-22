package cl.monsoon.s1next.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import cl.monsoon.s1next.Api;
import cl.monsoon.s1next.Config;
import cl.monsoon.s1next.R;
import cl.monsoon.s1next.activity.ReplyActivity;
import cl.monsoon.s1next.adapter.PostListRecyclerAdapter;
import cl.monsoon.s1next.model.Post;
import cl.monsoon.s1next.model.list.PostList;
import cl.monsoon.s1next.model.mapper.PostListWrapper;
import cl.monsoon.s1next.util.ToastHelper;
import cl.monsoon.s1next.widget.AsyncResult;

/**
 * A Fragment representing one of the pages of posts.
 * All activities containing this Fragment must
 * implement {@link cl.monsoon.s1next.fragment.PostListPagerFragment.OnPagerInteractionCallback}.
 * Similar to {@see ThreadListPagerFragment}
 */
public final class PostListPagerFragment extends AbsNavigationDrawerInteractionFragment<Post, PostListWrapper, PostListRecyclerAdapter.ViewHolder> {

    private static final String ARG_THREAD_ID = "thread_id";
    private static final String ARG_PAGE_NUM = "page_num";

    private CharSequence mThreadId;
    private int mPageNum;

    private MenuItem mMenuReply;

    public static PostListPagerFragment newInstance(CharSequence threadId, int page) {
        PostListPagerFragment fragment = new PostListPagerFragment();

        Bundle args = new Bundle();
        args.putCharSequence(ARG_THREAD_ID, threadId);
        args.putInt(ARG_PAGE_NUM, page);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mThreadId = getArguments().getCharSequence(ARG_THREAD_ID);
        mPageNum = getArguments().getInt(ARG_PAGE_NUM);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int padding = getResources().getDimensionPixelSize(R.dimen.recycler_view_card_padding);
        mRecyclerView.setPadding(0, padding, 0, padding);
    }

    @Override
    public void onResume() {
        super.onResume();

        prepareMenuReply();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.fragment_post, menu);

        // disable default in fragment_post.xml
        // we will enable it when finish loading
        // post list and user has already logged in
        // see PostListPagerFragment#onPostExecute()
        mMenuReply = menu.findItem(R.id.menu_reply);
        prepareMenuReply();
    }

    /**
     * Sets whether the menu reply is enabled depends on whether user logged before.
     */
    private void prepareMenuReply() {
        if (mMenuReply == null) {
            return;
        }

        if (mRecyclerAdapter.getItemCount() == 0 || TextUtils.isEmpty(Config.getUsername())) {
            mMenuReply.setEnabled(false);
        } else {
            mMenuReply.setEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_browser:
                String url = Api.getUrlBrowserPostList(mThreadId, mPageNum);

                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));

                startActivity(intent);

                return true;
            case R.id.menu_reply:
                intent = new Intent(getActivity(), ReplyActivity.class);

                intent.putExtra(ReplyActivity.ARG_THREAD_TITLE, getThreadTitle());
                intent.putExtra(ReplyActivity.ARG_THREAD_ID, mThreadId);

                startActivity(intent);

                return true;
            case R.id.menu_share:
                String value =
                        getThreadTitle() + "  " + Api.getUrlBrowserPostList(mThreadId, 1);

                intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_TEXT, value);
                intent.setType("text/plain");

                startActivity(intent);

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private CharSequence getThreadTitle() {
        CharSequence title = getActivity().getTitle();
        // remove two space and page number's length

        return title.subSequence(0, title.length() - 2 - String.valueOf(mPageNum).length());
    }

    @Override
    protected void initAdapter() {
        super.initAdapter();

        mRecyclerAdapter = new PostListRecyclerAdapter(getActivity());
    }

    @Override
    void load() {
        String url = Api.getUrlPostList(mThreadId, mPageNum);

        executeHttpGet(url, PostListWrapper.class);
    }

    @Override
    public void onPostExecute(AsyncResult<PostListWrapper> asyncResult) {
        super.onPostExecute(asyncResult);

        if (asyncResult.exception != null) {
            if (isVisible()) {
                AsyncResult.handleException(asyncResult.exception);
            }
        } else {
            try {
                PostList postList = asyncResult.data.unwrap();
                mRecyclerAdapter.setDataSet(postList.getPostList());
                mRecyclerAdapter.notifyDataSetChanged();

                prepareMenuReply();

                ((OnPagerInteractionCallback) getActivity())
                        .setCount(postList.getPostListInfo().getReplies() + 1);
            } catch (NullPointerException e) {
                ToastHelper.showByResId(R.string.message_server_error);
            } catch (ClassCastException e) {
                throw new IllegalStateException(
                        getActivity()
                                + " must implement OnPagerInteractionListener.");
            }
        }
    }

    /**
     * A callback interface that all activities containing this Fragment must
     * implement.
     */
    public static interface OnPagerInteractionCallback {

        /**
         * Callback to set actual page which used for {@link android.support.v4.view.PagerAdapter}
         */
        public void setCount(int i);
    }
}
