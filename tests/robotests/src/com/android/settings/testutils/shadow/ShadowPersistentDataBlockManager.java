/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.testutils.shadow;

import android.service.persistentdata.PersistentDataBlockManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

@Implements(PersistentDataBlockManager.class)
public class ShadowPersistentDataBlockManager {
    private static int sDataBlockSize = 0;

    @Resetter
    public static void reset() {
        sDataBlockSize = 0;
    }

    @Implementation
    protected int getDataBlockSize() {
        return sDataBlockSize;
    }

    public static void setDataBlockSize(int dataBlockSize) {
        sDataBlockSize = dataBlockSize;
    }
}
