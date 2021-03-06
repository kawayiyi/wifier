package com.traffic.wifiapp.rxjava;

import android.content.Context;

import com.traffic.wifiapp.retrofit.CacheManager;

import java.io.Serializable;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by Jam on 16-7-6
 * Description:
 * RxJava + Retrofit 的缓存机制
 */
public class RxRetrofitCache {
    private static final String TAG = "RxRetrofitCache";
    /**
     *
     * @param context
     * @param cacheKey 缓存key
     * @param expireTime 过期时间 0 表示有缓存就读，没有就从网络获取
     * @param fromNetwork 从网络获取的Observable
     * @param forceRefresh 是否强制刷新
     * @param <T>
     * @return
     */
    public static <T> Observable<T> load(final Context context, final String cacheKey, final long expireTime, Observable<T> fromNetwork, boolean forceRefresh) {
        Observable<T> fromCache = Observable.create((Observable.OnSubscribe<T>) subscriber -> {
            T cache = (T) CacheManager.readObject(context, cacheKey,expireTime);
            if (cache != null) {
                subscriber.onNext(cache);
            } else {
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());


        /**
         * 这里的fromNetwork 不需要指定Schedule,在handleRequest中已经变换了
         */
        fromNetwork = fromNetwork.map(new Func1<T, T>() {
            @Override
            public T call(T result) {
                CacheManager.saveObject(context, (Serializable) result, cacheKey);
                return result;
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        if (forceRefresh) {
            return fromNetwork;
        } else {
            return Observable.concat(fromCache, fromNetwork).first();
        }

    }


}
