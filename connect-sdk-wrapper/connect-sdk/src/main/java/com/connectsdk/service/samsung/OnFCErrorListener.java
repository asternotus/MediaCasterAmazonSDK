/*
 * Copyright (c) 2015 Samsung Electronics, Inc.
 * All rights reserved.
 */

package com.connectsdk.service.samsung;

import com.connectsdk.service.samsung.exceptions.FCError;

/**
 * Listener interface used to inform about errors during Casting. By this
 * listener all errors are communicated
 * 
 * @author m.gajewski
 *
 */
public interface OnFCErrorListener {
	/**
	 * Called when some error occure
	 * 
	 * @param error
	 *            {@link FCError} error object.
	 */
	void OnError(FCError error);
}
