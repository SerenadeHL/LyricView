package com.serenade.lyricview.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Y on 2017/4/25.
 */

public class LyricView extends View implements GestureDetector.OnGestureListener {

    private LyricInfo mLyricInfo;//歌词信息
    private List<LineInfo> mLines;//歌词行List

    private Paint mPaint;//指示器画笔
    private TextPaint mTextPaint;//歌词画笔
    private StaticLayout mStaticLayout;//绘制歌词区域

    private int mViewHeight = 0;//控件高度
    private int mViewWidth = 0;//控件宽度

    private String mDefaultText = "";//无歌词时显示的内容
    private int mDuration = 0;//歌曲总时长
    private int mCurrentPosition = 0;//当前播放位置
    private float mLineSpace = 0;//歌词间距
    private int mLeftAndRightPadding = 20;//歌词左右间距
    private int mPlayingLyricSize = 0;//播放行歌词字体大小
    private int mUnPlayingLyricSize = 0;//未播放行歌词字体大小
    private int mPlayingLyricColor = Color.parseColor("#F3520F");//播放行歌词字体颜色
    private int mUnPlayingLyricColor = Color.parseColor("#456789");//未播放行歌词字体颜色
    private float mCurrentLineTop = 0;//播放行顶部坐标
    private float mCurrentLineBottom = 0;//播放行底部坐标

    private int mCurrentLineIndex = 0;//当前播放行索引
    private int mNewLineIndex = 0;//当前播放行下一行索引

    private boolean mDragging = false;//是否拖动中
    private GestureDetector mGestureDetector;//手势监听
    private float mSpeed = 4;//拖动歌词速度
    private float mPartOfFling = 0;//滑动距离除以拖动歌词速度
    private boolean mFlinging = false;//是否处于滑动中
    private ValueAnimator mFlingAnimator;//滑动动画
    private float mDraggedOffset = 0;//拖动完成后中心与歌词行中心差值
    private float mDragged = 0;//拖动歌词的距离
    private int mDraggedLine = 0;//拖动后歌词处于的位置
    private boolean mHasMessage = false;//按下屏幕时记录是否有回滚到当前播放位置任务
    private int mClearTime = 2000;//拖动操作完成后返回播放位置的时间间隔
    private int mBackTime = 1000;//拖动操作完成后返回播放位置的动画时间
    private float mIndicatorRadius = 0;//指示器半径
    private float mIndicatorCenterX;//指示器中心点X轴坐标
    private float mIndicatorCenterY;//指示器中心点Y轴坐标
    private int mIndicatorLeft = 10;//指示器左边距
    private int mIndicatorColor = Color.parseColor("#F3520F");//指示器颜色
    private int mBrokenLineWidth = 20;//虚线单个Item长度
    private int mBrokenLineSpace = 20;//虚线间隔
    private int mBrokenLineLeft = 20;//虚线左边距
    private int mBrokenLineRight = 20;//虚线右边距
    private IndicatorListener mIndicatorListener;//指示器播放按钮点击回调接口

    private int mAnimatorDuration = 800;//歌词滚动动画时长


    private Handler mHandler;//歌词滚动Handler
    private int mRefreshTime = 400;//歌词检索刷新频率
    private boolean mChanged = true;//用来记录是否是否换行

    private float mDrawingStartY = 0;//开始绘制歌词Y轴位置
    private float mOffset = 0;//歌词滚动过的位置
    private static final int REFRESH = 1;
    private static final int CLEAR_DRAGGED = 2;

    public LyricView(Context context) {
        super(context);
        init();
    }

