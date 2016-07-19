package cl.monsoon.s1next.view.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.common.base.Preconditions;

import javax.inject.Inject;

import cl.monsoon.s1next.App;
import cl.monsoon.s1next.R;
import cl.monsoon.s1next.data.User;
import cl.monsoon.s1next.data.api.Api;
import cl.monsoon.s1next.data.api.model.Thread;
import cl.monsoon.s1next.data.api.model.ThreadLink;
import cl.monsoon.s1next.data.api.model.collection.Posts;
import cl.monsoon.s1next.data.event.QuoteEvent;
import cl.monsoon.s1next.util.ClipboardUtil;
import cl.monsoon.s1next.util.IntentUtil;
import cl.monsoon.s1next.util.MathUtil;
import cl.monsoon.s1next.util.RxJavaUtil;
import cl.monsoon.s1next.util.StringUtil;
import cl.monsoon.s1next.view.activity.ReplyActivity;
import cl.monsoon.s1next.view.dialog.LoginPromptDialogFragment;
import cl.monsoon.s1next.view.dialog.ThreadAttachmentDialogFragment;
import cl.monsoon.s1next.view.dialog.ThreadFavouritesAddDialogFragment;
import cl.monsoon.s1next.view.internal.CoordinatorLayoutAnchorDelegate;
import cl.monsoon.s1next.widget.EventBus;
import rx.Subscription;


/**
 * A Fragment includes {@link android.support.v4.view.ViewPager}
 * to represent each page of post lists.
 */
