package com.example.xng.rkcamera;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;


public abstract class ProcessButton extends FlatButton {

	private int mProgress;
	private int mMaxProgress;
	private int mMinProgress;
	private GradientDrawable mProgressDrawable;// ��ɫ
	private GradientDrawable mProgressDrawableBg;// ��ɫ
	public ProcessButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	public ProcessButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public ProcessButton(Context context) {
		super(context);
		init(context, null);
	}

	private void init(Context context, AttributeSet attrs) {
		mMinProgress = 0;
		mMaxProgress = 100;

		mProgressDrawable = (GradientDrawable) getDrawable(
				R.drawable.rect_progress).mutate();
		mProgressDrawable.setCornerRadius(getCornerRadius());

		mProgressDrawableBg = (GradientDrawable) getDrawable(
				R.drawable.rect_progressbg).mutate();
		mProgressDrawableBg.setCornerRadius(getCornerRadius());

		if (attrs != null) {
			initAttributes(context, attrs);
		}
	}

	private void initAttributes(Context context, AttributeSet attributeSet) {
		TypedArray attr = getTypedArray(context, attributeSet,
				R.styleable.ProcessButton);

		if (attr == null) {
			return;
		}

		try {

			int purple = getColor(R.color.purple_progress);
			int colorProgress = attr.getColor(
					R.styleable.ProcessButton_pb_colorProgress, purple);
			mProgressDrawable.setColor(colorProgress);

		} finally {
			attr.recycle();
		}
	}

	public void setProgress(int progress) {
		mProgress = progress;
		onProgress();
		invalidate();
	}

	// ��������ʾ�����Լ���ɫ����
	public void onProgress() {
		setText(mProgress + "%");
		// ���û�ɫ����
		setBackgroundCompat(getmProgressDrawableBg());
	}

	public void onNormalState(String mcomtext) {
		if (mcomtext != null) {
			setText(mcomtext);
		}
		setBackgroundCompat(getNormalDrawable());
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// progress
		if (mProgress > mMinProgress && mProgress < mMaxProgress) {
			drawProgress(canvas);
		}

		super.onDraw(canvas);
	}

	public abstract void drawProgress(Canvas canvas);

	public int getProgress() {
		return mProgress;
	}

	public int getMaxProgress() {
		return mMaxProgress;
	}

	public int getMinProgress() {
		return mMinProgress;
	}

	public GradientDrawable getProgressDrawable() {
		return mProgressDrawable;
	}

	public void setProgressDrawable(GradientDrawable progressDrawable) {
		mProgressDrawable = progressDrawable;
	}

	public GradientDrawable getmProgressDrawableBg() {
		return mProgressDrawableBg;
	}

	public void setmProgressDrawableBg(GradientDrawable mProgressDrawableBg) {
		this.mProgressDrawableBg = mProgressDrawableBg;
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState savedState = new SavedState(superState);
		savedState.mProgress = mProgress;

		return savedState;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		if (state instanceof SavedState) {
			SavedState savedState = (SavedState) state;
			mProgress = savedState.mProgress;
			super.onRestoreInstanceState(savedState.getSuperState());
			setProgress(mProgress);
		} else {
			super.onRestoreInstanceState(state);
		}
	}

	/**
	 * A {@link Parcelable} representing the
	 * {@link com.example.xng.rkcamera.ProcessButton}'s state.
	 */
	public static class SavedState extends BaseSavedState {

		private int mProgress;

		public SavedState(Parcelable parcel) {
			super(parcel);
		}

		private SavedState(Parcel in) {
			super(in);
			mProgress = in.readInt();
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeInt(mProgress);
		}

		public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {

			@Override
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			@Override
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}
}
