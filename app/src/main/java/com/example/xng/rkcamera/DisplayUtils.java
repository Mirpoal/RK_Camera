package com.example.xng.rkcamera;

import android.content.Context;
/**
 * 单位转换类
 *
 * @author WangYuWen
 * @version 1.0
 * @date 2015年1月30日
 * @Copyright: Copyright (c) 2014 Shenzhen Utoow Technology Co., Ltd. All rights reserved.
 */
public class DisplayUtils {

	/**
	 * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
	 *
	 * @version 1.0
	 * @createTime 2015年1月30日,下午1:47:55
	 * @updateTime 2015年1月30日,下午1:47:55
	 * @createAuthor WangYuWen
	 * @updateAuthor WangYuWen
	 * @updateInfo (此处输入修改内容,若无修改可不写.)
	 *
	 *  @param context 
	 *  @param dpValue
	 *  @return
	 */
	public static int dip2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}

	/**
	 * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
	 *
	 * @version 1.0
	 * @createTime 2015年1月30日,下午1:48:03
	 * @updateTime 2015年1月30日,下午1:48:03
	 * @createAuthor WangYuWen
	 * @updateAuthor WangYuWen
	 * @updateInfo (此处输入修改内容,若无修改可不写.)
	 *
	 *  @param context
	 *  @param pxValue
	 *  @return
	 */
	public static int px2dip(Context context, float pxValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (pxValue / scale + 0.5f);
	}

	/**
	 * 将px值转换为sp值，保证文字大小不变
	 *
	 * @version 1.0
	 * @createTime 2015年1月30日,下午1:48:10
	 * @updateTime 2015年1月30日,下午1:48:10
	 * @createAuthor WangYuWen
	 * @updateAuthor WangYuWen
	 * @updateInfo (此处输入修改内容,若无修改可不写.)
	 *
	 *  @param context
	 *  @param pxValue
	 *  @return
	 */
	public static int px2sp(Context context, float pxValue) {
		final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
		return (int) (pxValue / fontScale + 0.5f);
	}

	/**
	 * 将sp值转换为px值，保证文字大小不变
	 *
	 * @version 1.0
	 * @createTime 2015年1月30日,下午1:48:14
	 * @updateTime 2015年1月30日,下午1:48:14
	 * @createAuthor WangYuWen
	 * @updateAuthor WangYuWen
	 * @updateInfo (此处输入修改内容,若无修改可不写.)
	 *
	 *  @param context
	 *  @param spValue
	 *  @return
	 */
	public static int sp2px(Context context, float spValue) {
		final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
		return (int) (spValue * fontScale + 0.5f);
	}
}