public final class PostListFragment extends BaseViewPagerFragment
        implements PostListPagerFragment.PagerCallback, View.OnClickListener {

    public static final String TAG = PostListFragment.class.getName();

    private static final String ARG_THREAD = "thread";
    private static final String ARG_SHOULD_GO_TO_LAST_PAGE = "should_go_to_last_page";

    /**
     * ARG_JUMP_PAGE takes precedence over {@link #ARG_SHOULD_GO_TO_LAST_PAGE}.
     */
    private static final String ARG_JUMP_PAGE = "jump_page";
    private static final String ARG_QUOTE_POST_ID = "quote_post_id";

    @Inject
    EventBus mEventBus;

    @Inject
    User mUser;

    private String mThreadId;
    @Nullable
    private String mThreadTitle;

    private Posts.ThreadAttachment mThreadAttachment;
    private MenuItem mMenuThreadAttachment;

    private Subscription mSubscription;

    public static PostListFragment newInstance(Thread thread, boolean shouldGoToLastPage) {
        PostListFragment fragment = new PostListFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_THREAD, thread);
        bundle.putBoolean(ARG_SHOULD_GO_TO_LAST_PAGE, shouldGoToLastPage);
        fragment.setArguments(bundle);

        return fragment;
    }

    public static PostListFragment newInstance(ThreadLink threadLink) {
        Thread thread = new Thread();
        thread.setId(threadLink.getThreadId());

        PostListFragment fragment = new PostListFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_THREAD, thread);
        bundle.putInt(ARG_JUMP_PAGE, threadLink.getJumpPage());
        if (threadLink.getQuotePostId().isPresent()) {
            bundle.putString(ARG_QUOTE_POST_ID, threadLink.getQuotePostId().get());
        }
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        App.getAppComponent(getContext()).inject(this);

        Bundle bundle = getArguments();
        Thread thread = Preconditions.checkNotNull(bundle.getParcelable(ARG_THREAD));
        // thread title is null if this thread comes from ThreadLink
        mThreadTitle = thread.getTitle();
        mThreadId = thread.getId();

        if (savedInstanceState == null) {
            final int jumpPage = bundle.getInt(ARG_JUMP_PAGE, 0);
            if (jumpPage != 0) {
                // we do not know the total page if we open this thread by URL
                // so we set the jump page to total page
                setTotalPages(jumpPage);
                setCurrentPage(jumpPage - 1);
            } else {
                // +1 for original post
                setTotalPageByPosts(thread.getReplies() + 1);
                if (bundle.getBoolean(ARG_SHOULD_GO_TO_LAST_PAGE, false)) {
                    setCurrentPage(getTotalPages() - 1);
                }
            }
        }

        ((CoordinatorLayoutAnchorDelegate) getActivity()).setupFloatingActionButton(
                R.drawable.ic_insert_comment_white_24dp, this);
    }

    @Override
    public void onResume() {
        super.onResume();

        mSubscription = mEventBus.get().subscribe(o -> {
            if (o instanceof QuoteEvent) {
                QuoteEvent quoteEvent = (QuoteEvent) o;
                startReplyActivity(quoteEvent.getQuotePostId(), quoteEvent.getQuotePostCount());
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        RxJavaUtil.unsubscribeIfNotNull(mSubscription);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_post, menu);

        mMenuThreadAttachment = menu.findItem(R.id.menu_thread_attachment);
        if (mThreadAttachment == null) {
            mMenuThreadAttachment.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_thread_attachment:
                ThreadAttachmentDialogFragment.newInstance(mThreadAttachment).show(
                        getActivity().getSupportFragmentManager(),
                        ThreadAttachmentDialogFragment.TAG);

                return true;
            case R.id.menu_favourites_add:
                if (!LoginPromptDialogFragment.showLoginPromptDialogIfNeeded(getActivity(), mUser)) {
                    ThreadFavouritesAddDialogFragment.newInstance(mThreadId).show(
                            getActivity().getSupportFragmentManager(),
                            ThreadFavouritesAddDialogFragment.TAG);
                }

                return true;
            case R.id.menu_link:
                ClipboardUtil.copyText(getContext(), Api.getPostListUrlForBrowser(mThreadId,
                        getCurrentPage()));
                ((CoordinatorLayoutAnchorDelegate) getActivity()).showShortSnackbar(
                        R.string.message_thread_link_copy);

                return true;
            case R.id.menu_share:
                String value;
                String url = Api.getPostListUrlForBrowser(mThreadId, getCurrentPage());
                if (TextUtils.isEmpty(mThreadTitle)) {
                    value = url;
                } else {
                    value = StringUtil.concatWithTwoSpaces(mThreadTitle, url);
                }

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_TEXT, value);
                intent.setType("text/plain");

                startActivity(Intent.createChooser(intent, getString(R.string.menu_title_share)));

                return true;
            case R.id.menu_browser:
                IntentUtil.startViewIntentExcludeOurApp(getContext(), Uri.parse(
                        Api.getPostListUrlForBrowser(mThreadId, getCurrentPage() + 1)));

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    BaseFragmentStatePagerAdapter getPagerAdapter(FragmentManager fragmentManager) {
        return new PostListPagerAdapter(fragmentManager);
    }

    @Nullable
    @Override
    CharSequence getTitleWithoutPosition() {
        return mThreadTitle;
    }

    @Override
    public void setTotalPageByPosts(int threads) {
        setTotalPages(MathUtil.divide(threads, Api.POSTS_PER_PAGE));
    }

    @Override
    public void setThreadTitle(CharSequence title) {
        Thread thread = Preconditions.checkNotNull(getArguments().getParcelable(ARG_THREAD));
        thread.setTitle(title.toString());
        mThreadTitle = thread.getTitle();
        setTitleWithPosition(getCurrentPage());
    }

    @Override
    public void setupThreadAttachment(Posts.ThreadAttachment threadAttachment) {
        this.mThreadAttachment = threadAttachment;

        // mMenuThreadAttachment = null when configuration changes (like orientation changes)
        // but we don't need to care about the visibility of mMenuThreadAttachment
        // because mThreadAttachment != null and we won't invoke
        // mMenuThreadAttachment.setVisible(false) during onCreateOptionsMenu(Menu)
        if (mMenuThreadAttachment != null) {
            mMenuThreadAttachment.setVisible(true);
        }
    }

    @Override
    public void onClick(View v) {
        startReplyActivity(null, null);
    }

    private void startReplyActivity(@Nullable String quotePostId, @Nullable String quotePostCount) {
        if (LoginPromptDialogFragment.showLoginPromptDialogIfNeeded(getActivity(), mUser)) {
            return;
        }

        ReplyActivity.startReplyActivityForResultMessage(getActivity(), mThreadId, mThreadTitle,
                quotePostId, quotePostCount);
    }

    /**
     * Returns a Fragment corresponding to one of the pages of posts.
     */
    private class PostListPagerAdapter extends BaseFragmentStatePagerAdapter {

        private PostListPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Bundle bundle = getArguments();
            int jumpPage = bundle.getInt(ARG_JUMP_PAGE, -1);
            String quotePostId = bundle.getString(ARG_QUOTE_POST_ID);
            if (jumpPage == i + 1 && !TextUtils.isEmpty(quotePostId)) {
                // clear this arg string because we only need to tell PostListPagerFragment once
                bundle.putString(ARG_QUOTE_POST_ID, null);
                return PostListPagerFragment.newInstance(mThreadId, jumpPage, quotePostId);
            } else {
                return PostListPagerFragment.newInstance(mThreadId, i + 1);
            }
        }
    }
}
