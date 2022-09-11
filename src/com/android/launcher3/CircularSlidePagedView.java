package com.android.launcher3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.effect.TransitionEffect;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.pageindicators.PageIndicator;
import com.android.launcher3.settings.MxSettings;
import com.android.launcher3.touch.OverScroll;
import com.android.launcher3.touch.PagedOrientationHandler;
import com.android.mxlibrary.util.MxHandler;
import com.android.mxlibrary.util.XLog;

/**
 * Created by yuchuan
 * DATE 2022/9/11
 * TIME 10:21
 */
public class CircularSlidePagedView<T extends View & PageIndicator> extends PagedView<T> {

    private static final String TAG = "CircularSlidePagedView";

    private static final int FLING_THRESHOLD_VELOCITY = 500;
    private static final int MIN_SNAP_VELOCITY = 1500;
    private static final int MIN_FLING_VELOCITY = 250;

    private static final boolean DEBUG = true;

    protected static final int OVER_FIRST_PAGE_INDEX = -1;
    private static final int OVER_FIRST_INDEX = 0;
    private static final int OVER_LAST_INDEX = 1;
    private int[] mOverPageLeft = new int[]{0, 0};

    private Rect mViewport = new Rect();
    protected int mPageWidth = 0;
    protected int mFlingThresholdVelocity;
    protected int mMinFlingVelocity;// 最小惯性速度
    protected int mMinSnapVelocity;

    protected boolean mWasInOverscroll = false;

    /**
     * mOverScrollX is equal to getScrollX() when we're within the normal scroll range.
     * Otherwise(否则) it is equal to the scaled overscroll position. We use a separate
     * value so as to prevent the screens from continuing to translate beyond the normal bounds.
     * <p>
     * getScrollX():表示PageView左侧边缘位置从屏幕左侧边缘位置（Y轴）滑动到当前位置的滑动变量，
     * 如果View左侧边缘从屏幕左侧边缘移动到了屏幕左侧，getScrollX为View左侧边缘到屏幕边缘距离；
     * 如果View左侧边缘从屏幕左侧边缘移动到了屏幕右侧，getScrollX为View左侧边缘到屏幕边缘距离的负值
     * 例如：View在左侧边缘与屏幕左侧边缘重合，那么getScrollX=0（屏幕宽度假设720）
     * View左侧边缘到屏幕边缘的距离|getScrollX值
     * -2260                 2260
     * -1440                 1440
     * -720                  720
     * 0                       0
     * 720                   -720
     * 1440                  -1440
     * 2260                  -2260
     * 距离为负值，说明View的左侧边缘在屏幕左侧边缘的左侧，反之在右侧
     * (PageView向左滑动为正方向，getScrollX为正值；反之负方向，getScrollX为负值)
     */
    protected int mOverScrollX;

    protected int mUnboundedScrollX;

    public static float mDensity;

    public CircularSlidePagedView(Context context) {
        this(context, null);
    }

