package com.megacast.castsdk.providers.managers.subtitle;

import com.megacast.castsdk.model.SubtitleDescription;

import rx.Observable;

/**
 * Created by Dmitry on 12.01.17.
 */

public interface SubtitleConversionManager {

    Observable<SubtitleDescription> convert(SubtitleDescription subtitleDescription, Integer type);

    void setStorageDir(String tempStorage);
}
