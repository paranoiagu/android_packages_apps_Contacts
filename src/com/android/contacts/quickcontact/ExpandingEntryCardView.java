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
package com.android.contacts.quickcontact;

import com.android.contacts.R;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.QuickContact;
import android.support.v4.text.TextUtilsCompat;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Display entries in a LinearLayout that can be expanded to show all entries.
 */
public class ExpandingEntryCardView extends LinearLayout {

    private static final String TAG = "ExpandingEntryCardView";

    /**
     * Entry data.
     */
    public static final class Entry {

        private final Drawable mIcon;
        private final String mHeader;
        private final String mSubHeader;
        private final Drawable mSubHeaderIcon;
        private final String mText;
        private final Drawable mTextIcon;
        private final Intent mIntent;
        private final boolean mIsEditable;

        public Entry(Drawable icon, String header, String subHeader, String text,
                Intent intent, boolean isEditable) {
            this(icon, header, subHeader, null, text, null, intent, isEditable);
        }

        public Entry(Drawable mainIcon, String header, String subHeader,
                Drawable subHeaderIcon, String text, Drawable textIcon, Intent intent,
                boolean isEditable) {
            mIcon = mainIcon;
            mHeader = header;
            mSubHeader = subHeader;
            mSubHeaderIcon = subHeaderIcon;
            mText = text;
            mTextIcon = textIcon;
            mIntent = intent;
            mIsEditable = isEditable;
        }

        Drawable getIcon() {
            return mIcon;
        }

        String getHeader() {
            return mHeader;
        }

        String getSubHeader() {
            return mSubHeader;
        }

        Drawable getSubHeaderIcon() {
            return mSubHeaderIcon;
        }

        public String getText() {
            return mText;
        }

        Drawable getTextIcon() {
            return mTextIcon;
        }

        Intent getIntent() {
            return mIntent;
        }

        boolean isEditable() {
            return mIsEditable;
        }
    }

    private View mExpandCollapseButton;
    private TextView mExpandCollapseTextView;
    private TextView mTitleTextView;
    private CharSequence mExpandButtonText;
    private CharSequence mCollapseButtonText;
    private OnClickListener mOnClickListener;
    private boolean mIsExpanded = false;
    private int mCollapsedEntriesCount;
    private List<View> mEntryViews;
    private LinearLayout mEntriesViewGroup;
    private int mThemeColor;