    public CircularSlidePagedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircularSlidePagedView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mDensity = getResources().getDisplayMetrics().density;
        mFlingThresholdVelocity = (int) (FLING_THRESHOLD_VELOCITY * mDensity);
        mMinFlingVelocity = (int) (MIN_FLING_VELOCITY * mDensity);
        mMinSnapVelocity = (int) (MIN_SNAP_VELOCITY * mDensity);
    }

    protected boolean getPageScrolls(int[] outPageScrolls, boolean layoutChildren,
                                     ComputePageScrollsLogic scrollLogic) {
        final int childCount = getChildCount();

        int offsetX = getViewportOffsetX();
        int offsetY = getViewportOffsetY();
        // Update the viewport offsets
        mViewport.offset(offsetX, offsetY);

        final int startIndex = mIsRtl ? childCount - 1 : 0;
        final int endIndex = mIsRtl ? -1 : childCount;
        final int delta = mIsRtl ? -1 : 1;

        final int pageCenter = mOrientationHandler.getCenterForPage(this, mInsets);

        final int scrollOffsetStart = mOrientationHandler.getScrollOffsetStart(this, mInsets);
        final int scrollOffsetEnd = mOrientationHandler.getScrollOffsetEnd(this, mInsets);
        boolean pageScrollChanged = false;
        int panelCount = getPanelCount();

        for (int i = startIndex, childStart = scrollOffsetStart; i != endIndex; i += delta) {
            final View child = getPageAt(i);
            if (scrollLogic.shouldIncludeView(child)) {
                PagedOrientationHandler.ChildBounds bounds = mOrientationHandler.getChildBounds(child, childStart,
                        pageCenter, layoutChildren);
                final int primaryDimension = bounds.primaryDimension;
                final int childPrimaryEnd = bounds.childPrimaryEnd;

                // In case the pages are of different width, align the page to left edge for non-RTL
                // or right edge for RTL.
                final int pageScroll =
                        mIsRtl ? childPrimaryEnd - scrollOffsetEnd : childStart - scrollOffsetStart;
                if (outPageScrolls[i] != pageScroll) {
                    pageScrollChanged = true;
                    outPageScrolls[i] = pageScroll;
                }
                childStart += primaryDimension + getChildGap(i, i + delta);

                // This makes sure that the space is added after the page, not after each panel
                int lastPanel = mIsRtl ? 0 : panelCount - 1;
                if (i % panelCount == lastPanel) {
                    childStart += mPageSpacing;
                }
            }
        }

        if (panelCount > 1) {
            for (int i = 0; i < childCount; i++) {
                // In case we have multiple panels, always use left most panel's page scroll for all
                // panels on the screen.
                int adjustedScroll = outPageScrolls[getLeftmostVisiblePageForIndex(i)];
                if (outPageScrolls[i] != adjustedScroll) {
                    outPageScrolls[i] = adjustedScroll;
                    pageScrollChanged = true;
                }
            }
        }
        return pageScrollChanged;
    }

    // Convenience methods to get the actual width/height of the PagedView (since it is measured
    // to be larger to account for the minimum possible scale)
    int getViewportWidth() {
        return mViewport.width();
    }

    public int getViewportHeight() {
        return mViewport.height();
    }

    // Convenience methods to get the offset ASSUMING that we are centering the pages in the
    // PagedView both horizontally and vertically
    int getViewportOffsetX() {
        return (getMeasuredWidth() - getViewportWidth()) / 2;
    }

    int getViewportOffsetY() {
        return (getMeasuredHeight() - getViewportHeight()) / 2;
    }

    /**
     * Returns the index of page to be shown immediately afterwards.
     */
    @Override
    public int getNextPage() {
        int nextPage = mNextPage;
        if (enableLoop()) {
            if (mNextPage == OVER_FIRST_PAGE_INDEX) {
                nextPage = getChildCount() - 1;
            } else if (mNextPage == getChildCount()) {
                nextPage = 0;
            }
        }
        return (mNextPage != INVALID_PAGE) ? nextPage : mCurrentPage;
    }

    @Override
    protected int validateNewPage(int newPage) {
        int validatedPage = newPage;
        if (enableLoop()) {
            validatedPage = Math.max(OVER_FIRST_PAGE_INDEX, Math.min(validatedPage, getPageCount()));
        } else {
            // Ensure that it is clamped by the actual set of children in all cases
            validatedPage = super.validateNewPage(newPage);
        }
        return validatedPage;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (mPageScrolls != null && mPageScrolls.length > 1) {
            mPageWidth = Math.abs(mPageScrolls[1] - mPageScrolls[0]);
            mOverPageLeft[(mIsRtl ? OVER_LAST_INDEX : OVER_FIRST_INDEX)] = -mPageWidth;
            mOverPageLeft[(mIsRtl ? OVER_FIRST_INDEX : OVER_LAST_INDEX)] = mPageScrolls[mIsRtl ?
                    0 : (mPageScrolls.length - 1)] + mPageWidth;
        }
        if (DEBUG) {
            XLog.d(TAG, "onLayout mPageWidth:" + mPageWidth
                    + " mOverPageLeft[0]:" + mOverPageLeft[0]
                    + " mOverPageLeft[1]:" + mOverPageLeft[1]);
        }
    }

    // SPRD: add for circular sliding.
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (enableLoop()) {
            drawCircularPageIfNeed(canvas);
        }
    }

    @Override
    public int getScrollForPage(int index) {
        if (enableLoop()) {
            if (index == OVER_FIRST_PAGE_INDEX) {
                return mOverPageLeft[OVER_FIRST_INDEX];
            } else if (index == getChildCount()) {
                return mOverPageLeft[OVER_LAST_INDEX];
            }
        }
        return super.getScrollForPage(index);
    }

    @Override
    public void scrollBy(int x, int y) {
        scrollTo(mUnboundedScrollX + x, getScrollY() + y);
    }

    @Override
    public void scrollTo(int x, int y) {
        if (enableLoop()) {
            // In free scroll mode, we clamp the scrollX
            if (mFreeScroll) {
                // If the scroller is trying to move to a location beyond the maximum allowed
                // in the free scroll mode, we make sure to end the scroll operation.
                if (!mScroller.isFinished() && (x > mMaxScroll || x < 0)) {
                    forceFinishScroller();
                }

                x = Utilities.boundToRange(x, 0, mMaxScroll);
            }

            mUnboundedScrollX = x;

            boolean isXBeforeFirstPage = isXBeforeFirstPage(x);
            boolean isXAfterLastPage = isXAfterLastPage(x);
            if (isXBeforeFirstPage) {
                super.scrollTo(mIsRtl ? mMaxScroll : 0, y);
                if (mAllowOverScroll) {
                    mWasInOverscroll = true;
                    if (mIsRtl) {
                        overScroll(x - mMaxScroll);
                    } else {
                        overScroll(x);
                    }
                }
            } else if (isXAfterLastPage) {
                super.scrollTo(mIsRtl ? 0 : mMaxScroll, y);
                if (mAllowOverScroll) {
                    mWasInOverscroll = true;
                    if (mIsRtl) {
                        overScroll(x);
                    } else {
                        overScroll(x - mMaxScroll);
                    }
                }
            } else {
                if (mWasInOverscroll) {
                    overScroll(0);
                    mWasInOverscroll = false;
                }
                mOverScrollX = x;
                super.scrollTo(x, y);
            }
        } else {
            super.scrollTo(x, y);
        }
    }

    protected void overScroll(float amount) {
        dampedOverScroll(amount);
    }

    protected boolean isInOverScroll() {
        return (mOverScrollX > mMaxScroll || mOverScrollX < 0);
    }

    protected void dampedOverScroll(float amount) {
        if (Float.compare(amount, 0f) == 0) return;

        int overScrollAmount = OverScroll.dampedScroll(amount, getMeasuredWidth());
        if (amount < 0) {
            mOverScrollX = overScrollAmount;
            super.scrollTo(mOverScrollX, getScrollY());
        } else {
            mOverScrollX = mMaxScroll + overScrollAmount;
            super.scrollTo(mOverScrollX, getScrollY());
        }
        invalidate();
    }

    // X在第一页之前，表示从第一页循环到最后一页
    protected boolean isXBeforeFirstPage(int x) {
        return mIsRtl ? (x > mMaxScroll) : (x < 0);
    }

    // X在最后一页之后，表示从最后一页循环到第一页
    protected boolean isXAfterLastPage(int x) {
        return mIsRtl ? (x < 0) : (x > mMaxScroll);
    }

    protected int getMinPageIndex() {
        return enableLoop() ? OVER_FIRST_PAGE_INDEX : 0;
    }

    protected int getMaxPageIndex() {
        return enableLoop() ? getChildCount() : (getChildCount() - 1);
    }

    public boolean enableLoop() {
        //Can't circular slide when there is only one page
        boolean multiPage = mPageScrolls != null && mPageScrolls.length > 1;
        boolean enableLoop = isPagedViewCircledScroll() && multiPage && isPageInTransition();
        XLog.d(XLog.getTag(), XLog.TAG_GU + enableLoop);
        return enableLoop;
    }

    protected int validateCircularNewPage() {
        int currentPage;
        if (enableLoop()) {
            if (mNextPage == OVER_FIRST_PAGE_INDEX && mPageScrolls != null) {
                currentPage = getPageCount() - 1;
            } else if (mNextPage == getPageCount() && mPageScrolls != null) {
                currentPage = 0;
            } else {
                currentPage = validateNewPage(mNextPage);
            }
            scrollTo(mPageScrolls[currentPage], getScrollY());
        } else {
            currentPage = validateCircularNewPage();
        }
        if (DEBUG) {
            XLog.d(TAG, "validateCircularNewPage currentPage:" + currentPage);
        }
        return currentPage;
    }

    /**
     * 影响特效存在情况下循环滑动时最后一页与第一页切换时特效不对的问题
     *
     * @param scrollX getScrollX
     * @param v       当前页面
     * @param page    当前页面对应下标
     * @return 滑动进度
     */
    @Override
    public float getScrollProgress(int scrollX, View v, int page) {
        if (enableLoop()) {
            final int halfScreenSize = getMeasuredWidth() / 2;
            int delta = scrollX - (getScrollForPage(page) + halfScreenSize);

            final int totalDistance;
            // 滑动过程中正在进入的页面下标
            int adjacentPage = page + 1;
            if ((delta < 0 && !mIsRtl) || (delta > 0 && mIsRtl)) {
                adjacentPage = page - 1;
            }

            // 计算一个页面从开始到结束（一个页面切换过程）走过的完整距离
            totalDistance = computeTotalDistance(v, adjacentPage, page);
            delta = reComputeDelta(delta, scrollX, page, totalDistance);

            // 范围：[-1,1]
            float scrollProgress = delta / (totalDistance * 1.0f);
            scrollProgress = Math.min(scrollProgress, MAX_SCROLL_PROGRESS);
            scrollProgress = Math.max(scrollProgress, -MAX_SCROLL_PROGRESS);
            return scrollProgress;
        } else {
            return super.getScrollProgress(scrollX, v, page);
        }
    }

    protected int computeTotalDistance(View v, int adjacentPage, int page) {
        int totalDistance;
        if (enableLoop() && (adjacentPage == -1 || adjacentPage == getChildCount())) {
            totalDistance = Math.abs(getScrollForPage(adjacentPage) - getScrollForPage(page));
        } else {
            // 正在进入页面左边缘到正在退出页面左边缘的距离
            if (adjacentPage < 0 || adjacentPage > getChildCount() - 1) {
                totalDistance = v.getMeasuredWidth();
            } else {

                totalDistance = Math.abs(getScrollForPage(adjacentPage) - getScrollForPage(page));
            }
        }
        return totalDistance;
    }

    protected int reComputeDelta(int delta, int screenCenter, int page, int totalDistance) {
        int index = 0;
        final int halfScreenSize = getMeasuredWidth() / 2;
        if (enableLoop()) {// 循环滑动
            if (mIsRtl) {// 反向排列
                if (mOverScrollX < 0 && page == 0) {
                    index = getChildCount();
                } else if (mOverScrollX > mMaxScroll && page == getChildCount() - 1) {
                    index = OVER_FIRST_PAGE_INDEX;
                }
            } else {// 从左向右排列
                // 从最后一屏继续向左滑动Workspace，此时应该循环到第一屏
                if (mOverScrollX > mMaxScroll && page == 0) {
                    index = getChildCount();
                } else if (mOverScrollX < 0 && page == getChildCount() - 1) {// 从第一屏继续向右滑动，应该循环到最后一屏
                    index = OVER_FIRST_PAGE_INDEX;
                }
            }
        }
        return (index == 0) ? delta : (screenCenter - (getScrollForPage(index) + halfScreenSize));
    }

    private void drawCircularPageIfNeed(Canvas canvas) {
        // X在第一页之前，表示从第一页循环到最后一页
        boolean isXBeforeFirstPage = mIsRtl ? (mOverScrollX > mMaxScroll) : (mOverScrollX < 0);
        // X在最后一页之后，表示从从最后一页循环到第一页
        boolean isXAfterLastPage = mIsRtl ? (mOverScrollX < 0) : (mOverScrollX > mMaxScroll);
        if (isXBeforeFirstPage || isXAfterLastPage) {
            long drawingTime = getDrawingTime();
            int childCount = getChildCount();
            canvas.save();
            canvas.clipRect(getScrollX(), getScrollY(), getScrollX() + getRight() - getLeft(),
                    getScrollY() + getBottom() - getTop());
            // here we assume that a page's horizontal padding plus it's measured width
            // equals to ViewPort's width
            // 偏移量是所有页面的宽度之和
            int offset = (mIsRtl ? -childCount : childCount) * (mPageWidth);
            if (isXBeforeFirstPage) {
                // 偏移画布
                canvas.translate(-offset, 0);
                // 从第一页循环到最后一页的过程中，画布中左侧部分会留出空白，此时，为了让我们看似是循环滑动，
                // 就要把最后一页的一部分绘制到画布的空白部分上，一边滑动一边绘制，这样最后一页就是看似是一个
                // 渐显的过程，在绘制之前要先移动画布到对应最后一页的位置
                drawChild(canvas, getPageAt(childCount - 1), drawingTime);
                // 还原画布位置
                canvas.translate(+offset, 0);
            } else {
                // 偏移画布
                canvas.translate(+offset, 0);
                // 类似上面的从第一页到最后一页的过程
                drawChild(canvas, getPageAt(0), drawingTime);
                // 还原画布
                canvas.translate(-offset, 0);
            }
            canvas.restore();
        }
    }

    private boolean isPagedViewCircledScroll() {
        return true;
//        return MxSettings.getInstance().isPageViewCircleScroll();
    }

    protected TransitionEffect mTransitionEffect;

    public TransitionEffect getTransitionEffect() {
        return mTransitionEffect;
    }

    private void startScrollWithAnim(int screenScroll) {
        // -1,7,15,1,6,4
        int screenEffectNum = MxSettings.sLauncherEffect;
        mTransitionEffect.screenScrollByTransitionEffect(screenScroll, screenEffectNum);
    }

    private static final int WORKSPACE_MSG_PREVIEW_EFFECT = 100;
    private static final int WORKSPACE_MSG_BACK_EFFECT = 101;
    protected static final int SLOW_PAGE_SNAP_ANIMATION_DURATION = 950;