    public LyricView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LyricView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        setIndicatorPaint();
        mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);
        setUnPlayingTextPaint();
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int what = msg.what;
                switch (what) {
                    case REFRESH:
                        mCurrentPosition += mRefreshTime;
                        measureCurrentLine();
                        if (mNewLineIndex != mCurrentLineIndex && mChanged && !mDragging && mDragged == 0) {
                            smoothScrollTo(mLines.get(mCurrentLineIndex).getMiddle(), mLines.get(mNewLineIndex).getMiddle());
                            mChanged = false;
                        }
                        if (mCurrentPosition <= mDuration)
                            sendEmptyMessageDelayed(REFRESH, mRefreshTime);
                        break;
                    case CLEAR_DRAGGED:
                        backToPlayingLine(mDragged, 0);
//                        mDragged = 0;
//                        reDraw();
                        break;
                }
            }
        };
        mGestureDetector = new GestureDetector(getContext(), this);
    }

    /**
     * 设置指示器画笔
     */
    private void setIndicatorPaint() {
        mPaint.setColor(mIndicatorColor);
    }

    /**
     * 设置播放行画笔及指示器画笔
     */
    private void setPlayingTextPaint() {
        mTextPaint.setTextSize(mPlayingLyricSize);
        mTextPaint.setColor(mPlayingLyricColor);
        mPaint.setColor(mPlayingLyricColor);
    }

    /**
     * 设置未播放行画笔
     */
    private void setUnPlayingTextPaint() {
        mTextPaint.setTextSize(mUnPlayingLyricSize);
        mTextPaint.setColor(mUnPlayingLyricColor);
    }

    /**
     * 设置当前播放位置
     *
     * @param current 当前播放位置
     */
    public void setCurrentPosition(int current) {
        mCurrentPosition = current;
    }

    /**
     * 设置歌曲总时长
     *
     * @param duration 歌曲总时长
     */
    public void setDuration(int duration) {
        mDuration = duration;
    }

    /**
     * 设置无歌词时显示的内容
     *
     * @param text 无歌词时显示的内容
     */
    public void setNoLyricText(String text) {
        mDefaultText = text;
    }

    /**
     * 设置歌词行间距
     *
     * @param space 歌词行间距
     */
    public void setLineSpace(float space) {
        mLineSpace = space;
        reDraw();
    }

    /**
     * 设置播放行字体大小
     *
     * @param size 播放行字体大小
     */
    public void setPlayingLyricSize(int size) {
        mPlayingLyricSize = size;
        reDraw();
    }

    /**
     * 设置未播放行字体大小
     *
     * @param size 未播放行字体大小
     */
    public void setUnPlayingLyricSize(int size) {
        mUnPlayingLyricSize = size;
        reDraw();
    }

    /**
     * 设置播放行字体颜色
     *
     * @param color 播放行字体颜色
     */
    public void setPlayingLyricColor(int color) {
        mPlayingLyricColor = color;
        reDraw();
    }

    /**
     * 设置未播放行字体颜色
     *
     * @param color 未播放行字体颜色
     */
    public void setUnPlayingLyricColor(int color) {
        mUnPlayingLyricColor = color;
        reDraw();
    }

    /**
     * 设置指示器颜色
     *
     * @param color 指示器颜色
     */
    public void setIndicatorColor(int color) {
        mIndicatorColor = color;
        setIndicatorPaint();
    }

    /**
     * 设置歌词，并且解析
     *
     * @param lyric 歌词
     */
    public void setLyric(String lyric) {
        if (!TextUtils.isEmpty(lyric)) {
            mLyricInfo = parse_whole(lyric);
            mLines = mLyricInfo.getSong_lines();
        } else
            mDefaultText = "暂无歌词";
    }

    /**
     * 设置歌词文件，并且解析
     *
     * @param lryric_file 歌词文件
     */
    public void setLyric(File lryric_file) {
        BufferedReader reader = null;
        StringBuilder lyric_builder = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(lryric_file)));
            String line = "";
            while ((line= reader.readLine())!=null){
                lyric_builder.append(line).append("/n");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String lyric = lyric_builder.toString();
        if (!TextUtils.isEmpty(lyric)) {
            mLyricInfo = parse_whole(lyric);
            mLines = mLyricInfo.getSong_lines();
        } else
            mDefaultText = "暂无歌词";
    }

    /**
     * 解析全部歌词
     *
     * @param lyric 全部歌词
     */
    private LyricInfo parse_whole(String lyric) {
        String[] split = lyric.split("\t");
        LyricInfo lyricInfo = new LyricInfo();
        for (String line : split) {
            parse_line(lyricInfo, line);
        }
        return lyricInfo;
    }

    /**
     * 重新绘制View
     */
    private void reDraw() {
        if (Looper.myLooper() == Looper.getMainLooper())
            invalidate();
        else
            postInvalidate();
    }

    /**
     * 解析单行歌词
     *
     * @param line 单行歌词
     */
    private void parse_line(LyricInfo lyricInfo, String line) {
        line = line.replace("\n", "").replace("\t", "").trim();
        int index = line.lastIndexOf("]");
        if (!TextUtils.isEmpty(line)) {
            if (line.startsWith("[offset:")) {
                // 时间偏移量
                String string = line.substring(8, index).trim();
                lyricInfo.setSong_offset(Long.parseLong(string));
                return;
            }
            if (line.startsWith("[ti:")) {
                // 标题
                String string = line.substring(4, index).trim();
                lyricInfo.setSong_title(string);
                return;
            }
            if (line.startsWith("[ar:")) {
                // 作者
                String string = line.substring(4, index).trim();
                lyricInfo.setSong_artist(string);
                return;
            }
            if (line.startsWith("[al:")) {
                // 所属专辑
                String string = line.substring(4, index).trim();
                lyricInfo.setSong_album(string);
                return;
            }
            if (line.startsWith("[by:")) {
                return;
            }
            if (index == 9 && line.trim().length() > 10) {
                // 歌词内容
                LineInfo lineInfo = new LineInfo();
                lineInfo.setContent(line.substring(10, line.length()));
                lineInfo.setStart(measureStartTimeMillis(line.substring(0, 10)));
                lyricInfo.addSong_lines(lineInfo);
            }
        }
    }

    /**
     * 从字符串中获得时间值
     *
     * @param str 单行歌词开始部分时间字符串
     * @return 毫秒单位的时间值
     */
    private int measureStartTimeMillis(String str) {
        int minute = Integer.parseInt(str.substring(1, 3));
        int second = Integer.parseInt(str.substring(4, 6));
        int millisecond = Integer.parseInt(str.substring(7, 9));
        return millisecond + second * 1000 + minute * 60 * 1000;
    }


    /**
     * 判断当前时间所属行,使用二分查找减少计算时间
     */
    private void measureCurrentLine() {
        int low = 0, high = mLines.size() - 1, mid = 0;
        while (low <= high) {
            if (mCurrentPosition <= mLines.get(0).getStart()) {
                mNewLineIndex = 0;
                break;
            }
            if (mCurrentPosition >= mLines.get(mLines.size() - 1).getStart()) {
                mNewLineIndex = mLines.size() - 1;
                break;
            }
            mid = (low + high) / 2;
            if (mCurrentPosition >= mLines.get(mid).getStart()) {
                if (mCurrentPosition < mLines.get(mid + 1).getStart()) {
                    mNewLineIndex = mid;
                    break;
                } else {
                    low = mid + 1;
                }
            } else if (mCurrentPosition < mLines.get(mid).getStart()) {
                if (mCurrentPosition >= mLines.get(mid - 1).getStart()) {
                    mNewLineIndex = mid - 1;
                    break;
                } else {
                    high = mid - 1;
                }
            }
        }
    }

    /**
     * 平滑滚动
     */
    private void smoothScrollTo(final float start, float end) {
        ValueAnimator animator = ValueAnimator.ofFloat(start, end);
        animator.setDuration(mAnimatorDuration);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mOffset = (float) valueAnimator.getAnimatedValue() - start;
                reDraw();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentLineIndex = mNewLineIndex;
                mOffset = 0;
                mChanged = true;
                reDraw();
            }
        });
        animator.start();
    }

    /**
     * 处理拖动后中心和之间拖动后所属行中心的偏移量
     * <p>
     * 暂时取消此功能
     *
     * @param start
     * @param end
     */
    private void dealWithOffset(final float start, float end) {

        ValueAnimator animator = ValueAnimator.ofFloat(start, end);
        animator.setDuration(500);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float fraction = valueAnimator.getAnimatedFraction();
                mDragged += fraction;
                reDraw();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mOffset = 0;
                mChanged = true;
                reDraw();
            }
        });
        animator.start();
    }

    /**
     * 平滑滚动到播放位置
     *
     * @param start
     * @param end
     */
    private void backToPlayingLine(final float start, float end) {

        ValueAnimator animator = ValueAnimator.ofFloat(start, end);
        animator.setDuration(mBackTime);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mDragged = (float) valueAnimator.getAnimatedValue();
                reDraw();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mDragged = 0;
                reDraw();
            }
        });
        animator.start();
    }

    /**
     * 设置指示器按钮点击监听
     *
     * @param listener 指示器按钮点击监听
     */
    public void setOnIndicatorPlayListener(IndicatorListener listener) {
        this.mIndicatorListener = listener;
    }

    /**
     * 开始
     */
    public void start() {
        mHandler.sendEmptyMessage(REFRESH);
    }

    /**
     * 停止
     */
    public void stop() {
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //获取控件宽高
        mViewHeight = getMeasuredHeight();
        mViewWidth = getMeasuredWidth();
        //根据控件大小调整歌词参数
        mPlayingLyricSize = mViewWidth / 20;
        mUnPlayingLyricSize = mViewWidth / 20;
        mIndicatorRadius = mViewWidth / 40;
        mLineSpace = mViewWidth / 28;
    }

    /**
     * 测量行高
     *
     * @param content 行内容
     */
    private void measureLine(String content) {
        mStaticLayout = new StaticLayout(content
                , mTextPaint, (int) (mViewWidth - mBrokenLineRight * 3 - mIndicatorCenterX - mIndicatorRadius)
                , Layout.Alignment.ALIGN_CENTER
                , 1.0F
                , 0
                , true);
    }

    /**
     * 测量拖动后的位置属于哪一行
     */
    private void measureDraggedLine() {
        float center = mViewHeight / 2;
        if (mDragged < 0) {
            //向上检索
            for (int i = mCurrentLineIndex; i >= 0; i--) {
                LineInfo info = mLines.get(i);
                float range_top = info.getTop() - mLineSpace / 2 - mOffset - mDragged;
                float range_bottom = info.getBottom() + mLineSpace / 2 - mOffset - mDragged;
                if (range_top <= center && center <= range_bottom) {
                    mDraggedOffset = center - info.getMiddle() - mOffset - mDragged;
                    mDraggedLine = i;
                    break;
                }
            }
        } else {
            //向下检索
            int total = mLines.size() - 1;
            for (int i = mCurrentLineIndex; i <= total; i++) {
                LineInfo info = mLines.get(i);
                float range_top = info.getTop() - mLineSpace / 2 - mOffset - mDragged;
                float range_bottom = info.getBottom() + mLineSpace / 2 - mOffset - mDragged;
                if (range_top <= center && center <= range_bottom) {
                    mDraggedOffset = center - info.getMiddle() - mOffset - mDragged;
                    mDraggedLine = i;
                    break;
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mLines != null) {
            //设置指示器画笔
            setIndicatorPaint();
            //初始化指示器中心点坐标
            if (mIndicatorCenterX == 0 && mIndicatorCenterY == 0) {
                mIndicatorCenterX = mIndicatorLeft + mIndicatorRadius;
                mIndicatorCenterY = mViewHeight / 2;
            }

            if (mDragging || mDragged != 0) {
                //绘制圆
                mPaint.setStyle(Paint.Style.STROKE);
                canvas.drawCircle(mIndicatorCenterX,
                        mIndicatorCenterY,
                        mIndicatorRadius,
                        mPaint);

                //绘制虚线
                DashPathEffect pathEffect = new DashPathEffect(new float[]{mBrokenLineWidth, mBrokenLineSpace}, 0);
                mPaint.setPathEffect(pathEffect);
                Path path = new Path();
                path.moveTo(mIndicatorCenterX + mIndicatorRadius + mBrokenLineLeft, mIndicatorCenterY);
                path.lineTo(mViewWidth - mBrokenLineRight * 2, mIndicatorCenterY);
                canvas.drawPath(path, mPaint);
                mPaint.setPathEffect(null);

                //重置画笔
                mPaint.setStyle(Paint.Style.FILL);

                //绘制三角
                canvas.drawLine(mIndicatorCenterX - mIndicatorRadius / 2 + 2,
                        mIndicatorCenterY - mIndicatorRadius * 3 / 4 + 2 * ((float) Math.sqrt(3)),
                        mIndicatorCenterX - mIndicatorRadius / 2 + 2,
                        mIndicatorCenterY + mIndicatorRadius * 3 / 4 - 2 * ((float) Math.sqrt(3)),
                        mPaint);

                canvas.drawLine(mIndicatorCenterX - mIndicatorRadius / 2 + 2,
                        mIndicatorCenterY + mIndicatorRadius * 3 / 4 - 2 * ((float) Math.sqrt(3)),
                        mIndicatorCenterX + mIndicatorRadius - 2 - 4,
                        mIndicatorCenterY,
                        mPaint);

                canvas.drawLine(mIndicatorCenterX - mIndicatorRadius / 2 + 2,
                        mIndicatorCenterY - mIndicatorRadius * 3 / 4 + 2 * ((float) Math.sqrt(3)),
                        mIndicatorCenterX + mIndicatorRadius - 2 - 4,
                        mIndicatorCenterY,
                        mPaint);
            }

            //设置播放行画笔
            setPlayingTextPaint();
            //绘制播放行
            canvas.save();
            LineInfo currentLine = mLines.get(mCurrentLineIndex);
            String content = currentLine.getContent();
            measureLine(content);
            mCurrentLineTop = (mViewHeight - mStaticLayout.getHeight()) / 2;
            mCurrentLineBottom = (mViewHeight + mStaticLayout.getHeight()) / 2 + mLineSpace;
            currentLine.setTop(mCurrentLineTop);
            currentLine.setBottom(mCurrentLineBottom);
            currentLine.setMiddle(mViewHeight / 2);
            canvas.translate(mIndicatorCenterX + mIndicatorRadius + mBrokenLineLeft, mCurrentLineTop - mOffset - mDragged);
            mStaticLayout.draw(canvas);
            canvas.restore();

            //设置未播放行画笔
            setUnPlayingTextPaint();
            //绘制播放行前面歌词
            if (mCurrentLineIndex != 0)
                for (int i = mCurrentLineIndex - 1; i >= 0; i--) {
                    LineInfo line = mLines.get(i);
                    content = line.getContent();
                    canvas.save();
                    measureLine(content);
                    mCurrentLineTop -= mStaticLayout.getHeight() + mLineSpace;
                    line.setTop(mCurrentLineTop);
                    line.setBottom(mCurrentLineTop + mStaticLayout.getHeight());
                    line.setMiddle(mCurrentLineTop + mStaticLayout.getHeight() / 2);
                    canvas.translate(mIndicatorCenterX + mIndicatorRadius + mBrokenLineLeft, mCurrentLineTop - mOffset - mDragged);
                    mStaticLayout.draw(canvas);
                    canvas.restore();
                }
            //绘制播放行后面歌词
            if (mCurrentLineIndex != mLines.size())
                for (int i = mCurrentLineIndex + 1; i < mLines.size(); i++) {
                    LineInfo line = mLines.get(i);
                    content = line.getContent();
                    canvas.save();
                    measureLine(content);
                    canvas.translate(mIndicatorCenterX + mIndicatorRadius + mBrokenLineLeft, mCurrentLineBottom - mOffset - mDragged);
                    line.setTop(mCurrentLineBottom);
                    line.setBottom(mCurrentLineBottom + mStaticLayout.getHeight());
                    line.setMiddle(mCurrentLineBottom + mStaticLayout.getHeight() / 2);
                    mCurrentLineBottom += mStaticLayout.getHeight() + mLineSpace;
                    mStaticLayout.draw(canvas);
                    canvas.restore();
                }
        } else {
            mTextPaint.setColor(mPlayingLyricColor);
            mPaint.setTextSize(mPlayingLyricSize);
            canvas.save();
            measureLine(mDefaultText);
            mDrawingStartY = (mViewHeight - mStaticLayout.getHeight()) / 2;
            canvas.translate(mIndicatorCenterX + mIndicatorRadius + mBrokenLineLeft, mDrawingStartY);
            mStaticLayout.draw(canvas);
            canvas.restore();
            reDraw();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN://手指按下，显示指示器
                mDragging = true;
                if (mFlingAnimator != null)
                    mFlingAnimator.cancel();
                //手指触摸屏幕，取消上一次的返回播放位置延迟任务
                mHasMessage = mHandler.hasMessages(CLEAR_DRAGGED);
                if (mHasMessage)
                    mHandler.removeMessages(CLEAR_DRAGGED);
                reDraw();
                break;
            case MotionEvent.ACTION_MOVE://手指移动，拖动歌词

                break;
            case MotionEvent.ACTION_UP://手指抬起，隐藏指示器
                if (mHasMessage)
                    mHandler.sendEmptyMessageDelayed(CLEAR_DRAGGED, mClearTime);
                mDragging = false;
//                mDragged = 0;
                reDraw();
                break;
        }
        //让GestureDetector接管onTouchEvent事件
        mGestureDetector.onTouchEvent(event);
        return true;
    }


    //----------------------OnGestureListener方法----------------------
    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        float r_judge = (x - mIndicatorCenterX) * (x - mIndicatorCenterX) + (y - mIndicatorCenterY) * (y - mIndicatorCenterY);
        float r = mIndicatorRadius * mIndicatorRadius;
        //如果点击位置处于指示器播放按钮范围内
        if (r_judge <= r && mDragged != 0) {
            //取消上一次返回播放位置的延迟任务
            mHandler.removeMessages(CLEAR_DRAGGED);
            if (mIndicatorListener != null) {
                mCurrentLineIndex = mDraggedLine;
                mCurrentPosition = mLines.get(mCurrentLineIndex).getStart();
                mIndicatorListener.onPlayClick(mCurrentPosition);
                reDraw();
            }
            mDragged = 0;
        }
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        mDragged += distanceY;
        reDraw();
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(final MotionEvent e1, MotionEvent e2, float velocityX, final float velocityY) {
        mPartOfFling = velocityY / mSpeed;
        mFlingAnimator = ValueAnimator.ofFloat(velocityY, 0);
        mFlingAnimator.setDuration(1000);
        mFlingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                //计算当前滚动值所占比例并动态改变滑动速度
                if (velocityY > 0) {
                    if (value <= mPartOfFling) {
                        mSpeed = 1;
                    } else if (mPartOfFling < value && value <= 2 * mPartOfFling) {
                        mSpeed = 2;
                    } else if (2 * mPartOfFling < value && value <= 3 * mPartOfFling) {
                        mSpeed = 3;
                    } else if (value > 3 * mPartOfFling) {
                        mSpeed = 4;
                    }

                    mDragged -= mSpeed;
                } else {
                    if (value >= mPartOfFling) {
                        mSpeed = 1;
                    } else if (mPartOfFling > value && value >= 2 * mPartOfFling) {
                        mSpeed = 2;
                    } else if (2 * mPartOfFling > value && value >= 3 * mPartOfFling) {
                        mSpeed = 3;
                    } else if (value < 3 * mPartOfFling) {
                        mSpeed = 4;
                    }

                    mDragged += mSpeed;
                }
                reDraw();
            }
        });
        mFlingAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                //滑动结束后恢复默认速度
                mSpeed = 4;
