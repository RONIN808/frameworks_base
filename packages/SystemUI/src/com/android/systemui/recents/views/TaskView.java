/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewOutlineProvider;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsActivity;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.LaunchTaskEvent;
import com.android.systemui.recents.events.ui.DismissTaskViewEvent;
import com.android.systemui.recents.events.ui.TaskViewDismissedEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragStartEvent;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;

import java.util.ArrayList;

import static android.app.ActivityManager.StackId.INVALID_STACK_ID;

/**
 * A {@link TaskView} represents a fixed view of a task. Because the TaskView's layout is directed
 * solely by the {@link TaskStackView}, we make it a fixed size layout which allows relayouts down
 * the view hierarchy, but not upwards from any of its children (the TaskView will relayout itself
 * with the previous bounds if any child requests layout).
 */
public class TaskView extends FixedSizeFrameLayout implements Task.TaskCallbacks,
        TaskStackAnimationHelper.Callbacks, View.OnClickListener, View.OnLongClickListener {

    /** The TaskView callbacks */
    interface TaskViewCallbacks {
        void onTaskViewClipStateChanged(TaskView tv);
    }

    /**
     * The dim overlay is generally calculated from the task progress, but occasionally (like when
     * launching) needs to be animated independently of the task progress.  This call is only used
     * when animating the task into Recents, when the header dim is already applied
     */
    public static final Property<TaskView, Float> DIM_ALPHA_WITHOUT_HEADER =
            new FloatProperty<TaskView>("dimAlphaWithoutHeader") {
                @Override
                public void setValue(TaskView tv, float dimAlpha) {
                    tv.setDimAlphaWithoutHeader(dimAlpha);
                }

                @Override
                public Float get(TaskView tv) {
                    return tv.getDimAlpha();
                }
            };

    /**
     * The dim overlay is generally calculated from the task progress, but occasionally (like when
     * launching) needs to be animated independently of the task progress.
     */
    public static final Property<TaskView, Float> DIM_ALPHA =
            new FloatProperty<TaskView>("dimAlpha") {
                @Override
                public void setValue(TaskView tv, float dimAlpha) {
                    tv.setDimAlpha(dimAlpha);
                }

                @Override
                public Float get(TaskView tv) {
                    return tv.getDimAlpha();
                }
            };

    /**
     * The dim overlay is generally calculated from the task progress, but occasionally (like when
     * launching) needs to be animated independently of the task progress.
     */
    public static final Property<TaskView, Float> VIEW_OUTLINE_ALPHA =
            new FloatProperty<TaskView>("viewOutlineAlpha") {
                @Override
                public void setValue(TaskView tv, float alpha) {
                    tv.getViewBounds().setAlpha(alpha);
                }

                @Override
                public Float get(TaskView tv) {
                    return tv.getViewBounds().getAlpha();
                }
            };

    @ViewDebug.ExportedProperty(category="recents")
    float mDimAlpha;
    PorterDuffColorFilter mDimColorFilter = new PorterDuffColorFilter(0, PorterDuff.Mode.SRC_ATOP);
    Paint mDimLayerPaint = new Paint();
    float mActionButtonTranslationZ;

    @ViewDebug.ExportedProperty(deepExport=true, prefix="task_")
    Task mTask;
    @ViewDebug.ExportedProperty(category="recents")
    boolean mTaskDataLoaded;
    @ViewDebug.ExportedProperty(category="recents")
    boolean mClipViewInStack = true;
    @ViewDebug.ExportedProperty(category="recents")
    boolean mTouchExplorationEnabled;
    @ViewDebug.ExportedProperty(category="recents")
    boolean mIsDisabledInSafeMode;
    @ViewDebug.ExportedProperty(deepExport=true, prefix="view_bounds_")
    AnimateableViewBounds mViewBounds;

    private AnimatorSet mTransformAnimation;
    private final TaskViewTransform mTargetAnimationTransform = new TaskViewTransform();
    private ArrayList<Animator> mTmpAnimators = new ArrayList<>();

    View mContent;
    @ViewDebug.ExportedProperty(deepExport=true, prefix="thumbnail_")
    TaskViewThumbnail mThumbnailView;
    @ViewDebug.ExportedProperty(deepExport=true, prefix="header_")
    TaskViewHeader mHeaderView;
    View mActionButtonView;
    TaskViewCallbacks mCb;

    @ViewDebug.ExportedProperty(category="recents")
    Point mDownTouchPos = new Point();

    private Toast mDisabledAppToast;

    public TaskView(Context context) {
        this(context, null);
    }

    public TaskView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TaskView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TaskView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        RecentsConfiguration config = Recents.getConfiguration();
        Resources res = context.getResources();
        mViewBounds = new AnimateableViewBounds(this, res.getDimensionPixelSize(
                R.dimen.recents_task_view_shadow_rounded_corners_radius));
        if (config.fakeShadows) {
            setBackground(new FakeShadowDrawable(res, config));
        }
        setOutlineProvider(mViewBounds);
        setOnLongClickListener(this);
    }

    /** Set callback */
    void setCallbacks(TaskViewCallbacks cb) {
        mCb = cb;
    }

    /** Resets this TaskView for reuse. */
    void onResume(boolean isResumingFromVisible) {
        resetNoUserInteractionState();
        readSystemFlags();
        if (!isResumingFromVisible) {
            resetViewProperties();
            setClipViewInStack(false);
        }
        setCallbacks(null);
    }

    /** Gets the task */
    public Task getTask() {
        return mTask;
    }

    /** Returns the view bounds. */
    AnimateableViewBounds getViewBounds() {
        return mViewBounds;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        readSystemFlags();
    }

    @Override
    protected void onFinishInflate() {
        // Bind the views
        mContent = findViewById(R.id.task_view_content);
        mHeaderView = (TaskViewHeader) findViewById(R.id.task_view_bar);
        mThumbnailView = (TaskViewThumbnail) findViewById(R.id.task_view_thumbnail);
        mActionButtonView = findViewById(R.id.lock_to_app_fab);
        mActionButtonView.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                // Set the outline to match the FAB background
                outline.setOval(0, 0, mActionButtonView.getWidth(), mActionButtonView.getHeight());
                outline.setAlpha(0.35f);
            }
        });
        mActionButtonView.setOnClickListener(this);
        mActionButtonTranslationZ = mActionButtonView.getTranslationZ();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            mHeaderView.onTaskViewSizeChanged(w, h);
            mThumbnailView.onTaskViewSizeChanged(w, h);
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mDownTouchPos.set((int) (ev.getX() * getScaleX()), (int) (ev.getY() * getScaleY()));
        }
        return super.onInterceptTouchEvent(ev);
    }


    @Override
    protected void measureContents(int width, int height) {
        int widthWithoutPadding = width - mPaddingLeft - mPaddingRight;
        int heightWithoutPadding = height - mPaddingTop - mPaddingBottom;

        // Measure the content
        mContent.measure(MeasureSpec.makeMeasureSpec(widthWithoutPadding, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(heightWithoutPadding, MeasureSpec.EXACTLY));

        // Optimization: Prevent overdraw of the thumbnail under the header view
        mThumbnailView.updateClipToTaskBar(mHeaderView);

        setMeasuredDimension(width, height);
    }

    void updateViewPropertiesToTaskTransform(TaskViewTransform toTransform,
            AnimationProps toAnimation, ValueAnimator.AnimatorUpdateListener updateCallback) {
        RecentsConfiguration config = Recents.getConfiguration();
        cancelTransformAnimation();

        // Compose the animations for the transform
        mTmpAnimators.clear();
        toTransform.applyToTaskView(this, mTmpAnimators, toAnimation, !config.fakeShadows);
        if (toAnimation.isImmediate()) {
            if (Float.compare(getDimAlpha(), toTransform.dimAlpha) != 0) {
                setDimAlpha(toTransform.dimAlpha);
            }
            if (Float.compare(mViewBounds.getAlpha(), toTransform.viewOutlineAlpha) != 0) {
                mViewBounds.setAlpha(toTransform.viewOutlineAlpha);
            }
            // Manually call back to the animator listener and update callback
            if (toAnimation.getListener() != null) {
                toAnimation.getListener().onAnimationEnd(null);
            }
            if (updateCallback != null) {
                updateCallback.onAnimationUpdate(null);
            }
        } else {
            // Both the progress and the update are a function of the bounds movement of the task
            if (Float.compare(getDimAlpha(), toTransform.dimAlpha) != 0) {
                ObjectAnimator anim = ObjectAnimator.ofFloat(this, DIM_ALPHA, getDimAlpha(),
                        toTransform.dimAlpha);
                mTmpAnimators.add(toAnimation.apply(AnimationProps.BOUNDS, anim));
            }
            if (Float.compare(mViewBounds.getAlpha(), toTransform.viewOutlineAlpha) != 0) {
                ObjectAnimator anim = ObjectAnimator.ofFloat(this, VIEW_OUTLINE_ALPHA,
                        mViewBounds.getAlpha(), toTransform.viewOutlineAlpha);
                mTmpAnimators.add(toAnimation.apply(AnimationProps.BOUNDS, anim));
            }
            if (updateCallback != null) {
                ValueAnimator updateCallbackAnim = ValueAnimator.ofInt(0, 1);
                updateCallbackAnim.addUpdateListener(updateCallback);
                mTmpAnimators.add(toAnimation.apply(AnimationProps.BOUNDS, updateCallbackAnim));
            }

            // Create the animator
            mTransformAnimation = toAnimation.createAnimator(mTmpAnimators);
            mTransformAnimation.start();
            mTargetAnimationTransform.copyFrom(toTransform);
        }
    }

    /** Resets this view's properties */
    void resetViewProperties() {
        cancelTransformAnimation();
        setDimAlpha(0);
        setVisibility(View.VISIBLE);
        getViewBounds().reset();
        getHeaderView().reset();
        TaskViewTransform.reset(this);

        mActionButtonView.setScaleX(1f);
        mActionButtonView.setScaleY(1f);
        mActionButtonView.setAlpha(0f);
        mActionButtonView.setTranslationZ(mActionButtonTranslationZ);
    }

    /**
     * @return whether we are animating towards {@param transform}
     */
    boolean isAnimatingTo(TaskViewTransform transform) {
        return mTransformAnimation != null && mTransformAnimation.isStarted()
                && mTargetAnimationTransform.isSame(transform);
    }

    /**
     * Cancels any current transform animations.
     */
    public void cancelTransformAnimation() {
        Utilities.cancelAnimationWithoutCallbacks(mTransformAnimation);
    }

    /** Enables/disables handling touch on this task view. */
    void setTouchEnabled(boolean enabled) {
        setOnClickListener(enabled ? this : null);
    }

    /** Animates this task view if the user does not interact with the stack after a certain time. */
    void startNoUserInteractionAnimation() {
        mHeaderView.startNoUserInteractionAnimation();
    }

    /** Mark this task view that the user does has not interacted with the stack after a certain time. */
    void setNoUserInteractionState() {
        mHeaderView.setNoUserInteractionState();
    }

    /** Resets the state tracking that the user has not interacted with the stack after a certain time. */
    void resetNoUserInteractionState() {
        mHeaderView.resetNoUserInteractionState();
    }

    /** Dismisses this task. */
    void dismissTask() {
        // Animate out the view and call the callback
        final TaskView tv = this;
        DismissTaskViewEvent dismissEvent = new DismissTaskViewEvent(tv, mTask);
        dismissEvent.addPostAnimationCallback(new Runnable() {
            @Override
            public void run() {
                EventBus.getDefault().send(new TaskViewDismissedEvent(mTask, tv));
            }
        });
        EventBus.getDefault().send(dismissEvent);
    }

    /**
     * Returns whether this view should be clipped, or any views below should clip against this
     * view.
     */
    boolean shouldClipViewInStack() {
        // Never clip for freeform tasks or if invisible
        if (mTask.isFreeformTask() || getVisibility() != View.VISIBLE) {
            return false;
        }
        return mClipViewInStack;
    }

    /** Sets whether this view should be clipped, or clipped against. */
    void setClipViewInStack(boolean clip) {
        if (clip != mClipViewInStack) {
            mClipViewInStack = clip;
            if (mCb != null) {
                mCb.onTaskViewClipStateChanged(this);
            }
        }
    }

    public TaskViewHeader getHeaderView() {
        return mHeaderView;
    }

    /**
     * Sets the current dim.
     */
    public void setDimAlpha(float dimAlpha) {
        mDimAlpha = dimAlpha;
        mThumbnailView.setDimAlpha(dimAlpha);
        mHeaderView.setDimAlpha(dimAlpha);
    }

    /**
     * Sets the current dim without updating the header's dim.
     */
    public void setDimAlphaWithoutHeader(float dimAlpha) {
        mDimAlpha = dimAlpha;
        mThumbnailView.setDimAlpha(dimAlpha);
    }

    /**
     * Returns the current dim.
     */
    public float getDimAlpha() {
        return mDimAlpha;
    }

    /**
     * Explicitly sets the focused state of this task.
     */
    public void setFocusedState(boolean isFocused, boolean requestViewFocus) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (isFocused) {
            if (requestViewFocus && !isFocused()) {
                requestFocus();
            }
            if (requestViewFocus && !isAccessibilityFocused() && ssp.isTouchExplorationEnabled()) {
                requestAccessibilityFocus();
            }
        } else {
            if (isAccessibilityFocused() && ssp.isTouchExplorationEnabled()) {
                clearAccessibilityFocus();
            }
        }
    }

    /**
     * Shows the action button.
     * @param fadeIn whether or not to animate the action button in.
     * @param fadeInDuration the duration of the action button animation, only used if
     *                       {@param fadeIn} is true.
     */
    public void showActionButton(boolean fadeIn, int fadeInDuration) {
        mActionButtonView.setVisibility(View.VISIBLE);

        if (fadeIn && mActionButtonView.getAlpha() < 1f) {
            mActionButtonView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(fadeInDuration)
                    .setInterpolator(Interpolators.ALPHA_IN)
                    .start();
        } else {
            mActionButtonView.setScaleX(1f);
            mActionButtonView.setScaleY(1f);
            mActionButtonView.setAlpha(1f);
            mActionButtonView.setTranslationZ(mActionButtonTranslationZ);
        }
    }

    /**
     * Immediately hides the action button.
     *
     * @param fadeOut whether or not to animate the action button out.
     */
    public void hideActionButton(boolean fadeOut, int fadeOutDuration, boolean scaleDown,
            final Animator.AnimatorListener animListener) {
        if (fadeOut && mActionButtonView.getAlpha() > 0f) {
            if (scaleDown) {
                float toScale = 0.9f;
                mActionButtonView.animate()
                        .scaleX(toScale)
                        .scaleY(toScale);
            }
            mActionButtonView.animate()
                    .alpha(0f)
                    .setDuration(fadeOutDuration)
                    .setInterpolator(Interpolators.ALPHA_OUT)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            if (animListener != null) {
                                animListener.onAnimationEnd(null);
                            }
                            mActionButtonView.setVisibility(View.INVISIBLE);
                        }
                    })
                    .start();
        } else {
            mActionButtonView.setAlpha(0f);
            mActionButtonView.setVisibility(View.INVISIBLE);
            if (animListener != null) {
                animListener.onAnimationEnd(null);
            }
        }
    }

    /**** TaskStackAnimationHelper.Callbacks Implementation ****/

    @Override
    public void onPrepareLaunchTargetForEnterAnimation() {
        // These values will be animated in when onStartLaunchTargetEnterAnimation() is called
        setDimAlphaWithoutHeader(0);
        mActionButtonView.setAlpha(0f);
    }

    @Override
    public void onStartLaunchTargetEnterAnimation(TaskViewTransform transform, int duration,
            boolean screenPinningEnabled, ReferenceCountedTrigger postAnimationTrigger) {
        // Dim the view after the app window transitions down into recents
        postAnimationTrigger.increment();
        AnimationProps animation = new AnimationProps(duration, Interpolators.ALPHA_OUT);
        Animator anim = animation.apply(AnimationProps.DIM_ALPHA, ObjectAnimator.ofFloat(this,
                DIM_ALPHA_WITHOUT_HEADER, getDimAlpha(), transform.dimAlpha));
        anim.addListener(postAnimationTrigger.decrementOnAnimationEnd());
        anim.start();

        if (screenPinningEnabled) {
            showActionButton(true /* fadeIn */, duration /* fadeInDuration */);
        }
    }

    @Override
    public void onStartLaunchTargetLaunchAnimation(int duration, boolean screenPinningRequested,
            ReferenceCountedTrigger postAnimationTrigger) {
        // Un-dim the view before/while launching the target
        AnimationProps animation = new AnimationProps(duration, Interpolators.ALPHA_OUT);
        Animator anim = animation.apply(AnimationProps.DIM_ALPHA, ObjectAnimator.ofFloat(this,
                DIM_ALPHA, getDimAlpha(), 0));
        anim.start();

        postAnimationTrigger.increment();
        hideActionButton(true /* fadeOut */, duration,
                !screenPinningRequested /* scaleDown */,
                postAnimationTrigger.decrementOnAnimationEnd());
    }

    /**** TaskCallbacks Implementation ****/

    public void onTaskBound(Task t) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        mTask = t;
        mTask.addCallback(this);
        mIsDisabledInSafeMode = !mTask.isSystemApp && ssp.isInSafeMode();
    }

    @Override
    public void onTaskDataLoaded(Task task) {
        // Bind each of the views to the new task data
        mThumbnailView.rebindToTask(mTask, mIsDisabledInSafeMode);
        mHeaderView.rebindToTask(mTask, mTouchExplorationEnabled, mIsDisabledInSafeMode);
        mTaskDataLoaded = true;
    }

    @Override
    public void onTaskDataUnloaded() {
        // Unbind each of the views from the task data and remove the task callback
        mTask.removeCallback(this);
        mThumbnailView.unbindFromTask();
        mHeaderView.unbindFromTask(mTouchExplorationEnabled);
        mTaskDataLoaded = false;
    }

    @Override
    public void onTaskStackIdChanged() {
        mHeaderView.rebindToTask(mTask, mTouchExplorationEnabled, mIsDisabledInSafeMode);
    }

    /**** View.OnClickListener Implementation ****/

    @Override
     public void onClick(final View v) {
        if (mIsDisabledInSafeMode) {
            Context context = getContext();
            String msg = context.getString(R.string.recents_launch_disabled_message, mTask.title);
            if (mDisabledAppToast != null) {
                mDisabledAppToast.cancel();
            }
            mDisabledAppToast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
            mDisabledAppToast.show();
            return;
        }

        boolean screenPinningRequested = false;
        if (v == mActionButtonView) {
            // Reset the translation of the action button before we animate it out
            mActionButtonView.setTranslationZ(0f);
            screenPinningRequested = true;
        }
        EventBus.getDefault().send(new LaunchTaskEvent(this, mTask, null, INVALID_STACK_ID,
                screenPinningRequested));

        MetricsLogger.action(v.getContext(), MetricsEvent.ACTION_OVERVIEW_SELECT,
                mTask.key.getComponent().toString());
    }

    /**** View.OnLongClickListener Implementation ****/

    @Override
    public boolean onLongClick(View v) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        // Since we are clipping the view to the bounds, manually do the hit test
        Rect clipBounds = new Rect(mViewBounds.mClipBounds);
        clipBounds.scale(getScaleX());
        boolean inBounds = clipBounds.contains(mDownTouchPos.x, mDownTouchPos.y);
        if (v == this && inBounds && !ssp.hasDockedTask()) {
            // Start listening for drag events
            setClipViewInStack(false);

            mDownTouchPos.x += ((1f - getScaleX()) * getWidth()) / 2;
            mDownTouchPos.y += ((1f - getScaleY()) * getHeight()) / 2;

            EventBus.getDefault().register(this, RecentsActivity.EVENT_BUS_PRIORITY + 1);
            EventBus.getDefault().send(new DragStartEvent(mTask, this, mDownTouchPos));
            return true;
        }
        return false;
    }

    /**** Events ****/

    public final void onBusEvent(DragEndEvent event) {
        if (!(event.dropTarget instanceof TaskStack.DockState)) {
            event.addPostAnimationCallback(new Runnable() {
                @Override
                public void run() {
                    // Animate the drag view back from where it is, to the view location, then after
                    // it returns, update the clip state
                    setClipViewInStack(true);
                }
            });
        }
        EventBus.getDefault().unregister(this);
    }

    /**
     * Reads current system flags related to accessibility and screen pinning.
     */
    private void readSystemFlags() {
        SystemServicesProxy ssp = Recents.getSystemServices();
        mTouchExplorationEnabled = ssp.isTouchExplorationEnabled();
    }
}