//    public void previewTransitionEffect(MenuItem effect, MenuEffectController controller) {
//        if (!mScroller.isFinished() || getChildCount() < 2) {
//            return;
//        }
//        XLog.d(XLog.getTag(), XLog.TAG_GU_STATE + effect);
//        MxSettings.getInstance().setLauncherEffect(effect.getPosition());
//        controller.getAdapter().setSelected(effect);
//        mTransitionEffectHandler.removeMessages(WORKSPACE_MSG_PREVIEW_EFFECT);
//        Message msg = mTransitionEffectHandler.obtainMessage();
//        msg.what = WORKSPACE_MSG_PREVIEW_EFFECT;
//        mTransitionEffectHandler.sendMessage(msg);
//    }

    private TransitionEffectHandler mTransitionEffectHandler;

    private static class TransitionEffectHandler extends MxHandler<Workspace> {

        TransitionEffectHandler(Workspace workspace) {
            super(workspace);
        }

        @Override
        protected void handleMessage(Workspace workspace, Message msg) {
            switch (msg.what) {
                case WORKSPACE_MSG_PREVIEW_EFFECT:
                    workspace.mTransitionEffect.clearTransitionEffect();
                    int previewPage;
                    if (workspace.getNextPage() == workspace.getChildCount() - 1) {
                        previewPage = workspace.getChildCount() - 2;
                    } else {
                        previewPage = workspace.getNextPage() + 1;
                    }
                    workspace.snapToPage(previewPage);
                    sendEmptyMessageDelayed(WORKSPACE_MSG_BACK_EFFECT, SLOW_PAGE_SNAP_ANIMATION_DURATION);
                    break;
                case WORKSPACE_MSG_BACK_EFFECT:
                    final int backPage = workspace.getNextPage();
                    workspace.snapToPage(backPage);
                    break;
                default:
                    break;
            }
        }

    }

    /**
     * @return The open folder on the current screen, or null if there is none
     */
    public Folder getOpenFolder() {
        DragLayer dragLayer = Launcher.getLauncher(getContext()).getDragLayer();
        // Iterate in reverse order. Folder is added later to the dragLayer,
        // and will be one of the last views.
        for (int i = dragLayer.getChildCount() - 1; i >= 0; i--) {
            View child = dragLayer.getChildAt(i);
            if (child instanceof Folder) {
                Folder folder = (Folder) child;
                if (folder.isOpen())
                    return folder;
            }
        }
        return null;
    }

    private float mOverviewModeShrinkFactor;

    // 计算需要移动的距离
    public int getOverviewModeTranslationY() {
//        DeviceProfile grid = mLauncher.getDeviceProfile();
//        int overviewButtonBarHeight = grid.getOverviewModeButtonBarHeight();
//
//        int scaledHeight = (int) (mOverviewModeShrinkFactor * getNormalChildHeight());
//        Rect workspacePadding = grid.getWorkspacePadding(sTempRect);
//        int workspaceTop = mInsets.top + workspacePadding.top;
//        int workspaceBottom = getViewportHeight() - mInsets.bottom - workspacePadding.bottom;
//        int overviewTop = mInsets.top;
//        int overviewBottom = getViewportHeight() - mInsets.bottom - overviewButtonBarHeight;
//        int workspaceOffsetTopEdge = workspaceTop + ((workspaceBottom - workspaceTop) - scaledHeight) / 2;
//        int overviewOffsetTopEdge = overviewTop + (overviewBottom - overviewTop - scaledHeight) / 2;
//        return -workspaceOffsetTopEdge + overviewOffsetTopEdge;
        return 0;
    }

}
