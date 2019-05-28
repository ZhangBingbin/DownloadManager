package com.xunlei.downloadlib.listener;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * ================================================
 * 框架要求框架中的每个 {@link } 都需要实现此类,以满足规范
 *
 * @function 作用
 * Created by Joe_ZBB on 2019/3/29 3:27 PM
 * ================================================
 */
public class BaseObserveListener<T> implements Observer<T> {
    @Override
    public void onSubscribe(Disposable d) {

    }

    @Override
    public void onNext(T t) {

    }

    @Override
    public void onError(Throwable e) {

    }

    @Override
    public void onComplete() {

    }
}
