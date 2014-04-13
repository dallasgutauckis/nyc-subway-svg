/**
 * Copyright 2013 Romain Guy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.seatgeek.test.view;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.seatgeek.test.subwaysvg.R;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class SvgTraceView extends View {
    private static final String LOG_TAG = "SvgTraceView";

    private Paint mPaint;

    private final SvgHelper mSvg;
    private int mSvgResource;

    private final Object mSvgLock = new Object();
    private List<SvgHelper.SvgPath> mPaths = new ArrayList<SvgHelper.SvgPath>(0);
    private Thread mLoader;

    private float mPhase;
    private float mFadeFactor;
    private int mDuration;
    private float mParallax = 1.0f;
    private float mOffsetY;

    private ObjectAnimator mSvgAnimator;

    public SvgTraceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SvgTraceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.IntroView, defStyle, 0);
        try {
            if (a != null) {
                float strokeWidth = a.getFloat(R.styleable.SvgTraceView_strokeWidth, -1f);

                if (strokeWidth != -1f) {
                    createPaint();
                    mPaint.setStrokeWidth(strokeWidth);
                }

                int color = a.getColor(R.styleable.SvgTraceView_strokeColor, Color.TRANSPARENT);

                if (color != Color.TRANSPARENT) {
                    createPaint();
                    mPaint.setColor(color);
                }

                mPhase = a.getFloat(R.styleable.SvgTraceView_phase, 0.0f);
                mDuration = a.getInt(R.styleable.SvgTraceView_duration, 4000);
                mFadeFactor = a.getFloat(R.styleable.SvgTraceView_fadeFactor, 10.0f);
            }
        } finally {
            if (a != null) a.recycle();
        }

        mSvg = new SvgHelper(mPaint);
    }

    private void createPaint() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
    }

    private void updatePathsPhaseLocked() {
        final int count = mPaths.size();
        for (int i = 0; i < count; i++) {
            SvgHelper.SvgPath svgPath = mPaths.get(i);
            svgPath.paint.setPathEffect(createPathEffect(svgPath.length, mPhase, 0.0f));
        }
    }

    public float getParallax() {
        return mParallax;
    }

    public void setParallax(float parallax) {
        mParallax = parallax;
        invalidate();
    }

    public float getPhase() {
        return mPhase;
    }

    public void setPhase(float phase) {
        mPhase = phase;
        synchronized (mSvgLock) {
            updatePathsPhaseLocked();
        }
        invalidate();
    }

    public int getSvgResource() {
        return mSvgResource;
    }

    public void setSvgResource(int svgResource) {
        mSvgResource = svgResource;
    }

    @Override
    protected void onSizeChanged(final int w, final int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (mLoader != null) {
            try {
                mLoader.join();
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Unexpected error", e);
            }
        }

        mLoader = new Thread(new Runnable() {
            @Override
            public void run() {
                mSvg.load(getContext(), mSvgResource);
                synchronized (mSvgLock) {
                    mPaths = mSvg.getPathsForViewport(
                            w - getPaddingLeft() - getPaddingRight(),
                            h - getPaddingTop() - getPaddingBottom());

                    updatePathsPhaseLocked();
                }
            }
        }, "SVG Loader");
        mLoader.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        synchronized (mSvgLock) {
            canvas.save();
            canvas.translate(getPaddingLeft(), getPaddingTop() + mOffsetY);

            final int count = mPaths.size();

            for (int i = 0; i < count; i++) {
                SvgHelper.SvgPath svgPath = mPaths.get(i);

                // We use the fade factor to speed up the alpha animation
                int alpha = (int) (Math.min((1.0f - mPhase) * mFadeFactor, 1.0f) * 255.0f);
                svgPath.paint.setAlpha((int) (alpha * mParallax));

                canvas.drawPath(svgPath.path, svgPath.paint);
            }
            canvas.restore();
        }
    }

    private static PathEffect createPathEffect(float pathLength, float phase, float offset) {
        return new DashPathEffect(new float[] { pathLength, pathLength },
                Math.max(phase * pathLength, offset));
    }

    public void reveal(View view, int parentBottom) {
        if (mSvgAnimator == null) {
            mSvgAnimator = ObjectAnimator.ofFloat(this, "phase", mPhase, 0.0f);
            mSvgAnimator.setDuration(mDuration);
            mSvgAnimator.start();
        }

        float previousOffset = mOffsetY;
        mOffsetY = Math.min(0, view.getHeight() - (parentBottom - view.getScrollY()));
        if (previousOffset != mOffsetY) invalidate();
    }
}