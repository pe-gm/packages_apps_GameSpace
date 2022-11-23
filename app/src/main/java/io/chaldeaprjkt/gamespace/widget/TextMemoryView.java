/*
 * Copyright (C) 2022 riceDroid Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.chaldeaprjkt.gamespace.widget;

import android.app.ActivityManager;
import com.android.internal.util.MemInfoReader;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import java.lang.Runnable;

import io.chaldeaprjkt.gamespace.R;

public class TextMemoryView extends TextView {

    private ActivityManager mActivityManager;

    private Handler mHandler;
    private MemInfoWorker mWorker;

    public TextMemoryView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        mHandler = new Handler(Looper.getMainLooper());
        mWorker = new MemInfoWorker();
    }

    /* Hijack this method to detect visibility rather than
     * onVisibilityChanged() because the the latter one can be
     * influenced by more factors, leading to unstable behavior. */
    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);

        if (visibility == VISIBLE)
            mHandler.post(mWorker);
        else
            mHandler.removeCallbacks(mWorker);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setVisibility(VISIBLE);
    }

    @Override
    public void onDetachedFromWindow() {
        setVisibility(GONE);
        super.onDetachedFromWindow();
    }

    private class MemInfoWorker implements Runnable {
        @Override
        public void run() {
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            MemInfoReader mMemInfoReader = new MemInfoReader();
            mActivityManager.getMemoryInfo(memInfo);
            mMemInfoReader.readMemInfo();
            long totalMem = mMemInfoReader.getTotalSize();
            long usedZramMem = (mMemInfoReader.getSwapTotalSizeKb() * 1024) - (mMemInfoReader.getSwapFreeSizeKb() * 1024);
            long availMem = mMemInfoReader.getFreeSize() + mMemInfoReader.getKernelUsedSize() + mMemInfoReader.getCachedSize() + usedZramMem - memInfo.secondaryServerThreshold;
            long availMemMiB = availMem / (1024 * 1024);
            long totalMemMiB = totalMem / (1024 * 1024);
            int percentMem = (int)(Math.round(availMemMiB * 100.0 / totalMemMiB));
            int usedMem = (int)(100 - percentMem);
            setText(getContext().getString(R.string.memory_format, usedMem));

            mHandler.postDelayed(this, 1000);
        }
    }
}
