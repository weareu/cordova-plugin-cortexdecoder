package com.cordova.plugins.cortexdecoder.view;
// Copyright (c) 2014-2019 The Code Corporation. All rights reserved.

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;


public class BarcodeFinderView extends View {

  public String barcodeData;
  float p1x, p1y, p2x, p2y, p3x, p3y, p4x, p4y;
  float mWidth;
  int screenDiff;
  int mHeight;
  Context mContext;

  public BarcodeFinderView(Context context, int[] points, int screenWidth, int screenHeight, int screenHeightDiff, float heightRatio, float widthRatio, String data) {
    super(context);
    mInitializePoints(points, heightRatio);
    mWidth = screenWidth;
    mHeight = screenHeight;
    screenDiff = screenHeightDiff;
    mContext = context;
    barcodeData = data;
  }

  public BarcodeFinderView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  private void mInitializePoints(int[] p, float hr) {
    p1x = p[0] * hr;
    p1y = p[1] * hr;
    p2x = p[2] * hr;
    p2y = p[3] * hr;
    p3x = p[4] * hr;
    p3y = p[5] * hr;
    p4x = p[6] * hr;
    p4y = p[7] * hr;
  }

  @Override
  public void onDraw(Canvas canvas) {
    WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
    if (wm == null) return;
    int rotation = wm.getDefaultDisplay().getRotation();
    switch (rotation) {
      case Surface.ROTATION_0:
        drawPortrait(canvas, 0);
        break;
      case Surface.ROTATION_90:
        drawLandscape(canvas, 1);
        break;
      case Surface.ROTATION_180:
        drawPortrait(canvas, 2);
        break;
      case Surface.ROTATION_270:
        drawLandscape(canvas, 3);
        break;

      default:
        break;
    }
  }

  private void drawPortrait(Canvas c, int rotate) {
    Paint mPaint = new Paint();
    mPaint.setColor(1748159794);
    mPaint.setStyle(Paint.Style.STROKE);
    mPaint.setStrokeWidth(10);
    Path mPath = new Path();

    if (rotate == 2) {
      inversePortrait(mPath, c, mPaint);
    } else {
      mPath.moveTo(mWidth + (-1 * p1y), p1x - screenDiff);
      mPath.lineTo(mWidth + (-1 * p2y), p2x - screenDiff);
      mPath.lineTo(mWidth + (-1 * p3y), p3x - screenDiff);
      mPath.lineTo(mWidth + (-1 * p4y), p4x - screenDiff);
      mPath.lineTo(mWidth + (-1 * p1y), p1x - screenDiff - 7);
      c.drawPath(mPath, mPaint);
    }

  }

  private void drawLandscape(Canvas c, int rotation) {
    int xOffset = 0;
    int yOffset;
    int xMulti = 1;
    int yMulti = 1;
    if (rotation == 3) {
      xOffset = (int) mWidth;
      yOffset = (mHeight * -1) + screenDiff;
      xMulti *= -1;
      yMulti *= -1;
    } else {
      yOffset = screenDiff;
    }

    Paint mPaint = new Paint();
    mPaint.setColor(2070243378);
    mPaint.setStyle(Paint.Style.STROKE);
    mPaint.setStrokeWidth(10);

    Path mPath = new Path();

    mPath.moveTo((p1x * xMulti) + xOffset, (p1y * yMulti) - yOffset);
    mPath.lineTo((p2x * xMulti) + xOffset, (p2y * yMulti) - yOffset);
    mPath.lineTo((p3x * xMulti) + xOffset, (p3y * yMulti) - yOffset);
    mPath.lineTo((p4x * xMulti) + xOffset, (p4y * yMulti) - yOffset);
    mPath.lineTo((p1x * xMulti) + xOffset, (p1y * yMulti) - yOffset);

    c.drawPath(mPath, mPaint);
  }


  private void inversePortrait(Path p, Canvas c, Paint paint) {
    float tp1x = p1y;
    float tp1y = (-1 * p1x);
    float tp2x = p2y;
    float tp2y = (-1 * p2x);
    float tp3x = p3y;
    float tp3y = (-1 * p3x);
    float tp4x = p4y;
    float tp4y = (-1 * p4x);

    p.moveTo(tp1x, tp1y + mHeight - screenDiff);
    p.lineTo(tp2x, tp2y + mHeight - screenDiff);
    p.lineTo(tp3x, tp3y + mHeight - screenDiff);
    p.lineTo(tp4x, tp4y + mHeight - screenDiff);
    p.lineTo(tp1x, tp1y + mHeight - screenDiff);
    c.drawPath(p, paint);

  }

}
