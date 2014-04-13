package com.seatgeek.test.subwaysvg;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.seatgeek.test.view.SvgTraceView;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private SvgTraceView mSvgTraceView;

    private ObjectAnimator mAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSvgTraceView = (SvgTraceView) findViewById(R.id.svg);
        mSvgTraceView.setSvgResource(R.raw.nyc_subway);

        findViewById(R.id.run).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAnimator == null) {
                    mAnimator = ObjectAnimator.ofFloat(mSvgTraceView, "phase", 1f, 0.0f);
                    mAnimator.setDuration(4000);
                }

                if (mAnimator.isRunning()) {
                    mAnimator.reverse();
                } else {
                    mAnimator.start();
                }
            }
        });
    }
}