    private final OnClickListener mExpandCollapseButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mIsExpanded) {
                collapse();
            } else {
                expand();
            }
        }
    };

    public ExpandingEntryCardView(Context context) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View expandingEntryCardView = inflater.inflate(R.layout.expanding_entry_card_view, this);
        mEntriesViewGroup = (LinearLayout)
                expandingEntryCardView.findViewById(R.id.content_area_linear_layout);
        mTitleTextView = (TextView) expandingEntryCardView.findViewById(R.id.title);
    }

    public ExpandingEntryCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(context);
        View expandingEntryCardView = inflater.inflate(R.layout.expanding_entry_card_view, this);
        mEntriesViewGroup = (LinearLayout)
                expandingEntryCardView.findViewById(R.id.content_area_linear_layout);
        mTitleTextView = (TextView) expandingEntryCardView.findViewById(R.id.title);
    }

    /**
     * Sets the Entry list to display.
     *
     * @param entries The Entry list to display.
     */
    public void initialize(List<Entry> entries, int numInitialVisibleEntries,
            boolean isExpanded, int themeColor) {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        mIsExpanded = isExpanded;
        mEntryViews = createEntryViews(layoutInflater, entries);
        mThemeColor = themeColor;
        mCollapsedEntriesCount = Math.min(numInitialVisibleEntries, entries.size());
        if (mExpandCollapseButton == null) {
            createExpandButton(layoutInflater);
        }
        insertEntriesIntoViewGroup();
    }

    /**
     * Sets the text for the expand button.
     *
     * @param expandButtonText The expand button text.
     */
    public void setExpandButtonText(CharSequence expandButtonText) {
        mExpandButtonText = expandButtonText;
        if (mExpandCollapseTextView != null && !mIsExpanded) {
            mExpandCollapseTextView.setText(expandButtonText);
        }
    }

    /**
     * Sets the text for the expand button.
     *
     * @param expandButtonText The expand button text.
     */
    public void setCollapseButtonText(CharSequence expandButtonText) {
        mCollapseButtonText = expandButtonText;
        if (mExpandCollapseTextView != null && mIsExpanded) {
            mExpandCollapseTextView.setText(mCollapseButtonText);
        }
    }

    @Override
    public void setOnClickListener(OnClickListener listener) {
        mOnClickListener = listener;
    }

    private void insertEntriesIntoViewGroup() {
        mEntriesViewGroup.removeAllViews();
        for (int i = 0; i < mCollapsedEntriesCount; ++i) {
            addEntry(mEntryViews.get(i));
        }
        if (mIsExpanded) {
            for (int i = mCollapsedEntriesCount; i < mEntryViews.size(); ++i) {
                addEntry(mEntryViews.get(i));
            }
        }

        removeView(mExpandCollapseButton);
        if (mCollapsedEntriesCount < mEntryViews.size()
                && mExpandCollapseButton.getParent() == null) {
            addView(mExpandCollapseButton, -1);
        }
    }

    private void addEntry(View entry) {
        if (mEntriesViewGroup.getChildCount() > 0) {
            View separator = new View(getContext());
            separator.setBackgroundColor(getResources().getColor(
                    R.color.expanding_entry_card_item_separator_color));
            LayoutParams layoutParams = generateDefaultLayoutParams();
            Resources resources = getResources();
            layoutParams.height = resources.getDimensionPixelSize(
                    R.dimen.expanding_entry_card_item_separator_height);
            if (getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                layoutParams.rightMargin = resources.getDimensionPixelSize(
                        R.dimen.expanding_entry_card_item_padding_start);
                layoutParams.leftMargin = resources.getDimensionPixelSize(
                        R.dimen.expanding_entry_card_item_padding_end);
            } else {
                layoutParams.leftMargin = resources.getDimensionPixelSize(
                        R.dimen.expanding_entry_card_item_padding_start);
                layoutParams.rightMargin = resources.getDimensionPixelSize(
                        R.dimen.expanding_entry_card_item_padding_end);
            }
            separator.setLayoutParams(layoutParams);
            mEntriesViewGroup.addView(separator);
        }
        mEntriesViewGroup.addView(entry);
    }

    private CharSequence getExpandButtonText() {
        if (!TextUtils.isEmpty(mExpandButtonText)) {
            return mExpandButtonText;
        } else {
            // Default to "See more".
            return getResources().getText(R.string.expanding_entry_card_view_see_more);
        }
    }

    private CharSequence getCollapseButtonText() {
        if (!TextUtils.isEmpty(mCollapseButtonText)) {
            return mCollapseButtonText;
        } else {
            // Default to "See less".
            return getResources().getText(R.string.expanding_entry_card_view_see_less);
        }
    }

    private void createExpandButton(LayoutInflater layoutInflater) {
        mExpandCollapseButton = layoutInflater.inflate(
                R.layout.quickcontact_expanding_entry_card_button, this, false);
        mExpandCollapseTextView = (TextView) mExpandCollapseButton.findViewById(R.id.text);
        if (mIsExpanded) {
            updateExpandCollapseButton(getCollapseButtonText());
        } else {
            updateExpandCollapseButton(getExpandButtonText());
        }
        mExpandCollapseButton.setOnClickListener(mExpandCollapseButtonListener);
    }

    private List<View> createEntryViews(LayoutInflater layoutInflater, List<Entry> entries) {
        ArrayList<View> views = new ArrayList<View>(entries.size());
        for (int i = 0; i < entries.size(); ++i) {
            Entry entry = entries.get(i);
            views.add(createEntryView(layoutInflater, entry));
        }
        return views;
    }

    private View createEntryView(LayoutInflater layoutInflater, Entry entry) {
        View view = layoutInflater.inflate(
                R.layout.expanding_entry_card_item, this, false);

        ImageView icon = (ImageView) view.findViewById(R.id.icon);
        icon.setImageDrawable(entry.getIcon());

        TextView header = (TextView) view.findViewById(R.id.header);
        if (entry.getHeader() != null) {
            header.setText(entry.getHeader());
        } else {
            header.setVisibility(View.GONE);
        }

        TextView subHeader = (TextView) view.findViewById(R.id.sub_header);
        if (entry.getSubHeader() != null) {
            subHeader.setText(entry.getSubHeader());
        } else {
            subHeader.setVisibility(View.GONE);
        }

        ImageView subHeaderIcon = (ImageView) view.findViewById(R.id.icon_sub_header);
        if (entry.getSubHeaderIcon() != null) {
            subHeaderIcon.setImageDrawable(entry.getSubHeaderIcon());
        } else {
            subHeaderIcon.setVisibility(View.GONE);
        }

        TextView text = (TextView) view.findViewById(R.id.text);
        if (entry.getText() != null) {
            text.setText(entry.getText());
        } else {
            text.setVisibility(View.GONE);
        }

        ImageView textIcon = (ImageView) view.findViewById(R.id.icon_text);
        if (entry.getTextIcon() != null) {
            textIcon.setImageDrawable(entry.getTextIcon());
        } else {
            textIcon.setVisibility(View.GONE);
        }

        if (entry.getIntent() != null) {
            View entryLayout = view.findViewById(R.id.entry_layout);
            entryLayout.setOnClickListener(mOnClickListener);
            entryLayout.setTag(entry.getIntent());
        }

        return view;
    }

    private void updateExpandCollapseButton(CharSequence buttonText) {
        int resId = mIsExpanded ? R.drawable.expanding_entry_card_collapse_white_24
                : R.drawable.expanding_entry_card_expand_white_24;
        // TODO: apply color theme to the drawable
        Drawable drawable = getResources().getDrawable(resId);
        if (getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            mExpandCollapseTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable,
                    null);
        } else {
            mExpandCollapseTextView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null,
                    null);
        }
        mExpandCollapseTextView.setText(buttonText);
    }

    private void expand() {
        final int startingHeight = mEntriesViewGroup.getHeight();

        mIsExpanded = true;
        insertEntriesIntoViewGroup();
        updateExpandCollapseButton(getCollapseButtonText());

        createExpandAnimator(startingHeight, measureContentAreaHeight()).start();
    }

    private void collapse() {
        int startingHeight = mEntriesViewGroup.getHeight();

        // Figure out the height the view will be after the animation is finished.
        mIsExpanded = false;
        insertEntriesIntoViewGroup();
        int finishHeight = measureContentAreaHeight();

        // During the animation, mEntriesViewGroup should contain the same views as it did before
        // the animation. Otherwise, the animation will look very silly.
        mIsExpanded = true;
        insertEntriesIntoViewGroup();

        mIsExpanded = false;
        updateExpandCollapseButton(getExpandButtonText());
        createExpandAnimator(startingHeight, finishHeight).start();
    }

    private int measureContentAreaHeight() {
        // Measure the LinearLayout, assuming no constraints from the parent.
        final int widthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final int heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        mEntriesViewGroup.measure(widthSpec, heightSpec);
        return mEntriesViewGroup.getMeasuredHeight();
    }

    /**
     * Create ValueAnimator that performs an expand animation on the content LinearLayout.
     *
     * The animation needs to be performed manually using a ValueAnimator, since LinearLayout
     * doesn't have a single set-able height property (ie, no setHeight()).
     */
    private ValueAnimator createExpandAnimator(int start, int end) {
        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int value = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = mEntriesViewGroup.getLayoutParams();
                layoutParams.height = value;
                mEntriesViewGroup.setLayoutParams(layoutParams);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                insertEntriesIntoViewGroup();
            }
        });
        return animator;
    }

    /**
     * Returns whether the view is currently in its expanded state.
     */
    public boolean isExpanded() {
        return mIsExpanded;
    }

    /**
     * Sets the title text of this ExpandingEntryCardView.
     * @param title The title to set. A null title will result in an empty string being set.
     */
    public void setTitle(String title) {
        if (mTitleTextView == null) {
            Log.e(TAG, "mTitleTextView is null");
        }
        if (title == null) {
            mTitleTextView.setText("");
        }
        mTitleTextView.setText(title);
    }
}