//                reDraw();

                //计算滑动后的位置属于第几行
                measureDraggedLine();
                //调整差值，使中心滑动到最近的一行的中心(此功能暂时取消)
//                dealWithOffset(mDraggedOffset, 0);

                //拖动操作完成2秒后自动返回播放位置
                mHandler.sendEmptyMessageDelayed(CLEAR_DRAGGED, mClearTime);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                //滑动取消后恢复默认速度
                mSpeed = 4;
            }
        });
        mFlingAnimator.start();
        return false;
    }
    //----------------------------------------------------------------

    //指示器开始按钮点击监听接口
    public interface IndicatorListener {
        void onPlayClick(int position);
    }

    /**
     * 歌词行信息实体类
     */
    class LineInfo {
        private String content;  // 歌词内容
        private int start;  // 开始时间
        private float middle, top, bottom;

        public float getMiddle() {
            return middle;
        }

        public void setMiddle(float middle) {
            this.middle = middle;
        }

        public float getTop() {
            return top;
        }

        public void setTop(float top) {
            this.top = top;
        }

        public float getBottom() {
            return bottom;
        }

        public void setBottom(float bottom) {
            this.bottom = bottom;
        }

        public int getStart() {
            return start;
        }

        public void setStart(int start) {
            this.start = start;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    /**
     * 歌词信息实体类
     */
    class LyricInfo {
        private List<LineInfo> song_lines;

        private String song_artist;  // 歌手
        private String song_title;  // 标题
        private String song_album;  // 专辑

        private long song_offset;  // 偏移量

        public LyricInfo() {
            song_lines = new ArrayList<>();
        }

        public void addSong_lines(LineInfo lineInfo) {
            if (lineInfo != null)
                song_lines.add(lineInfo);
        }

        public List<LineInfo> getSong_lines() {
            return song_lines;
        }

        public void setSong_lines(List<LineInfo> song_lines) {
            this.song_lines = song_lines;
        }

        public String getSong_artist() {
            return song_artist;
        }

        public void setSong_artist(String song_artist) {
            this.song_artist = song_artist;
        }

        public String getSong_title() {
            return song_title;
        }

        public void setSong_title(String song_title) {
            this.song_title = song_title;
        }

        public String getSong_album() {
            return song_album;
        }

        public void setSong_album(String song_album) {
            this.song_album = song_album;
        }

        public long getSong_offset() {
            return song_offset;
        }

        public void setSong_offset(long song_offset) {
            this.song_offset = song_offset;
        }
    }
}
