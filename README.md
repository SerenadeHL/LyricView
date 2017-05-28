# LyricView 1.0

## API

- 设置歌词
  - ``setLyric(String lyric)``
  - ``setLyric(File file)``
- 设置无歌词时显示的内容
  - ``setNoLyricText(String text)``
- 设置当前播放时间
  - ``setCurrentPosition(int current)``
- 设置总时长
  - ``setDuration(int duration)``
- 设置歌词行间距
  - ``setLineSpace(int space)``
- 设置播放行字体大小
  - ``setPlayingLyricSize(int size)``
- 设置未播放行字体大小
  - ``setUnPlayingLyricSize(int size)``
- 设置播放行字体颜色
  - ``setPlayingLyricColor(int color)``
- 设置未播放行字体颜色
  - ``setUnPlayingLyricColor(int color)``
- 设置指示器颜色
  - ``setIndicatorColor(int color)``
- 设置指示器开始按钮点击事件监听器
  - ``setOnIndicatorPlayListener(IndicatorListener listener)``
- 开始
  - ``start()``
- 停止
  - ``stop()``